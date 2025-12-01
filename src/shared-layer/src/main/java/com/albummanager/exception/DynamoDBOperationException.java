package com.albummanager.exception;

import java.util.Map;

/**
 * Exception thrown when a DynamoDB operation fails.
 * HTTP Status: 500 Internal Server Error or 503 Service Unavailable
 * 
 * Use cases:
 * - Put/Get/Update/Delete item failures
 * - Query/Scan failures
 * - Conditional check failures (other than expected conflicts)
 * - DynamoDB service unavailable (retryable)
 * - Throughput exceeded (retryable)
 */
public class DynamoDBOperationException extends AlbumManagerException {

    private static final int DEFAULT_HTTP_STATUS = 500;
    private static final String DEFAULT_ERROR_CODE = "DYNAMODB_OPERATION_FAILED";

    private final boolean retryable;

    /**
     * Constructs a DynamoDBOperationException with a message.
     *
     * @param message Human-readable error message
     */
    public DynamoDBOperationException(String message) {
        super(message, DEFAULT_ERROR_CODE, DEFAULT_HTTP_STATUS);
        this.retryable = false;
    }

    /**
     * Constructs a DynamoDBOperationException with a cause.
     *
     * @param message Human-readable error message
     * @param cause   The underlying cause (typically SDK exception)
     */
    public DynamoDBOperationException(String message, Throwable cause) {
        super(message, DEFAULT_ERROR_CODE, DEFAULT_HTTP_STATUS, cause);
        this.retryable = false;
    }

    /**
     * Constructs a DynamoDBOperationException with retryable flag.
     *
     * @param message   Human-readable error message
     * @param retryable Whether the operation can be retried
     * @param cause     The underlying cause
     */
    public DynamoDBOperationException(String message, boolean retryable, Throwable cause) {
        super(message, DEFAULT_ERROR_CODE, retryable ? 503 : DEFAULT_HTTP_STATUS, cause);
        this.retryable = retryable;
    }

    /**
     * Constructs a DynamoDBOperationException with context and cause.
     *
     * @param message   Human-readable error message
     * @param errorCode Specific error code
     * @param context   Additional context (tableName, userId, etc.)
     * @param cause     The underlying cause
     */
    public DynamoDBOperationException(String message, String errorCode, Map<String, Object> context, Throwable cause) {
        super(message, errorCode, DEFAULT_HTTP_STATUS, context, cause);
        this.retryable = false;
    }

    /**
     * Constructs a DynamoDBOperationException with all parameters.
     *
     * @param message    Human-readable error message
     * @param errorCode  Specific error code
     * @param httpStatus HTTP status code (500 or 503)
     * @param retryable  Whether the operation can be retried
     * @param context    Additional context
     * @param cause      The underlying cause
     */
    public DynamoDBOperationException(String message, String errorCode, int httpStatus,
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
     * Factory method for put item failures.
     *
     * @param tableName The DynamoDB table name
     * @param userId    The partition key value
     * @param sortKey   The sort key value
     * @param cause     The underlying cause
     * @return A DynamoDBOperationException configured for put failure
     */
    public static DynamoDBOperationException putItemFailed(String tableName, String userId, 
                                                            String sortKey, Throwable cause) {
        DynamoDBOperationException ex = new DynamoDBOperationException(
                "Failed to put item in DynamoDB table: " + tableName, cause);
        ex.addContext("tableName", tableName);
        ex.addContext("userId", userId);
        ex.addContext("sortKey", sortKey);
        ex.addContext("operation", "putItem");
        return ex;
    }

    /**
     * Factory method for get item failures.
     *
     * @param tableName The DynamoDB table name
     * @param userId    The partition key value
     * @param sortKey   The sort key value
     * @param cause     The underlying cause
     * @return A DynamoDBOperationException configured for get failure
     */
    public static DynamoDBOperationException getItemFailed(String tableName, String userId,
                                                            String sortKey, Throwable cause) {
        DynamoDBOperationException ex = new DynamoDBOperationException(
                "Failed to get item from DynamoDB table: " + tableName, cause);
        ex.addContext("tableName", tableName);
        ex.addContext("userId", userId);
        ex.addContext("sortKey", sortKey);
        ex.addContext("operation", "getItem");
        return ex;
    }

    /**
     * Factory method for update item failures.
     *
     * @param tableName The DynamoDB table name
     * @param userId    The partition key value
     * @param sortKey   The sort key value
     * @param cause     The underlying cause
     * @return A DynamoDBOperationException configured for update failure
     */
    public static DynamoDBOperationException updateItemFailed(String tableName, String userId,
                                                               String sortKey, Throwable cause) {
        DynamoDBOperationException ex = new DynamoDBOperationException(
                "Failed to update item in DynamoDB table: " + tableName, cause);
        ex.addContext("tableName", tableName);
        ex.addContext("userId", userId);
        ex.addContext("sortKey", sortKey);
        ex.addContext("operation", "updateItem");
        return ex;
    }

    /**
     * Factory method for delete item failures.
     *
     * @param tableName The DynamoDB table name
     * @param userId    The partition key value
     * @param sortKey   The sort key value
     * @param cause     The underlying cause
     * @return A DynamoDBOperationException configured for delete failure
     */
    public static DynamoDBOperationException deleteItemFailed(String tableName, String userId,
                                                               String sortKey, Throwable cause) {
        DynamoDBOperationException ex = new DynamoDBOperationException(
                "Failed to delete item from DynamoDB table: " + tableName, cause);
        ex.addContext("tableName", tableName);
        ex.addContext("userId", userId);
        ex.addContext("sortKey", sortKey);
        ex.addContext("operation", "deleteItem");
        return ex;
    }

    /**
     * Factory method for query failures.
     *
     * @param tableName The DynamoDB table name
     * @param userId    The partition key value
     * @param cause     The underlying cause
     * @return A DynamoDBOperationException configured for query failure
     */
    public static DynamoDBOperationException queryFailed(String tableName, String userId, Throwable cause) {
        DynamoDBOperationException ex = new DynamoDBOperationException(
                "Failed to query DynamoDB table: " + tableName, true, cause);
        ex.addContext("tableName", tableName);
        ex.addContext("userId", userId);
        ex.addContext("operation", "query");
        return ex;
    }

    /**
     * Factory method for throughput exceeded errors.
     *
     * @param tableName The DynamoDB table name
     * @param operation The operation that exceeded throughput
     * @param cause     The underlying cause
     * @return A DynamoDBOperationException configured for throughput exceeded
     */
    public static DynamoDBOperationException throughputExceeded(String tableName, String operation, Throwable cause) {
        DynamoDBOperationException ex = new DynamoDBOperationException(
                "DynamoDB throughput exceeded for table: " + tableName, true, cause);
        ex.addContext("tableName", tableName);
        ex.addContext("operation", operation);
        ex.addContext("errorType", "THROUGHPUT_EXCEEDED");
        return ex;
    }

    /**
     * Factory method for conditional check failures (non-conflict scenarios).
     *
     * @param tableName The DynamoDB table name
     * @param userId    The partition key value
     * @param sortKey   The sort key value
     * @param cause     The underlying cause
     * @return A DynamoDBOperationException configured for conditional check failure
     */
    public static DynamoDBOperationException conditionalCheckFailed(String tableName, String userId,
                                                                     String sortKey, Throwable cause) {
        DynamoDBOperationException ex = new DynamoDBOperationException(
                "Conditional check failed for DynamoDB operation on table: " + tableName,
                "CONDITIONAL_CHECK_FAILED", null, cause);
        ex.addContext("tableName", tableName);
        ex.addContext("userId", userId);
        ex.addContext("sortKey", sortKey);
        ex.addContext("operation", "conditionalWrite");
        return ex;
    }

    /**
     * Factory method for album deletion with images error.
     *
     * @param userId     The user ID
     * @param albumId    The album ID
     * @param imageCount The number of images in the album
     * @return A DynamoDBOperationException configured for non-empty album deletion
     */
    public static DynamoDBOperationException albumNotEmpty(String userId, String albumId, int imageCount) {
        DynamoDBOperationException ex = new DynamoDBOperationException(
                "Cannot delete album with " + imageCount + " images. Delete all images first.");
        ex.addContext("userId", userId);
        ex.addContext("albumId", albumId);
        ex.addContext("imageCount", imageCount);
        ex.addContext("operation", "deleteAlbum");
        return ex;
    }

    @Override
    public String toString() {
        return super.toString().replace("}", ", retryable=" + retryable + "}");
    }
}
