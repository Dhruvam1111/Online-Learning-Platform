package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.*;
import com.olp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentGamificationServiceTest {

    @Mock
    private StudentGamificationRepository gamificationRepository;

    @Mock
    private GamificationPointLogRepository pointLogRepository;

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private StudentBadgeRepository studentBadgeRepository;

    @Mock
    private LearningChallengeRepository challengeRepository;

    @Mock
    private StudentChallengeProgressRepository challengeProgressRepository;

    @InjectMocks
    private StudentGamificationService gamificationService;

    private User student;
    private StudentGamification profile;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(5);
        student.setName("Charlie");

        profile = new StudentGamification(student);
        profile.setId(1);
        profile.setTotalPoints(120);
        profile.setCurrentStreakDays(3);
        profile.setLongestStreakDays(5);
        profile.setLastActivityDate(LocalDate.now());
        profile.setLevel(2);
    }

    @Test
    void testGetGamificationProfile_Success() {
        when(gamificationRepository.findByStudentId(5)).thenReturn(Optional.of(profile));
        when(studentBadgeRepository.findByStudentId(5)).thenReturn(List.of());
        when(gamificationRepository.computeRankByPoints(120)).thenReturn(3);

        GamificationProfileResponse res = gamificationService.getProfile(student);

        assertNotNull(res);
        assertEquals(120, res.getTotalPoints());
        assertEquals(3, res.getCurrentStreakDays());
        assertEquals(2, res.getLevel());
        assertEquals(3, res.getRank());
    }

    @Test
    void testRecordActivity_AwardsPointsAndUpdatesProfile() {
        when(gamificationRepository.findByStudentId(5)).thenReturn(Optional.of(profile));
        when(gamificationRepository.save(any(StudentGamification.class))).thenAnswer(i -> i.getArgument(0));
        when(challengeRepository.findActiveChallenges(any(LocalDateTime.class))).thenReturn(List.of());

        GamificationProfileResponse res = gamificationService.recordActivity(student, "LECTURE_COMPLETED", 101, "Finished video lecture");

        assertNotNull(res);
        assertTrue(res.getTotalPoints() > 120);
        verify(pointLogRepository).save(any(GamificationPointLog.class));
        verify(gamificationRepository).save(profile);
    }

    @Test
    void testGetLeaderboard_Success() {
        when(gamificationRepository.findLeaderboard(any(Pageable.class))).thenReturn(List.of(profile));
        when(studentBadgeRepository.findByStudentId(5)).thenReturn(List.of());

        List<LeaderboardEntryDto> leaderboard = gamificationService.getLeaderboard(student, 10);

        assertNotNull(leaderboard);
        assertEquals(1, leaderboard.size());
        assertEquals("Charlie", leaderboard.get(0).getStudentName());
        assertEquals(1, leaderboard.get(0).getRank());
    }

    @Test
    void testGetChallenges_Success() {
        LearningChallenge challenge = new LearningChallenge();
        challenge.setId(10);
        challenge.setTitle("Quiz Sprinter");
        challenge.setDescription("Pass 3 quizzes");
        challenge.setPointsReward(50);
        challenge.setTargetType("QUIZ_PASS");
        challenge.setTargetCount(3);
        challenge.setStartDate(LocalDateTime.now().minusDays(1));
        challenge.setEndDate(LocalDateTime.now().plusDays(5));
        challenge.setIsActive(true);

        when(challengeRepository.findActiveChallenges(any(LocalDateTime.class))).thenReturn(List.of(challenge));
        when(challengeProgressRepository.findByStudentId(5)).thenReturn(List.of());

        List<LearningChallengeResponse> list = gamificationService.getActiveChallenges(student);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Quiz Sprinter", list.get(0).getTitle());
        assertEquals(0, list.get(0).getCurrentProgress());
        assertFalse(list.get(0).getIsCompleted());
    }
}
