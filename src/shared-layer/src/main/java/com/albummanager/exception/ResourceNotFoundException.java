package com.albummanager.exception;

import java.util.Map;

/**
 * Exception thrown when a requested resource is not found.
 * HTTP Status: 404 Not Found
 * 
 * Use cases:
 * - Album does not exist
 * - Image does not exist
 * - User has no access to resource
 */
public class ResourceNotFoundException extends AlbumManagerException {

    private static final int HTTP_STATUS = 404;
    private static final String DEFAULT_ERROR_CODE = "RESOURCE_NOT_FOUND";

    /**
     * Constructs a ResourceNotFoundException with a message.
     *
     * @param message Human-readable error message
     */
    public ResourceNotFoundException(String message) {
        super(message, DEFAULT_ERROR_CODE, HTTP_STATUS);
    }

    /**
     * Constructs a ResourceNotFoundException with a specific error code.
     *
     * @param message   Human-readable error message
     * @param errorCode Specific error code (e.g., "ALBUM_NOT_FOUND")
     */
    public ResourceNotFoundException(String message, String errorCode) {
        super(message, errorCode, HTTP_STATUS);
    }

    /**
     * Constructs a ResourceNotFoundException with context data.
     *
     * @param message   Human-readable error message
     * @param errorCode Specific error code
     * @param context   Additional context (userId, albumId, etc.)
     */
    public ResourceNotFoundException(String message, String errorCode, Map<String, Object> context) {
        super(message, errorCode, HTTP_STATUS, context);
    }

    /**
     * Factory method for album not found errors.
     *
     * @param userId  The user ID who owns the album
     * @param albumId The album ID that was not found
     * @return A ResourceNotFoundException configured for album not found
     */
    public static ResourceNotFoundException albumNotFound(String userId, String albumId) {
        return (ResourceNotFoundException) new ResourceNotFoundException(
                "Album not found: " + albumId,
                "ALBUM_NOT_FOUND"
        ).addContext("userId", userId)
         .addContext("albumId", albumId);
    }

    /**
     * Factory method for image not found errors.
     *
     * @param userId  The user ID who owns the image
     * @param albumId The album ID containing the image
     * @param imageId The image ID that was not found
     * @return A ResourceNotFoundException configured for image not found
     */
    public static ResourceNotFoundException imageNotFound(String userId, String albumId, String imageId) {
        return (ResourceNotFoundException) new ResourceNotFoundException(
                "Image not found: " + imageId + " in album: " + albumId,
                "IMAGE_NOT_FOUND"
        ).addContext("userId", userId)
         .addContext("albumId", albumId)
         .addContext("imageId", imageId);
    }

    /**
     * Factory method for S3 object not found errors.
     *
     * @param bucket The S3 bucket name
     * @param s3Key  The S3 object key
     * @return A ResourceNotFoundException configured for S3 object not found
     */
    public static ResourceNotFoundException s3ObjectNotFound(String bucket, String s3Key) {
        return (ResourceNotFoundException) new ResourceNotFoundException(
                "S3 object not found: " + s3Key + " in bucket: " + bucket,
                "S3_OBJECT_NOT_FOUND"
        ).addContext("bucket", bucket)
         .addContext("s3Key", s3Key);
    }
}
