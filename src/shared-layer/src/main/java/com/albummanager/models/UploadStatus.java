package com.albummanager.models;

/**
 * Status of an image upload to S3.
 * Tracks the lifecycle of an image from initial request to completion.
 */
public enum UploadStatus {
    /**
     * Upload has been initiated but the file has not been uploaded to S3 yet.
     * The presigned URL has been generated and provided to the client.
     */
    PENDING,

    /**
     * Upload has been successfully completed.
     * The file exists in S3 and is ready for processing.
     */
    COMPLETED,

    /**
     * Upload has failed.
     * This could be due to client timeout, S3 errors, or validation failures.
     */
    FAILED;

    /**
     * Parses a string value to UploadStatus (case-insensitive).
     *
     * @param value the string value to parse
     * @return the matching UploadStatus
     * @throws IllegalArgumentException if the value is not a valid status
     */
    public static UploadStatus fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Upload status cannot be null");
        }
        try {
            return UploadStatus.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid upload status: " + value + 
                ". Valid values are: PENDING, COMPLETED, FAILED");
        }
    }

    /**
     * Checks if the given string is a valid upload status.
     *
     * @param value the string value to check
     * @return true if the value is a valid status, false otherwise
     */
    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        try {
            UploadStatus.valueOf(value.toUpperCase().trim());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
