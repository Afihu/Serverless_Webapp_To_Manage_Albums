package com.albummanager.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Base exception for all Album Manager application exceptions.
 * Provides structured error information including machine-readable error codes,
 * HTTP status codes, and contextual data for debugging.
 */
public class AlbumManagerException extends Exception {

    private final String errorCode;
    private final int httpStatus;
    private final Map<String, Object> context;

    /**
     * Constructs a new AlbumManagerException with the specified details.
     *
     * @param message    Human-readable error message
     * @param errorCode  Machine-readable error code (e.g., "ALBUM_NOT_FOUND")
     * @param httpStatus HTTP status code (e.g., 400, 404, 500)
     */
    public AlbumManagerException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode cannot be null");
        this.httpStatus = httpStatus;
        this.context = new HashMap<>();
    }

    /**
     * Constructs a new AlbumManagerException with context data.
     *
     * @param message    Human-readable error message
     * @param errorCode  Machine-readable error code
     * @param httpStatus HTTP status code
     * @param context    Additional context data (userId, albumId, etc.)
     */
    public AlbumManagerException(String message, String errorCode, int httpStatus, Map<String, Object> context) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode cannot be null");
        this.httpStatus = httpStatus;
        this.context = context != null ? new HashMap<>(context) : new HashMap<>();
    }

    /**
     * Constructs a new AlbumManagerException with a cause.
     *
     * @param message    Human-readable error message
     * @param errorCode  Machine-readable error code
     * @param httpStatus HTTP status code
     * @param cause      The underlying cause of this exception
     */
    public AlbumManagerException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode cannot be null");
        this.httpStatus = httpStatus;
        this.context = new HashMap<>();
    }

    /**
     * Constructs a new AlbumManagerException with context and cause.
     *
     * @param message    Human-readable error message
     * @param errorCode  Machine-readable error code
     * @param httpStatus HTTP status code
     * @param context    Additional context data
     * @param cause      The underlying cause of this exception
     */
    public AlbumManagerException(String message, String errorCode, int httpStatus, 
                                  Map<String, Object> context, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode cannot be null");
        this.httpStatus = httpStatus;
        this.context = context != null ? new HashMap<>(context) : new HashMap<>();
    }

    /**
     * Returns the machine-readable error code.
     *
     * @return The error code (e.g., "ALBUM_NOT_FOUND", "VALIDATION_ERROR")
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the HTTP status code associated with this exception.
     *
     * @return HTTP status code (e.g., 400, 404, 500)
     */
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * Returns an unmodifiable view of the context data.
     *
     * @return Immutable map of context data
     */
    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(context);
    }

    /**
     * Adds a context entry to this exception.
     *
     * @param key   The context key
     * @param value The context value
     * @return This exception instance for method chaining
     */
    public AlbumManagerException addContext(String key, Object value) {
        if (key != null && value != null) {
            this.context.put(key, value);
        }
        return this;
    }

    /**
     * Returns a detailed string representation including all context for logging.
     *
     * @return Formatted string with error details
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName())
          .append("{")
          .append("errorCode='").append(errorCode).append("'")
          .append(", httpStatus=").append(httpStatus)
          .append(", message='").append(getMessage()).append("'");
        
        if (!context.isEmpty()) {
            sb.append(", context={");
            boolean first = true;
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
            sb.append("}");
        }
        
        if (getCause() != null) {
            sb.append(", cause=").append(getCause().getClass().getSimpleName())
              .append(": ").append(getCause().getMessage());
        }
        
        sb.append("}");
        return sb.toString();
    }
}
