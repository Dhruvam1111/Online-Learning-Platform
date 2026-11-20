package com.olp.dto;

import com.olp.entity.Badge;
import java.time.LocalDateTime;

public class BadgeResponse {

    private Integer id;
    private String code;
    private String name;
    private String description;
    private String iconUrl;
    private Integer pointsRequired;
    private String category;
    private Boolean isEarned;
    private LocalDateTime awardedAt;

    public BadgeResponse() {}

    public static BadgeResponse fromEntity(Badge badge, boolean isEarned, LocalDateTime awardedAt) {
        if (badge == null) return null;
        BadgeResponse dto = new BadgeResponse();
        dto.setId(badge.getId());
        dto.setCode(badge.getCode());
        dto.setName(badge.getName());
        dto.setDescription(badge.getDescription());
        dto.setIconUrl(badge.getIconUrl());
        dto.setPointsRequired(badge.getPointsRequired());
        dto.setCategory(badge.getCategory());
        dto.setIsEarned(isEarned);
        dto.setAwardedAt(awardedAt);
        return dto;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public Integer getPointsRequired() {
        return pointsRequired;
    }

    public void setPointsRequired(Integer pointsRequired) {
        this.pointsRequired = pointsRequired;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getIsEarned() {
        return isEarned;
    }

    public void setIsEarned(Boolean isEarned) {
        this.isEarned = isEarned;
    }

    public LocalDateTime getAwardedAt() {
        return awardedAt;
    }

    public void setAwardedAt(LocalDateTime awardedAt) {
        this.awardedAt = awardedAt;
    }
}
