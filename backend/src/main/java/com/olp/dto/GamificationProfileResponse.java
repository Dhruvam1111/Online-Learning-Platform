package com.olp.dto;

import java.time.LocalDate;
import java.util.List;

public class GamificationProfileResponse {

    private Integer studentId;
    private String studentName;
    private Integer totalPoints;
    private Integer currentStreakDays;
    private Integer longestStreakDays;
    private LocalDate lastActivityDate;
    private Integer level;
    private Integer rank;
    private Integer totalEarnedBadges;
    private List<BadgeResponse> badges;

    public GamificationProfileResponse() {}

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    public Integer getCurrentStreakDays() {
        return currentStreakDays;
    }

    public void setCurrentStreakDays(Integer currentStreakDays) {
        this.currentStreakDays = currentStreakDays;
    }

    public Integer getLongestStreakDays() {
        return longestStreakDays;
    }

    public void setLongestStreakDays(Integer longestStreakDays) {
        this.longestStreakDays = longestStreakDays;
    }

    public LocalDate getLastActivityDate() {
        return lastActivityDate;
    }

    public void setLastActivityDate(LocalDate lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Integer getTotalEarnedBadges() {
        return totalEarnedBadges;
    }

    public void setTotalEarnedBadges(Integer totalEarnedBadges) {
        this.totalEarnedBadges = totalEarnedBadges;
    }

    public List<BadgeResponse> getBadges() {
        return badges;
    }

    public void setBadges(List<BadgeResponse> badges) {
        this.badges = badges;
    }
}
