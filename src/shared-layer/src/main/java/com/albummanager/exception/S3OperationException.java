package com.albummanager.exception;

import java.util.Map;

/**
 * Exception thrown when an S3 operation fails.
 * HTTP Status: 500 Internal Server Error or 503 Service Unavailable
 * 
 * Use cases:
 * - Presigned URL generation failure
 * - Object upload/download failure
 * - Object deletion failure
 * - S3 service unavailable (retryable)
 */
public class S3OperationException extends AlbumManagerException {

    private static final int DEFAULT_HTTP_STATUS = 500;
    private static final String DEFAULT_ERROR_CODE = "S3_OPERATION_FAILED";

    private final boolean retryable;

    /**
     * Constructs an S3OperationException with a message.
     *
     * @param message Human-readable error message
     */
    public S3OperationException(String message) {
        super(message, DEFAULT_ERROR_CODE, DEFAULT_HTTP_STATUS);
        this.retryable = false;
    }

    /**
     * Constructs an S3OperationException with a cause.
     *
     * @param message Human-readable error message
     * @param cause   The underlying cause (typically SDK exception)
     */
    public S3OperationException(String message, Throwable cause) {
        super(message, DEFAULT_ERROR_CODE, DEFAULT_HTTP_STATUS, cause);
        this.retryable = false;
    }

    /**
     * Constructs an S3OperationException with retryable flag.
     *
     * @param message   Human-readable error message
     * @param retryable Whether the operation can be retried
     * @param cause     The underlying cause
     */
    public S3OperationException(String message, boolean retryable, Throwable cause) {
        super(message, DEFAULT_ERROR_CODE, retryable ? 503 : DEFAULT_HTTP_STATUS, cause);
        this.retryable = retryable;
    }

    /**
     * Constructs an S3OperationException with context and cause.
     *
     * @param message   Human-readable error message
     * @param errorCode Specific error code
     * @param context   Additional context (bucket, s3Key, etc.)
     * @param cause     The underlying cause
     */
    public S3OperationException(String message, String errorCode, Map<String, Object> context, Throwable cause) {
        super(message, errorCode, DEFAULT_HTTP_STATUS, context, cause);
        this.retryable = false;
    }

    /**
     * Constructs an S3OperationException with all parameters.
     *
     * @param message    Human-readable error message
     * @param errorCode  Specific error code
     * @param httpStatus HTTP status code (500 or 503)
     * @param retryable  Whether the operation can be retried
     * @param context    Additional context
     * @param cause      The underlying cause
     */
    public S3OperationException(String message, String errorCode, int httpStatus, 
                                 boolean retryable, Map<String, Object> context, Throwable cause) {
        super(message, errorCode, httpStatus, context, cause);
        this.retryable = retryable;
    }

    /**
     * Returns whether this operation can be retried.
     *
     * @return true if the operation is retryable, false otherwise
     */
    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Factory method for presigned URL generation failures.
     *
     * @param bucket The S3 bucket name
     * @param s3Key  The S3 object key
     * @param cause  The underlying cause
     * @return An S3OperationException configured for presigned URL failure
     */
    public static S3OperationException presignedUrlFailed(String bucket, String s3Key, Throwable cause) {
        S3OperationException ex = new S3OperationException(
                "Failed to generate presigned URL for: " + s3Key, cause);
        ex.addContext("bucket", bucket);
        ex.addContext("s3Key", s3Key);
        ex.addContext("operation", "generatePresignedUrl");
        return ex;
    }

    /**
     * Factory method for upload failures.
     *
     * @param bucket The S3 bucket name
     * @param s3Key  The S3 object key
     * @param cause  The underlying cause
     * @return An S3OperationException configured for upload failure
     */
    public static S3OperationException uploadFailed(String bucket, String s3Key, Throwable cause) {
        S3OperationException ex = new S3OperationException(
                "Failed to upload object to S3: " + s3Key, true, cause);
        ex.addContext("bucket", bucket);
        ex.addContext("s3Key", s3Key);
        ex.addContext("operation", "upload");
        return ex;
    }

    /**
     * Factory method for download failures.
     *
     * @param bucket The S3 bucket name
     * @param s3Key  The S3 object key
     * @param cause  The underlying cause
     * @return An S3OperationException configured for download failure
     */
    public static S3OperationException downloadFailed(String bucket, String s3Key, Throwable cause) {
        S3OperationException ex = new S3OperationException(
                "Failed to download object from S3: " + s3Key, true, cause);
        ex.addContext("bucket", bucket);
        ex.addContext("s3Key", s3Key);
        ex.addContext("operation", "download");
        return ex;
    }

    /**
     * Factory method for delete failures.
     *
     * @param bucket The S3 bucket name
     * @param s3Key  The S3 object key
     * @param cause  The underlying cause
     * @return An S3OperationException configured for delete failure
     */
    public static S3OperationException deleteFailed(String bucket, String s3Key, Throwable cause) {
        S3OperationException ex = new S3OperationException(
                "Failed to delete object from S3: " + s3Key, cause);
        ex.addContext("bucket", bucket);
        ex.addContext("s3Key", s3Key);
        ex.addContext("operation", "delete");
        return ex;
    }

    @Override
    public String toString() {
        return super.toString().replace("}", ", retryable=" + retryable + "}");
    }
}
