package com.olp.dto;

public class ActiveVsDroppedStats {

    private long totalStudents;
    private long activeCount;
    private long inProgressCount;
    private long completedCount;
    private long droppedCount;
    private long notStartedCount;
    private double activeRate;
    private double dropRate;
    private double completionRate;
    private int inactivityThresholdDays;

    public ActiveVsDroppedStats() {
    }

    public ActiveVsDroppedStats(
            long totalStudents,
            long activeCount,
            long inProgressCount,
            long completedCount,
            long droppedCount,
            long notStartedCount,
            double activeRate,
            double dropRate,
            double completionRate,
            int inactivityThresholdDays) {
        this.totalStudents = totalStudents;
        this.activeCount = activeCount;
        this.inProgressCount = inProgressCount;
        this.completedCount = completedCount;
        this.droppedCount = droppedCount;
        this.notStartedCount = notStartedCount;
        this.activeRate = activeRate;
        this.dropRate = dropRate;
        this.completionRate = completionRate;
        this.inactivityThresholdDays = inactivityThresholdDays;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(long activeCount) {
        this.activeCount = activeCount;
    }

    public long getInProgressCount() {
        return inProgressCount;
    }

    public void setInProgressCount(long inProgressCount) {
        this.inProgressCount = inProgressCount;
    }

    public long getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(long completedCount) {
        this.completedCount = completedCount;
    }

    public long getDroppedCount() {
        return droppedCount;
    }

    public void setDroppedCount(long droppedCount) {
        this.droppedCount = droppedCount;
    }

    public long getNotStartedCount() {
        return notStartedCount;
    }

    public void setNotStartedCount(long notStartedCount) {
        this.notStartedCount = notStartedCount;
    }

    public double getActiveRate() {
        return activeRate;
    }

    public void setActiveRate(double activeRate) {
        this.activeRate = activeRate;
    }

    public double getDropRate() {
        return dropRate;
    }

    public void setDropRate(double dropRate) {
        this.dropRate = dropRate;
    }

    public double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(double completionRate) {
        this.completionRate = completionRate;
    }

    public int getInactivityThresholdDays() {
        return inactivityThresholdDays;
    }

    public void setInactivityThresholdDays(int inactivityThresholdDays) {
        this.inactivityThresholdDays = inactivityThresholdDays;
    }
}
