package com.albummanager.models;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AlbumTest {

    private static final String VALID_USER_ID = "auth0|user-123abc";
    private static final String VALID_ALBUM_ID = UUID.randomUUID().toString();
    private static final String VALID_ALBUM_NAME = "My Vacation Images";
    private static final String VALID_DESCRIPTION = "Images from my summer vacation";
    private static final long VALID_CREATED_AT = System.currentTimeMillis();
    private static final long VALID_UPDATED_AT = VALID_CREATED_AT + 1000;

    @Test
    void testValidAlbumCreationWithAllFields() {
        Album album = Album.builder()
            .userId(VALID_USER_ID)
            .albumId(VALID_ALBUM_ID)
            .albumName(VALID_ALBUM_NAME)
            .description(VALID_DESCRIPTION)
            .createdAt(VALID_CREATED_AT)
            .updatedAt(VALID_UPDATED_AT)
            .imageCount(5)
            .build();

        assertEquals(VALID_USER_ID, album.getUserId());
        assertEquals(VALID_ALBUM_ID, album.getAlbumId());
        assertEquals(VALID_ALBUM_ID, album.getId());
        assertEquals(VALID_ALBUM_NAME, album.getAlbumName());
        assertEquals(VALID_DESCRIPTION, album.getDescription());
        assertEquals(VALID_CREATED_AT, album.getCreatedAt());
        assertEquals(VALID_UPDATED_AT, album.getUpdatedAt());
        assertEquals(5, album.getImageCount());
        assertEquals(Entry.ENTITY_TYPE_ALBUM, album.getEntityType());
    }

    @Test
    void testValidAlbumCreationWithNullDescription() {
        Album album = Album.builder()
            .userId(VALID_USER_ID)
            .albumId(VALID_ALBUM_ID)
            .albumName(VALID_ALBUM_NAME)
            .description(null)
            .createdAt(VALID_CREATED_AT)
            .updatedAt(VALID_UPDATED_AT)
            .imageCount(0)
            .build();

        assertNull(album.getDescription());
        assertEquals(0, album.getImageCount());
    }

    @Test
    void testAlbumWithMaxLengthName() {
        String maxLengthName = "a".repeat(255);
        Album album = Album.builder()
            .userId(VALID_USER_ID)
            .albumId(VALID_ALBUM_ID)
            .albumName(maxLengthName)
            .createdAt(VALID_CREATED_AT)
            .updatedAt(VALID_UPDATED_AT)
            .build();

        assertEquals(maxLengthName, album.getAlbumName());
    }

    @Test
    void testAlbumWithMinLengthName() {
        Album album = Album.builder()
            .userId(VALID_USER_ID)
            .albumId(VALID_ALBUM_ID)
            .albumName("a")
            .createdAt(VALID_CREATED_AT)
            .updatedAt(VALID_UPDATED_AT)
            .build();

        assertEquals("a", album.getAlbumName());
    }

    @Test
    void testAlbumNameTrimming() {
        Album album = Album.builder()
            .userId(VALID_USER_ID)
            .albumId(VALID_ALBUM_ID)
            .albumName("  My Album  ")
            .createdAt(VALID_CREATED_AT)
            .updatedAt(VALID_UPDATED_AT)
            .build();

        assertEquals("My Album", album.getAlbumName());
    }

    @Test
    void testInvalidAlbumNameNull() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            Album.builder()
                .userId(VALID_USER_ID)
                .albumId(VALID_ALBUM_ID)
                .albumName(null)
                .createdAt(VALID_CREATED_AT)
                .updatedAt(VALID_UPDATED_AT)
                .build()
        );
        assertTrue(exception.getMessage().contains("albumName cannot be null or blank"));
    }

    @Test
    void testInvalidAlbumNameBlank() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            Album.builder()
                .userId(VALID_USER_ID)
                .albumId(VALID_ALBUM_ID)
                .albumName("   ")
                .createdAt(VALID_CREATED_AT)
                .updatedAt(VALID_UPDATED_AT)
                .build()
        );
        assertTrue(exception.getMessage().contains("albumName cannot be null or blank"));
    }

    @Test
    void testInvalidAlbumNameTooLong() {
        String tooLongName = "a".repeat(256);
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            Album.builder()
                .userId(VALID_USER_ID)
                .albumId(VALID_ALBUM_ID)
                .albumName(tooLongName)
                .createdAt(VALID_CREATED_AT)
                .updatedAt(VALID_UPDATED_AT)
                .build()
        );
        assertTrue(exception.getMessage().contains("albumName must be between 1 and 255 characters"));
    }

    @Test
    void testInvalidDescriptionTooLong() {
        String tooLongDescription = "a".repeat(501);
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            Album.builder()
                .userId(VALID_USER_ID)
                .albumId(VALID_ALBUM_ID)
                .albumName(VALID_ALBUM_NAME)
                .description(tooLongDescription)
                .createdAt(VALID_CREATED_AT)
                .updatedAt(VALID_UPDATED_AT)
                .build()
        );
        assertTrue(exception.getMessage().contains("description cannot exceed 500 characters"));
    }

    @Test
    void testInvalidUserIdNull() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            Album.builder()
                .userId(null)
                .albumId(VALID_ALBUM_ID)
                .albumName(VALID_ALBUM_NAME)
                .createdAt(VALID_CREATED_AT)
                .updatedAt(VALID_UPDATED_AT)
                .build()
        );
        assertTrue(exception.getMessage().contains("userId cannot be null or blank"));
    }

    @Test
    void testInvalidUserIdBlank() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            Album.builder()
                .userId("   ")
                .albumId(VALID_ALBUM_ID)
                .albumName(VALID_ALBUM_NAME)
                .createdAt(VALID_CREATED_AT)
                .updatedAt(VALID_UPDATED_AT)
                .build()
        );
        assertTrue(exception.getMessage().contains("userId cannot be null or blank"));
    }

    @Test
    void testInvalidUserIdTooLong() {
        String tooLongUserId = "a".repeat(129);
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            Album.builder()
                .userId(tooLongUserId)
                .albumId(VALID_ALBUM_ID)
                .albumName(VALID_ALBUM_NAME)
                .createdAt(VALID_CREATED_AT)
                .updatedAt(VALID_UPDATED_AT)
                .build()
        );
        assertTrue(exception.getMessage().contains("userId cannot exceed 128 characters"));
    }

    @Test
    void testInvalidAlbumIdNotUUID() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            Album.builder()
                .userId(VALID_USER_ID)
                .albumId("not-a-uuid")
                .albumName(VALID_ALBUM_NAME)
                .createdAt(VALID_CREATED_AT)
                .updatedAt(VALID_UPDATED_AT)
                .build()
        );
        assertTrue(exception.getMessage().contains("albumId must be a valid UUID v4 format"));
    }

    @Test
    void testInvalidCreatedAtAfterUpdatedAt() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            Album.builder()
                .userId(VALID_USER_ID)
                .albumId(VALID_ALBUM_ID)
                .albumName(VALID_ALBUM_NAME)
                .createdAt(VALID_UPDATED_AT)
                .updatedAt(VALID_CREATED_AT)
                .build()
        );
        assertTrue(exception.getMessage().contains("createdAt cannot be after updatedAt"));
    }

    @Test
    void testInvalidNegativeImageCount() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            Album.builder()
                .userId(VALID_USER_ID)
                .albumId(VALID_ALBUM_ID)
                .albumName(VALID_ALBUM_NAME)
                .createdAt(VALID_CREATED_AT)
                .updatedAt(VALID_UPDATED_AT)
                .imageCount(-1)
                .build()
        );
        assertTrue(exception.getMessage().contains("imageCount cannot be negative"));
    }

    @Test
    void testEqualsAndHashCode() {
        Album album1 = Album.builder()
            .userId(VALID_USER_ID)
            .albumId(VALID_ALBUM_ID)
            .albumName(VALID_ALBUM_NAME)
            .description(VALID_DESCRIPTION)
            .createdAt(VALID_CREATED_AT)
            .updatedAt(VALID_UPDATED_AT)
            .imageCount(5)
            .build();

        Album album2 = Album.builder()
            .userId(VALID_USER_ID)
            .albumId(VALID_ALBUM_ID)
            .albumName(VALID_ALBUM_NAME)
            .description(VALID_DESCRIPTION)
            .createdAt(VALID_CREATED_AT)
            .updatedAt(VALID_UPDATED_AT)
            .imageCount(5)
            .build();

        assertEquals(album1, album2);
        assertEquals(album1.hashCode(), album2.hashCode());
    }

    @Test
    void testNotEquals() {
        Album album1 = Album.builder()
            .userId(VALID_USER_ID)
            .albumId(VALID_ALBUM_ID)
            .albumName(VALID_ALBUM_NAME)
            .createdAt(VALID_CREATED_AT)
            .updatedAt(VALID_UPDATED_AT)
            .build();

        Album album2 = Album.builder()
            .userId(VALID_USER_ID)
            .albumId(UUID.randomUUID().toString())
            .albumName(VALID_ALBUM_NAME)
            .createdAt(VALID_CREATED_AT)
            .updatedAt(VALID_UPDATED_AT)
            .build();

        assertNotEquals(album1, album2);
    }

    @Test
    void testToString() {
        Album album = Album.builder()
            .userId(VALID_USER_ID)
            .albumId(VALID_ALBUM_ID)
            .albumName(VALID_ALBUM_NAME)
            .createdAt(VALID_CREATED_AT)
            .updatedAt(VALID_UPDATED_AT)
            .build();

        String toString = album.toString();
        assertTrue(toString.contains("Album{"));
        assertTrue(toString.contains("userId="));
        assertTrue(toString.contains("albumId="));
        assertTrue(toString.contains("albumName="));
    }

    @Test
    void testImplementsEntry() {
        Album album = Album.builder()
            .userId(VALID_USER_ID)
            .albumId(VALID_ALBUM_ID)
            .albumName(VALID_ALBUM_NAME)
            .createdAt(VALID_CREATED_AT)
            .updatedAt(VALID_UPDATED_AT)
            .build();

        assertTrue(album instanceof Entry);
        Entry entry = album;
        assertEquals(VALID_USER_ID, entry.getUserId());
        assertEquals(VALID_ALBUM_ID, entry.getId());
        assertEquals(VALID_CREATED_AT, entry.getCreatedAt());
        assertEquals(Entry.ENTITY_TYPE_ALBUM, entry.getEntityType());
    }
}
