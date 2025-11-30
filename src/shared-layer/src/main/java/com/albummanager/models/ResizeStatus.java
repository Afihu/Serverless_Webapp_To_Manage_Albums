package com.albummanager.models;

/**
 * Status of image resize/processing operation.
 * Tracks the lifecycle of thumbnail and resize generation.
 */
public enum ResizeStatus {
    /**
     * Resize has not started yet.
     * The original image has been uploaded but not processed.
     */
    PENDING,

    /**
     * Resize has been successfully completed.
     * Thumbnails have been generated and stored in S3.
     */
    COMPLETED,

    /**
     * Resize has failed.
     * This could be due to unsupported format, corrupted image, or processing errors.
     */
    FAILED;

    /**
     * Parses a string value to ResizeStatus (case-insensitive).
     *
     * @param value the string value to parse
     * @return the matching ResizeStatus
     * @throws IllegalArgumentException if the value is not a valid status
     */
    public static ResizeStatus fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Resize status cannot be null");
        }
        try {
            return ResizeStatus.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid resize status: " + value + 
                ". Valid values are: PENDING, COMPLETED, FAILED");
        }
    }

    /**
     * Checks if the given string is a valid resize status.
     *
     * @param value the string value to check
     * @return true if the value is a valid status, false otherwise
     */
    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        try {
            ResizeStatus.valueOf(value.toUpperCase().trim());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
