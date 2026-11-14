package com.olp.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class YoutubeUtilTest {

    private static final String ID = "dQw4w9WgXcQ";

    @Test
    void testExtractVideoId_StandardWatchUrl() {
        assertEquals(ID, YoutubeUtil.extractVideoId("https://www.youtube.com/watch?v=" + ID));
    }

    @Test
    void testExtractVideoId_WatchUrlWithoutWww() {
        assertEquals(ID, YoutubeUtil.extractVideoId("https://youtube.com/watch?v=" + ID));
    }

    @Test
    void testExtractVideoId_WatchUrlWithExtraParamsBeforeV() {
        assertEquals(ID, YoutubeUtil.extractVideoId("https://www.youtube.com/watch?feature=shared&v=" + ID));
    }

    @Test
    void testExtractVideoId_ShortLink() {
        assertEquals(ID, YoutubeUtil.extractVideoId("https://youtu.be/" + ID));
    }

    @Test
    void testExtractVideoId_EmbedUrl() {
        assertEquals(ID, YoutubeUtil.extractVideoId("https://www.youtube.com/embed/" + ID));
    }

    @Test
    void testExtractVideoId_ShortsUrl() {
        assertEquals(ID, YoutubeUtil.extractVideoId("https://www.youtube.com/shorts/" + ID));
    }

    @Test
    void testExtractVideoId_LegacyVUrl() {
        assertEquals(ID, YoutubeUtil.extractVideoId("https://www.youtube.com/v/" + ID));
    }

    @Test
    void testExtractVideoId_WithoutScheme() {
        assertEquals(ID, YoutubeUtil.extractVideoId("youtube.com/watch?v=" + ID));
    }

    @Test
    void testExtractVideoId_TrimsSurroundingWhitespace() {
        assertEquals(ID, YoutubeUtil.extractVideoId("   https://youtu.be/" + ID + "   "));
    }

    @Test
    void testExtractVideoId_BareElevenCharacterId() {
        assertEquals(ID, YoutubeUtil.extractVideoId(ID));
    }

    @Test
    void testExtractVideoId_BareIdWithUnderscoreAndDash() {
        assertEquals("a_b-c_d-e_f", YoutubeUtil.extractVideoId("a_b-c_d-e_f"));
    }

    @Test
    void testExtractVideoId_NullBlankAndEmptyReturnNull() {
        assertNull(YoutubeUtil.extractVideoId(null));
        assertNull(YoutubeUtil.extractVideoId(""));
        assertNull(YoutubeUtil.extractVideoId("    "));
    }

    @Test
    void testExtractVideoId_NonYoutubeUrlReturnsNull() {
        assertNull(YoutubeUtil.extractVideoId("https://vimeo.com/123456789"));
        assertNull(YoutubeUtil.extractVideoId("https://example.com/video"));
    }

    @Test
    void testExtractVideoId_TooShortIdReturnsNull() {
        assertNull(YoutubeUtil.extractVideoId("https://youtu.be/short"));
        assertNull(YoutubeUtil.extractVideoId("abcdefghij"));
    }

    @Test
    void testExtractVideoId_BareTwelveCharacterStringReturnsNull() {
        assertNull(YoutubeUtil.extractVideoId("abcdefghijkl"));
    }

    @Test
    void testExtractVideoId_BareElevenCharsWithInvalidSymbolReturnsNull() {
        assertNull(YoutubeUtil.extractVideoId("abcde!ghijk"));
    }

    @Test
    void testIsValidYoutubeUrl() {
        assertTrue(YoutubeUtil.isValidYoutubeUrl("https://youtu.be/" + ID));
        assertTrue(YoutubeUtil.isValidYoutubeUrl(ID));
        assertFalse(YoutubeUtil.isValidYoutubeUrl("https://example.com"));
        assertFalse(YoutubeUtil.isValidYoutubeUrl(null));
    }

    @Test
    void testBuildEmbedUrl() {
        assertEquals("https://www.youtube.com/embed/" + ID, YoutubeUtil.buildEmbedUrl(ID));
    }

    @Test
    void testBuildEmbedUrl_NullAndBlankReturnNull() {
        assertNull(YoutubeUtil.buildEmbedUrl(null));
        assertNull(YoutubeUtil.buildEmbedUrl(" "));
    }

    @Test
    void testToEmbedUrl_DelegatesToBuildEmbedUrl() {
        assertEquals(YoutubeUtil.buildEmbedUrl(ID), YoutubeUtil.toEmbedUrl(ID));
        assertNull(YoutubeUtil.toEmbedUrl(null));
    }

    @Test
    void testBuildThumbnailUrl_UsesMediumQuality() {
        assertEquals("https://img.youtube.com/vi/" + ID + "/mqdefault.jpg", YoutubeUtil.buildThumbnailUrl(ID));
    }

    @Test
    void testBuildThumbnailUrl_NullAndBlankReturnNull() {
        assertNull(YoutubeUtil.buildThumbnailUrl(null));
        assertNull(YoutubeUtil.buildThumbnailUrl(""));
    }

    @Test
    void testToThumbnailUrl_UsesHighQuality() {
        assertEquals("https://img.youtube.com/vi/" + ID + "/hqdefault.jpg", YoutubeUtil.toThumbnailUrl(ID));
    }

    @Test
    void testToThumbnailUrl_NullAndBlankReturnNull() {
        assertNull(YoutubeUtil.toThumbnailUrl(null));
        assertNull(YoutubeUtil.toThumbnailUrl("  "));
    }
}
