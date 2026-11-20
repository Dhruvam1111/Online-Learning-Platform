package com.olp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Response DTO for the admin platform statistics dashboard (FR-A20, ADM-026).
 */
public class AdminDashboardStatsResponse {

    private long totalUsers;
    private Map<String, Long> usersByRole;
    private long totalCourses;
    private long activeCourses;
    private Map<String, Long> coursesByStatus;
    private long totalEnrollments;
    private boolean revenueAvailable;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal totalRevenue;

    public AdminDashboardStatsResponse() {
    }

    public AdminDashboardStatsResponse(
            long totalUsers,
            Map<String, Long> usersByRole,
            long totalCourses,
            long activeCourses,
            Map<String, Long> coursesByStatus,
            long totalEnrollments,
            boolean revenueAvailable,
            BigDecimal totalRevenue) {
        this.totalUsers = totalUsers;
        this.usersByRole = usersByRole;
        this.totalCourses = totalCourses;
        this.activeCourses = activeCourses;
        this.coursesByStatus = coursesByStatus;
        this.totalEnrollments = totalEnrollments;
        this.revenueAvailable = revenueAvailable;
        this.totalRevenue = totalRevenue;
    }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
    public Map<String, Long> getUsersByRole() { return usersByRole; }
    public void setUsersByRole(Map<String, Long> usersByRole) { this.usersByRole = usersByRole; }
    public long getTotalCourses() { return totalCourses; }
    public void setTotalCourses(long totalCourses) { this.totalCourses = totalCourses; }
    public long getActiveCourses() { return activeCourses; }
    public void setActiveCourses(long activeCourses) { this.activeCourses = activeCourses; }
    public Map<String, Long> getCoursesByStatus() { return coursesByStatus; }
    public void setCoursesByStatus(Map<String, Long> coursesByStatus) { this.coursesByStatus = coursesByStatus; }
    public long getTotalEnrollments() { return totalEnrollments; }
    public void setTotalEnrollments(long totalEnrollments) { this.totalEnrollments = totalEnrollments; }
    public boolean isRevenueAvailable() { return revenueAvailable; }
    public void setRevenueAvailable(boolean revenueAvailable) { this.revenueAvailable = revenueAvailable; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
}