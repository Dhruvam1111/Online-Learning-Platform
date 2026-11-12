package com.olp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PlaybackPositionRequest {
    @NotNull(message = "Playback position is required")
    @Min(value = 0, message = "Playback position must be non-negative")
    private Integer playbackPositionSeconds;

    public PlaybackPositionRequest() {}
    public PlaybackPositionRequest(Integer playbackPositionSeconds) {
        this.playbackPositionSeconds = playbackPositionSeconds;
    }

    public Integer getPlaybackPositionSeconds() { return playbackPositionSeconds; }
    public void setPlaybackPositionSeconds(Integer playbackPositionSeconds) { 
        this.playbackPositionSeconds = playbackPositionSeconds; 
    }
}