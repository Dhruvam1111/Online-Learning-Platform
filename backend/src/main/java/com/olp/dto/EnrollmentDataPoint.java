package com.olp.dto;

public class EnrollmentDataPoint {

    private String period;
    private String label;
    private long count;
    private long cumulativeCount;

    public EnrollmentDataPoint() {
    }

    public EnrollmentDataPoint(String period, String label, long count, long cumulativeCount) {
        this.period = period;
        this.label = label;
        this.count = count;
        this.cumulativeCount = cumulativeCount;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getCumulativeCount() {
        return cumulativeCount;
    }

    public void setCumulativeCount(long cumulativeCount) {
        this.cumulativeCount = cumulativeCount;
    }
}
