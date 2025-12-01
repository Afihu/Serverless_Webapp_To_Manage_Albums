package com.albummanager.exception;

import java.util.Map;

/**
 * Exception thrown when attempting to create an album with a name that already exists.
 * HTTP Status: 409 Conflict
 * 
 * Use cases:
 * - Creating a new album with an existing name for the same user
 * - Renaming an album to a name that already exists
 */
public class DuplicateAlbumException extends AlbumManagerException {

    private static final int HTTP_STATUS = 409;
    private static final String DEFAULT_ERROR_CODE = "DUPLICATE_ALBUM";

    /**
     * Constructs a DuplicateAlbumException with a message.
     *
     * @param message Human-readable error message
     */
    public DuplicateAlbumException(String message) {
        super(message, DEFAULT_ERROR_CODE, HTTP_STATUS);
    }

    /**
     * Constructs a DuplicateAlbumException with context data.
     *
     * @param message Human-readable error message
     * @param context Additional context (userId, albumName, etc.)
     */
    public DuplicateAlbumException(String message, Map<String, Object> context) {
        super(message, DEFAULT_ERROR_CODE, HTTP_STATUS, context);
    }

    /**
     * Constructs a DuplicateAlbumException with a specific error code.
     *
     * @param message   Human-readable error message
     * @param errorCode Specific error code
     */
    public DuplicateAlbumException(String message, String errorCode) {
        super(message, errorCode, HTTP_STATUS);
    }

    /**
     * Factory method for duplicate album name errors.
     *
     * @param userId    The user ID who owns the album
     * @param albumName The duplicate album name
     * @return A DuplicateAlbumException with context
     */
    public static DuplicateAlbumException forAlbumName(String userId, String albumName) {
        return (DuplicateAlbumException) new DuplicateAlbumException(
                "An album with the name '" + albumName + "' already exists"
        ).addContext("userId", userId)
         .addContext("albumName", albumName);
    }

    /**
     * Factory method for duplicate album name on rename operation.
     *
     * @param userId       The user ID who owns the album
     * @param albumId      The album ID being renamed
     * @param newAlbumName The new name that conflicts
     * @return A DuplicateAlbumException with context
     */
    public static DuplicateAlbumException forRename(String userId, String albumId, String newAlbumName) {
        return (DuplicateAlbumException) new DuplicateAlbumException(
                "Cannot rename album to '" + newAlbumName + "': an album with this name already exists",
                "DUPLICATE_ALBUM_ON_RENAME"
        ).addContext("userId", userId)
         .addContext("albumId", albumId)
         .addContext("newAlbumName", newAlbumName);
    }
}
