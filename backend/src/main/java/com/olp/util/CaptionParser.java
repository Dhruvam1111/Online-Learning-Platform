package com.olp.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and validates WebVTT / SRT caption files (FR-I67) and converts them to WebVTT.
 * <p>
 * Pure utility: no persistence and no dependency on lessons or videos. Invalid input
 * raises {@link IllegalArgumentException}, which the API layer maps to HTTP 400.
 * <p>
 * Timestamp rules: every cue must have {@code start < end}, minutes/seconds below 60,
 * and cue start times must not go backwards. Overlapping cues are allowed because
 * real caption files use them for simultaneous speakers.
 */
public final class CaptionParser {

    public enum Format { VTT, SRT }

    /** A single caption cue; times are milliseconds from the start of the video. */
    public record Cue(long startMs, long endMs, String text) {}

    private static final int MAX_CUES = 20_000;
    private static final Pattern TIMING_LINE = Pattern.compile("^(\\S+)\\s+-->\\s+(\\S+)(?:\\s+.*)?$");
    private static final Pattern VTT_TIME = Pattern.compile("^(?:(\\d{2,}):)?(\\d{2}):(\\d{2})\\.(\\d{3})$");
    private static final Pattern SRT_TIME = Pattern.compile("^(\\d{2,}):(\\d{2}):(\\d{2}),(\\d{3})$");
    private static final Pattern SRT_INDEX = Pattern.compile("^\\d+$");

    /**
     * Guess the format from the content: a leading {@code WEBVTT} header means VTT, otherwise SRT.
     */
    public static Format detectFormat(String content) {
        String normalized = normalize(content);
        return normalized.startsWith("WEBVTT") ? Format.VTT : Format.SRT;
    }

    /** Parse with auto-detected format. */
    public static List<Cue> parse(String content) {
        return parse(content, detectFormat(content));
    }

    /**
     * Parse and validate captions in the given format.
     *
     * @throws IllegalArgumentException if the content is empty, malformed, or has invalid timestamps
     */
    public static List<Cue> parse(String content, Format format) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Caption file is empty");
        }
        if (format == null) {
            throw new IllegalArgumentException("Caption format is required");
        }

        String normalized = normalize(content);
        if (format == Format.VTT && !normalized.startsWith("WEBVTT")) {
            throw new IllegalArgumentException("Invalid WebVTT file: missing WEBVTT header");
        }

        List<Cue> cues = new ArrayList<>();
        for (String block : normalized.split("\n{2,}")) {
            if (block.isBlank()) {
                continue;
            }
            Cue cue = parseBlock(block, format, cues.size() + 1);
            if (cue == null) {
                continue; // WebVTT header / NOTE / STYLE / REGION block
            }
            if (!cues.isEmpty() && cue.startMs() < cues.get(cues.size() - 1).startMs()) {
                throw new IllegalArgumentException(
                        "Cue " + (cues.size() + 1) + " starts before the previous cue");
            }
            cues.add(cue);
            if (cues.size() > MAX_CUES) {
                throw new IllegalArgumentException("Caption file has more than " + MAX_CUES + " cues");
            }
        }

        if (cues.isEmpty()) {
            throw new IllegalArgumentException("Caption file contains no cues");
        }
        return List.copyOf(cues);
    }

    /** Render cues as a WebVTT document (used to store SRT uploads in one playable format). */
    public static String toVtt(List<Cue> cues) {
        StringBuilder sb = new StringBuilder("WEBVTT\n\n");
        for (Cue cue : cues) {
            sb.append(formatVttTime(cue.startMs()))
              .append(" --> ")
              .append(formatVttTime(cue.endMs()))
              .append('\n')
              .append(cue.text())
              .append("\n\n");
        }
        return sb.toString();
    }

    private static Cue parseBlock(String block, Format format, int cueNumber) {
        String[] lines = block.split("\n");

        int timingIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("-->")) {
                timingIndex = i;
                break;
            }
        }

        if (timingIndex < 0) {
            if (format == Format.VTT && isVttNonCueBlock(lines[0])) {
                return null;
            }
            throw new IllegalArgumentException("Cue " + cueNumber + " has no timestamp line");
        }

        if (format == Format.SRT && timingIndex > 0
                && !(timingIndex == 1 && SRT_INDEX.matcher(lines[0].trim()).matches())) {
            throw new IllegalArgumentException("Cue " + cueNumber + " has an invalid index line");
        }
        if (format == Format.VTT && timingIndex > 1) {
            throw new IllegalArgumentException("Cue " + cueNumber + " has an invalid identifier");
        }

        Matcher timing = TIMING_LINE.matcher(lines[timingIndex].trim());
        if (!timing.matches()) {
            throw new IllegalArgumentException("Cue " + cueNumber + " has a malformed timestamp line");
        }
        long start = parseTime(timing.group(1), format, cueNumber);
        long end = parseTime(timing.group(2), format, cueNumber);
        if (start >= end) {
            throw new IllegalArgumentException("Cue " + cueNumber + " must end after it starts");
        }

        StringBuilder text = new StringBuilder();
        for (int i = timingIndex + 1; i < lines.length; i++) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(lines[i]);
        }
        String body = text.toString().trim();
        if (body.isEmpty()) {
            throw new IllegalArgumentException("Cue " + cueNumber + " has no text");
        }
        return new Cue(start, end, body);
    }

    private static boolean isVttNonCueBlock(String firstLine) {
        return firstLine.startsWith("WEBVTT")
                || firstLine.startsWith("NOTE")
                || firstLine.startsWith("STYLE")
                || firstLine.startsWith("REGION");
    }

    private static long parseTime(String raw, Format format, int cueNumber) {
        Matcher m = (format == Format.VTT ? VTT_TIME : SRT_TIME).matcher(raw);
        if (!m.matches()) {
            throw new IllegalArgumentException("Cue " + cueNumber + " has an invalid timestamp: " + raw);
        }
        long hours = m.group(1) == null ? 0 : Long.parseLong(m.group(1));
        long minutes = Long.parseLong(m.group(2));
        long seconds = Long.parseLong(m.group(3));
        long millis = Long.parseLong(m.group(4));
        if (minutes > 59 || seconds > 59) {
            throw new IllegalArgumentException("Cue " + cueNumber + " has an out-of-range timestamp: " + raw);
        }
        return ((hours * 60 + minutes) * 60 + seconds) * 1000 + millis;
    }

    private static String formatVttTime(long ms) {
        long hours = ms / 3_600_000;
        long minutes = (ms / 60_000) % 60;
        long seconds = (ms / 1000) % 60;
        long millis = ms % 1000;
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }

    private static String normalize(String content) {
        if (content == null) {
            return "";
        }
        String s = content.startsWith("﻿") ? content.substring(1) : content;
        return s.replace("\r\n", "\n").replace('\r', '\n').strip();
    }

    private CaptionParser() {}
}
