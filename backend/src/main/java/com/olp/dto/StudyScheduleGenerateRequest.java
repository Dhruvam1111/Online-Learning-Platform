package com.olp.dto;

public class StudyScheduleGenerateRequest {

    private Integer daysAhead = 14;

    private Double preferredDailyHours = 2.0;

    private String timeZone = "Asia/Kolkata";

    public StudyScheduleGenerateRequest() {}

    public Integer getDaysAhead() {
        return daysAhead;
    }

    public void setDaysAhead(Integer daysAhead) {
        this.daysAhead = daysAhead != null ? daysAhead : 14;
    }

    public Double getPreferredDailyHours() {
        return preferredDailyHours;
    }

    public void setPreferredDailyHours(Double preferredDailyHours) {
        this.preferredDailyHours = preferredDailyHours != null ? preferredDailyHours : 2.0;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone != null ? timeZone : "Asia/Kolkata";
    }
}
