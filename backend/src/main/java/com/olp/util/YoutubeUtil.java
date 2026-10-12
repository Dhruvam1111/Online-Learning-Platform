package com.olp.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeUtil {

    // Matches various YouTube URL formats:
    // https://www.youtube.com/watch?v=dQw4w9WgXcQ
    // https://youtube.com/watch?v=dQw4w9WgXcQ
    // https://youtu.be/dQw4w9WgXcQ
    // https://www.youtube.com/embed/dQw4w9WgXcQ
    // https://www.youtube.com/watch?feature=shared&v=dQw4w9WgXcQ
    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/|v/)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
    );

    private static final Pattern DIRECT_VIDEO_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{11}$");

    /**
     * Extract the 11-character video ID from a YouTube URL.
     * Returns null if the URL is not a valid YouTube URL.
     */
    public static String extractVideoId(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        Matcher matcher = YOUTUBE_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }
        if (DIRECT_VIDEO_ID_PATTERN.matcher(trimmed).matches()) {
            return trimmed;
        }
        return null;
    }

    /**
     * Check if a URL is a valid YouTube video URL.
     */
    public static boolean isValidYoutubeUrl(String url) {
        return extractVideoId(url) != null;
    }

    /**
     * Build the embeddable YouTube URL from a video ID.
     */
    public static String buildEmbedUrl(String videoId) {
        if (videoId == null || videoId.isBlank()) {
            return null;
        }
        return "https://www.youtube.com/embed/" + videoId;
    }

    /**
     * Build the thumbnail URL from a video ID.
     */
    public static String buildThumbnailUrl(String videoId) {
        if (videoId == null || videoId.isBlank()) {
            return null;
        }
        return "https://img.youtube.com/vi/" + videoId + "/mqdefault.jpg";
    }

    public static String toEmbedUrl(String videoId) {
        return buildEmbedUrl(videoId);
    }

    public static String toThumbnailUrl(String videoId) {
        if (videoId == null || videoId.isBlank()) {
            return null;
        }
        return "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
    }

    private YoutubeUtil() {}
}
