package com.albummanager.util;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SortKeyBuilderTest {

    private static final String VALID_ALBUM_ID = UUID.randomUUID().toString();
    private static final String VALID_IMAGE_ID = UUID.randomUUID().toString();

    @Test
    void testBuildAlbumSortKey() {
        String sortKey = SortKeyBuilder.buildAlbumSortKey(VALID_ALBUM_ID);
        assertEquals("ALBUM#" + VALID_ALBUM_ID, sortKey);
    }

    @Test
    void testBuildAlbumSortKeyNullAlbumId() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            SortKeyBuilder.buildAlbumSortKey(null)
        );
        assertTrue(exception.getMessage().contains("albumId cannot be null or blank"));
    }

    @Test
    void testBuildAlbumSortKeyBlankAlbumId() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            SortKeyBuilder.buildAlbumSortKey("   ")
        );
        assertTrue(exception.getMessage().contains("albumId cannot be null or blank"));
    }

    @Test
    void testBuildImageSortKey() {
        String sortKey = SortKeyBuilder.buildImageSortKey(VALID_ALBUM_ID, VALID_IMAGE_ID);
        assertEquals("IMAGE#" + VALID_ALBUM_ID + "#" + VALID_IMAGE_ID, sortKey);
    }

    @Test
    void testBuildImageSortKeyNullAlbumId() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            SortKeyBuilder.buildImageSortKey(null, VALID_IMAGE_ID)
        );
        assertTrue(exception.getMessage().contains("albumId cannot be null or blank"));
    }

    @Test
    void testBuildImageSortKeyNullImageId() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            SortKeyBuilder.buildImageSortKey(VALID_ALBUM_ID, null)
        );
        assertTrue(exception.getMessage().contains("imageId cannot be null or blank"));
    }

    @Test
    void testExtractAlbumId() {
        String sortKey = "ALBUM#" + VALID_ALBUM_ID;
        String extractedId = SortKeyBuilder.extractAlbumId(sortKey);
        assertEquals(VALID_ALBUM_ID, extractedId);
    }

    @Test
    void testExtractAlbumIdInvalidFormat() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            SortKeyBuilder.extractAlbumId("INVALID#" + VALID_ALBUM_ID)
        );
        assertTrue(exception.getMessage().contains("Invalid album sort key format"));
    }

    @Test
    void testExtractAlbumIdFromImage() {
        String sortKey = "IMAGE#" + VALID_ALBUM_ID + "#" + VALID_IMAGE_ID;
        String extractedAlbumId = SortKeyBuilder.extractAlbumIdFromImage(sortKey);
        assertEquals(VALID_ALBUM_ID, extractedAlbumId);
    }

    @Test
    void testExtractAlbumIdFromImageInvalidFormat() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            SortKeyBuilder.extractAlbumIdFromImage("ALBUM#" + VALID_ALBUM_ID)
        );
        assertTrue(exception.getMessage().contains("Invalid image sort key format"));
    }

    @Test
    void testExtractImageId() {
        String sortKey = "IMAGE#" + VALID_ALBUM_ID + "#" + VALID_IMAGE_ID;
        String extractedImageId = SortKeyBuilder.extractImageId(sortKey);
        assertEquals(VALID_IMAGE_ID, extractedImageId);
    }

    @Test
    void testExtractImageIdInvalidFormat() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            SortKeyBuilder.extractImageId("ALBUM#" + VALID_ALBUM_ID)
        );
        assertTrue(exception.getMessage().contains("Invalid image sort key format"));
    }

    @Test
    void testAlbumQueryPrefix() {
        String prefix = SortKeyBuilder.albumQueryPrefix();
        assertEquals("ALBUM#", prefix);
    }

    @Test
    void testImageQueryPrefix() {
        String prefix = SortKeyBuilder.imageQueryPrefix(VALID_ALBUM_ID);
        assertEquals("IMAGE#" + VALID_ALBUM_ID + "#", prefix);
    }

    @Test
    void testImageQueryPrefixNullAlbumId() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            SortKeyBuilder.imageQueryPrefix(null)
        );
        assertTrue(exception.getMessage().contains("albumId cannot be null or blank"));
    }

    @Test
    void testIsAlbumSortKeyTrue() {
        String sortKey = "ALBUM#" + VALID_ALBUM_ID;
        assertTrue(SortKeyBuilder.isAlbumSortKey(sortKey));
    }

    @Test
    void testIsAlbumSortKeyFalse() {
        String sortKey = "IMAGE#" + VALID_ALBUM_ID + "#" + VALID_IMAGE_ID;
        assertFalse(SortKeyBuilder.isAlbumSortKey(sortKey));
    }

    @Test
    void testIsAlbumSortKeyNull() {
        assertFalse(SortKeyBuilder.isAlbumSortKey(null));
    }

    @Test
    void testIsAlbumSortKeyBlank() {
        assertFalse(SortKeyBuilder.isAlbumSortKey("   "));
    }

    @Test
    void testIsImageSortKeyTrue() {
        String sortKey = "IMAGE#" + VALID_ALBUM_ID + "#" + VALID_IMAGE_ID;
        assertTrue(SortKeyBuilder.isImageSortKey(sortKey));
    }

    @Test
    void testIsImageSortKeyFalse() {
        String sortKey = "ALBUM#" + VALID_ALBUM_ID;
        assertFalse(SortKeyBuilder.isImageSortKey(sortKey));
    }

    @Test
    void testIsImageSortKeyNull() {
        assertFalse(SortKeyBuilder.isImageSortKey(null));
    }

    @Test
    void testUtilityClassCannotBeInstantiated() throws Exception {
        java.lang.reflect.Constructor<SortKeyBuilder> constructor = 
            SortKeyBuilder.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        Exception exception = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            constructor.newInstance();
        });
        
        // Verify the cause is UnsupportedOperationException
        Throwable cause = exception.getCause();
        assertNotNull(cause);
        assertTrue(cause instanceof UnsupportedOperationException);
        assertTrue(cause.getMessage().contains("Utility class cannot be instantiated"));
    }

    @Test
    void testRoundTripAlbumSortKey() {
        String originalAlbumId = VALID_ALBUM_ID;
        String sortKey = SortKeyBuilder.buildAlbumSortKey(originalAlbumId);
        String extractedAlbumId = SortKeyBuilder.extractAlbumId(sortKey);
        assertEquals(originalAlbumId, extractedAlbumId);
    }

    @Test
    void testRoundTripImageSortKey() {
        String originalAlbumId = VALID_ALBUM_ID;
        String originalImageId = VALID_IMAGE_ID;
        String sortKey = SortKeyBuilder.buildImageSortKey(originalAlbumId, originalImageId);
        String extractedAlbumId = SortKeyBuilder.extractAlbumIdFromImage(sortKey);
        String extractedImageId = SortKeyBuilder.extractImageId(sortKey);
        assertEquals(originalAlbumId, extractedAlbumId);
        assertEquals(originalImageId, extractedImageId);
    }
}
