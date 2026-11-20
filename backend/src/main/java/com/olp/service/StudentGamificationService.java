package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.exception.InvalidCredentialsException;
import com.olp.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing student gamification, points awarding, learning streak tracking,
 * achievement badges, leaderboards, and learning challenges (FR-S66, FR-S67, FR-S68, FR-S69, FR-S70).
 */
@Service
public class StudentGamificationService {

    private final StudentGamificationRepository gamificationRepository;
    private final GamificationPointLogRepository pointLogRepository;
    private final BadgeRepository badgeRepository;
    private final StudentBadgeRepository studentBadgeRepository;
    private final LearningChallengeRepository challengeRepository;
    private final StudentChallengeProgressRepository challengeProgressRepository;

    public StudentGamificationService(StudentGamificationRepository gamificationRepository,
                                      GamificationPointLogRepository pointLogRepository,
                                      BadgeRepository badgeRepository,
                                      StudentBadgeRepository studentBadgeRepository,
                                      LearningChallengeRepository challengeRepository,
                                      StudentChallengeProgressRepository challengeProgressRepository) {
        this.gamificationRepository = gamificationRepository;
        this.pointLogRepository = pointLogRepository;
        this.badgeRepository = badgeRepository;
        this.studentBadgeRepository = studentBadgeRepository;
        this.challengeRepository = challengeRepository;
        this.challengeProgressRepository = challengeProgressRepository;
    }

    @Transactional
    public StudentGamification getOrCreateGamification(User student) {
        return gamificationRepository.findByStudentId(student.getId())
                .orElseGet(() -> gamificationRepository.save(new StudentGamification(student)));
    }

    @Transactional(readOnly = true)
    public GamificationProfileResponse getProfile(User student) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        StudentGamification gamification = gamificationRepository.findByStudentId(student.getId())
                .orElseGet(() -> new StudentGamification(student));

        List<StudentBadge> studentBadges = studentBadgeRepository.findByStudentId(student.getId());
        List<BadgeResponse> badgeDtos = studentBadges.stream()
                .map(sb -> BadgeResponse.fromEntity(sb.getBadge(), true, sb.getAwardedAt()))
                .collect(Collectors.toList());

        int rank = gamificationRepository.computeRankByPoints(gamification.getTotalPoints());

        GamificationProfileResponse response = new GamificationProfileResponse();
        response.setStudentId(student.getId());
        response.setStudentName(student.getName());
        response.setTotalPoints(gamification.getTotalPoints());
        response.setCurrentStreakDays(gamification.getCurrentStreakDays());
        response.setLongestStreakDays(gamification.getLongestStreakDays());
        response.setLastActivityDate(gamification.getLastActivityDate());
        response.setLevel(gamification.getLevel());
        response.setRank(rank);
        response.setTotalEarnedBadges(badgeDtos.size());
        response.setBadges(badgeDtos);

        return response;
    }

    @Transactional
    public GamificationProfileResponse recordActivity(User student, String activityType, Integer refId, String description) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        StudentGamification gamification = getOrCreateGamification(student);

        int pointsToAdd = computePoints(activityType);
        gamification.setTotalPoints(gamification.getTotalPoints() + pointsToAdd);

        // FR-S67: Track student learning streaks (consecutive calendar days)
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate lastDate = gamification.getLastActivityDate();

        if (lastDate == null) {
            gamification.setCurrentStreakDays(1);
        } else if (lastDate.plusDays(1).isEqual(today)) {
            gamification.setCurrentStreakDays(gamification.getCurrentStreakDays() + 1);
        } else if (lastDate.isBefore(today.minusDays(1))) {
            gamification.setCurrentStreakDays(1);
        } // if lastDate == today, streak days remain unchanged

        if (gamification.getCurrentStreakDays() > gamification.getLongestStreakDays()) {
            gamification.setLongestStreakDays(gamification.getCurrentStreakDays());
        }
        gamification.setLastActivityDate(today);

        // Compute level (100 points per level)
        int newLevel = (gamification.getTotalPoints() / 100) + 1;
        gamification.setLevel(newLevel);

        gamificationRepository.save(gamification);

        // FR-S66: Award points and log entry
        GamificationPointLog log = new GamificationPointLog(
                student, activityType, pointsToAdd, refId, description
        );
        pointLogRepository.save(log);

        // FR-S68: Award badges for predefined achievements
        checkAndAwardBadges(student, gamification, activityType);

        // FR-S70: Update active learning challenges
        updateChallengeProgress(student, activityType);

        return getProfile(student);
    }

    @Transactional(readOnly = true)
    public List<BadgeResponse> getAllBadgesForStudent(User student) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        List<Badge> allBadges = badgeRepository.findAll();
        List<StudentBadge> earned = studentBadgeRepository.findByStudentId(student.getId());
        Map<Integer, LocalDateTime> earnedMap = earned.stream()
                .collect(Collectors.toMap(sb -> sb.getBadge().getId(), StudentBadge::getAwardedAt, (a, b) -> a));

        return allBadges.stream().map(b -> {
            boolean isEarned = earnedMap.containsKey(b.getId());
            LocalDateTime awardedAt = earnedMap.get(b.getId());
            return BadgeResponse.fromEntity(b, isEarned, awardedAt);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> getLeaderboard(User student, Integer limit) {
        int max = (limit != null && limit > 0 && limit <= 100) ? limit : 20;
        List<StudentGamification> list = gamificationRepository.findLeaderboard(PageRequest.of(0, max));

        List<LeaderboardEntryDto> result = new ArrayList<>();
        int rank = 1;
        for (StudentGamification g : list) {
            int badgesCount = (int) studentBadgeRepository.findByStudentId(g.getStudent().getId()).size();
            result.add(new LeaderboardEntryDto(
                    rank++,
                    g.getStudent().getId(),
                    g.getStudent().getName(),
                    g.getTotalPoints(),
                    g.getLevel(),
                    g.getCurrentStreakDays(),
                    badgesCount
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<LearningChallengeResponse> getActiveChallenges(User student) {
        if (student == null) {
            throw new InvalidCredentialsException("User is not authenticated");
        }
        LocalDateTime now = LocalDateTime.now();
        List<LearningChallenge> activeChallenges = challengeRepository.findActiveChallenges(now);
        List<StudentChallengeProgress> progressList = challengeProgressRepository.findByStudentId(student.getId());
        Map<Integer, StudentChallengeProgress> progressMap = progressList.stream()
                .collect(Collectors.toMap(p -> p.getChallenge().getId(), p -> p, (a, b) -> a));

        return activeChallenges.stream().map(c -> {
            StudentChallengeProgress p = progressMap.get(c.getId());
            return LearningChallengeResponse.fromEntity(c, p);
        }).collect(Collectors.toList());
    }

    private int computePoints(String activityType) {
        if (activityType == null) return 5;
        return switch (activityType.toUpperCase().trim()) {
            case "LECTURE_COMPLETED" -> 10;
            case "QUIZ_PASSED" -> 25;
            case "EXAM_PASSED" -> 50;
            case "COURSE_COMPLETED" -> 150;
            case "DISCUSSION_POST" -> 15;
            case "ASSIGNMENT_SUBMITTED" -> 20;
            case "DAILY_LOGIN" -> 5;
            default -> 5;
        };
    }

    private void checkAndAwardBadges(User student, StudentGamification gamification, String activityType) {
        if (gamification.getCurrentStreakDays() >= 3) {
            awardBadgeIfNotPresent(student, "STREAK_3_DAYS");
        }
        if (gamification.getCurrentStreakDays() >= 7) {
            awardBadgeIfNotPresent(student, "STREAK_7_DAYS");
        }
        if ("QUIZ_PASSED".equalsIgnoreCase(activityType)) {
            awardBadgeIfNotPresent(student, "QUIZ_MASTER");
        }
        if ("EXAM_PASSED".equalsIgnoreCase(activityType)) {
            awardBadgeIfNotPresent(student, "PERFECT_EXAM");
        }
        if ("COURSE_COMPLETED".equalsIgnoreCase(activityType)) {
            awardBadgeIfNotPresent(student, "COURSE_COMPLETED");
        }
        if ("DISCUSSION_POST".equalsIgnoreCase(activityType)) {
            awardBadgeIfNotPresent(student, "ACTIVE_COMMUNICATOR");
        }
        if ("FIRST_ENROLLMENT".equalsIgnoreCase(activityType)) {
            awardBadgeIfNotPresent(student, "FIRST_ENROLLMENT");
        }
    }

    private void awardBadgeIfNotPresent(User student, String badgeCode) {
        if (!studentBadgeRepository.existsByStudentIdAndBadgeCode(student.getId(), badgeCode)) {
            badgeRepository.findByCode(badgeCode).ifPresent(badge -> {
                studentBadgeRepository.save(new StudentBadge(student, badge));
            });
        }
    }

    private void updateChallengeProgress(User student, String activityType) {
        LocalDateTime now = LocalDateTime.now();
        List<LearningChallenge> activeChallenges = challengeRepository.findActiveChallenges(now);

        for (LearningChallenge c : activeChallenges) {
            boolean matches = false;
            if ("COMPLETE_LESSONS".equalsIgnoreCase(c.getTargetType()) && "LECTURE_COMPLETED".equalsIgnoreCase(activityType)) {
                matches = true;
            } else if ("PASS_QUIZZES".equalsIgnoreCase(c.getTargetType()) && "QUIZ_PASSED".equalsIgnoreCase(activityType)) {
                matches = true;
            }

            if (matches) {
                StudentChallengeProgress progress = challengeProgressRepository
                        .findByStudentIdAndChallengeId(student.getId(), c.getId())
                        .orElseGet(() -> new StudentChallengeProgress(student, c));

                if (!Boolean.TRUE.equals(progress.getIsCompleted())) {
                    progress.setCurrentProgress(progress.getCurrentProgress() + 1);
                    if (progress.getCurrentProgress() >= c.getTargetCount()) {
                        progress.setIsCompleted(true);
                        progress.setCompletedAt(LocalDateTime.now());
                        // Award challenge reward points
                        StudentGamification g = getOrCreateGamification(student);
                        g.setTotalPoints(g.getTotalPoints() + c.getPointsReward());
                        gamificationRepository.save(g);
                    }
                    challengeProgressRepository.save(progress);
                }
            }
        }
    }
}
