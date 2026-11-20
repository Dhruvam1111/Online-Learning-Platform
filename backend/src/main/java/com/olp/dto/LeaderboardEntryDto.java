package com.olp.dto;

public class LeaderboardEntryDto {

    private Integer rank;
    private Integer studentId;
    private String studentName;
    private Integer totalPoints;
    private Integer level;
    private Integer streakDays;
    private Integer badgeCount;

    public LeaderboardEntryDto() {}

    public LeaderboardEntryDto(Integer rank, Integer studentId, String studentName,
                               Integer totalPoints, Integer level, Integer streakDays, Integer badgeCount) {
        this.rank = rank;
        this.studentId = studentId;
        this.studentName = studentName;
        this.totalPoints = totalPoints;
        this.level = level;
        this.streakDays = streakDays;
        this.badgeCount = badgeCount;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

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

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getStreakDays() {
        return streakDays;
    }

    public void setStreakDays(Integer streakDays) {
        this.streakDays = streakDays;
    }

    public Integer getBadgeCount() {
        return badgeCount;
    }

    public void setBadgeCount(Integer badgeCount) {
        this.badgeCount = badgeCount;
    }
}
