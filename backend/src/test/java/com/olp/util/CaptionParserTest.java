package com.olp.util;

import com.olp.util.CaptionParser.Cue;
import com.olp.util.CaptionParser.Format;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CaptionParserTest {

    private static final String VTT = """
            WEBVTT

            NOTE a comment block

            intro
            00:00:01.000 --> 00:00:03.500 align:start
            Hello <b>world</b>

            01:02.000 --> 01:05.250
            Line one
            Line two
            """;

    private static final String SRT = """
            1
            00:00:01,000 --> 00:00:03,500
            Hello world

            2
            00:01:02,000 --> 00:01:05,250
            Line one
            Line two
            """;

    // ===== VTT =====

    @Test
    void parseVtt_ValidFile() {
        List<Cue> cues = CaptionParser.parse(VTT, Format.VTT);

        assertEquals(2, cues.size());
        assertEquals(new Cue(1000, 3500, "Hello <b>world</b>"), cues.get(0));
        assertEquals(new Cue(62_000, 65_250, "Line one\nLine two"), cues.get(1));
    }

    @Test
    void parseVtt_HourOptionalAndLongHours() {
        String vtt = "WEBVTT\n\n100:00:00.000 --> 100:00:01.000\nLong video";
        assertEquals(360_000_000L, CaptionParser.parse(vtt, Format.VTT).get(0).startMs());
    }

    @Test
    void parseVtt_MissingHeader_Rejected() {
        String vtt = "00:00:01.000 --> 00:00:02.000\nHi";
        assertThrows(IllegalArgumentException.class, () -> CaptionParser.parse(vtt, Format.VTT));
    }

    @Test
    void parseVtt_CommaMillisecondSeparator_Rejected() {
        String vtt = "WEBVTT\n\n00:00:01,000 --> 00:00:02,000\nHi";
        assertThrows(IllegalArgumentException.class, () -> CaptionParser.parse(vtt, Format.VTT));
    }

    @Test
    void parseVtt_OnlyHeader_NoCues_Rejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> CaptionParser.parse("WEBVTT\n\nNOTE nothing here", Format.VTT));
        assertTrue(ex.getMessage().contains("no cues"));
    }

    // ===== SRT =====

    @Test
    void parseSrt_ValidFile() {
        List<Cue> cues = CaptionParser.parse(SRT, Format.SRT);

        assertEquals(2, cues.size());
        assertEquals(new Cue(1000, 3500, "Hello world"), cues.get(0));
        assertEquals(new Cue(62_000, 65_250, "Line one\nLine two"), cues.get(1));
    }

    @Test
    void parseSrt_WindowsLineEndingsAndBom() {
        String crlf = "﻿" + SRT.replace("\n", "\r\n");
        assertEquals(2, CaptionParser.parse(crlf, Format.SRT).size());
    }

    @Test
    void parseSrt_DotMillisecondSeparator_Rejected() {
        String srt = "1\n00:00:01.000 --> 00:00:02.000\nHi";
        assertThrows(IllegalArgumentException.class, () -> CaptionParser.parse(srt, Format.SRT));
    }

    @Test
    void parseSrt_NonNumericIndex_Rejected() {
        String srt = "abc\n00:00:01,000 --> 00:00:02,000\nHi";
        assertThrows(IllegalArgumentException.class, () -> CaptionParser.parse(srt, Format.SRT));
    }

    // ===== Timestamp validation (both formats) =====

    @Test
    void parse_EndNotAfterStart_Rejected() {
        String same = "1\n00:00:02,000 --> 00:00:02,000\nHi";
        String reversed = "1\n00:00:03,000 --> 00:00:02,000\nHi";
        assertThrows(IllegalArgumentException.class, () -> CaptionParser.parse(same, Format.SRT));
        assertThrows(IllegalArgumentException.class, () -> CaptionParser.parse(reversed, Format.SRT));
    }

    @Test
    void parse_CueStartsBeforePreviousCue_Rejected() {
        String srt = "1\n00:00:05,000 --> 00:00:06,000\nA\n\n2\n00:00:01,000 --> 00:00:02,000\nB";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> CaptionParser.parse(srt, Format.SRT));
        assertTrue(ex.getMessage().contains("Cue 2"));
    }

    @Test
    void parse_OverlappingCues_Allowed() {
        String srt = "1\n00:00:01,000 --> 00:00:05,000\nA\n\n2\n00:00:03,000 --> 00:00:06,000\nB";
        assertEquals(2, CaptionParser.parse(srt, Format.SRT).size());
    }

    @Test
    void parse_MinutesOrSecondsOutOfRange_Rejected() {
        String badSeconds = "1\n00:00:61,000 --> 00:01:02,000\nHi";
        String badMinutes = "1\n00:60:00,000 --> 01:00:01,000\nHi";
        assertThrows(IllegalArgumentException.class, () -> CaptionParser.parse(badSeconds, Format.SRT));
        assertThrows(IllegalArgumentException.class, () -> CaptionParser.parse(badMinutes, Format.SRT));
    }

    @Test
    void parse_MalformedTimestamp_Rejected() {
        String srt = "1\n0:0:1,0 --> 00:00:02,000\nHi";
        assertThrows(IllegalArgumentException.class, () -> CaptionParser.parse(srt, Format.SRT));
    }

    @Test
    void parse_CueWithoutText_Rejected() {
        String srt = "1\n00:00:01,000 --> 00:00:02,000\n\n2\n00:00:03,000 --> 00:00:04,000\nHi";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> CaptionParser.parse(srt, Format.SRT));
        assertTrue(ex.getMessage().contains("Cue 1") || ex.getMessage().contains("no timestamp"));
    }

    @Test
    void parse_BlockWithoutTimestamp_Rejected() {
        String srt = "1\n00:00:01,000 --> 00:00:02,000\nHi\n\n2\njust text";
        assertThrows(IllegalArgumentException.class, () -> CaptionParser.parse(srt, Format.SRT));
    }

    // ===== Input guards =====

    @Test
    void parse_NullBlankOrNullFormat_Rejected() {
        assertThrows(IllegalArgumentException.class, () -> CaptionParser.parse(null, Format.VTT));
        assertThrows(IllegalArgumentException.class, () -> CaptionParser.parse("   \n ", Format.SRT));
        assertThrows(IllegalArgumentException.class, () -> CaptionParser.parse(SRT, null));
    }

    @Test
    void parse_TooManyCues_Rejected() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20_001; i++) {
            long s = i * 1000L;
            sb.append(i + 1).append('\n')
              .append(String.format("%02d:%02d:%02d,000 --> %02d:%02d:%02d,500",
                      s / 3_600_000, (s / 60_000) % 60, (s / 1000) % 60,
                      s / 3_600_000, (s / 60_000) % 60, (s / 1000) % 60))
              .append("\nx\n\n");
        }
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> CaptionParser.parse(sb.toString(), Format.SRT));
        assertTrue(ex.getMessage().contains("more than"));
    }

    // ===== Detection and conversion =====

    @Test
    void detectFormat_ByHeader() {
        assertEquals(Format.VTT, CaptionParser.detectFormat(VTT));
        assertEquals(Format.VTT, CaptionParser.detectFormat("﻿WEBVTT\n\n"));
        assertEquals(Format.SRT, CaptionParser.detectFormat(SRT));
    }

    @Test
    void parse_AutoDetect() {
        assertEquals(2, CaptionParser.parse(VTT).size());
        assertEquals(2, CaptionParser.parse(SRT).size());
    }

    @Test
    void toVtt_RoundTripsFromSrt() {
        List<Cue> fromSrt = CaptionParser.parse(SRT, Format.SRT);
        String vtt = CaptionParser.toVtt(fromSrt);

        assertTrue(vtt.startsWith("WEBVTT\n\n00:00:01.000 --> 00:00:03.500\nHello world"));
        assertTrue(vtt.contains("00:01:02.000 --> 00:01:05.250"));
        assertEquals(fromSrt, CaptionParser.parse(vtt, Format.VTT));
    }
}
