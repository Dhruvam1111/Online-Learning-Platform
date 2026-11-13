package com.olp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request payload for updating the downloadable vs view-only status of a material (FR-I17).
 */
public class UpdateDownloadableRequest {

    @JsonProperty("isDownloadable")
    private Boolean isDownloadable;

    private Boolean downloadable;

    public UpdateDownloadableRequest() {}

    public UpdateDownloadableRequest(Boolean isDownloadable) {
        this.isDownloadable = isDownloadable;
        this.downloadable = isDownloadable;
    }

    public Boolean getIsDownloadable() {
        return isDownloadable != null ? isDownloadable : downloadable;
    }

    public void setIsDownloadable(Boolean isDownloadable) {
        this.isDownloadable = isDownloadable;
    }

    public Boolean getDownloadable() {
        return downloadable != null ? downloadable : isDownloadable;
    }

    public void setDownloadable(Boolean downloadable) {
        this.downloadable = downloadable;
    }

    public Boolean resolveDownloadable() {
        if (isDownloadable != null) {
            return isDownloadable;
        }
        return downloadable;
    }
}
