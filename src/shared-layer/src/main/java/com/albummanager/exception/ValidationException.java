package com.albummanager.exception;

import java.util.Map;

/**
 * Exception thrown when request validation fails.
 * HTTP Status: 400 Bad Request
 * 
 * Use cases:
 * - Missing required fields
 * - Invalid field format (e.g., invalid UUID)
 * - Field length violations
 * - Invalid enum values
 */
public class ValidationException extends AlbumManagerException {

    private static final int HTTP_STATUS = 400;
    private static final String DEFAULT_ERROR_CODE = "VALIDATION_ERROR";

    /**
     * Constructs a ValidationException with a message.
     *
     * @param message Human-readable validation error message
     */
    public ValidationException(String message) {
        super(message, DEFAULT_ERROR_CODE, HTTP_STATUS);
    }

    /**
     * Constructs a ValidationException with a specific error code.
     *
     * @param message   Human-readable validation error message
     * @param errorCode Specific error code (e.g., "INVALID_ALBUM_NAME")
     */
    public ValidationException(String message, String errorCode) {
        super(message, errorCode, HTTP_STATUS);
    }

    /**
     * Constructs a ValidationException with context data.
     *
     * @param message   Human-readable validation error message
     * @param errorCode Specific error code
     * @param context   Additional context (field name, provided value, etc.)
     */
    public ValidationException(String message, String errorCode, Map<String, Object> context) {
        super(message, errorCode, HTTP_STATUS, context);
    }

    /**
     * Constructs a ValidationException with a cause.
     *
     * @param message Human-readable validation error message
     * @param cause   The underlying cause
     */
    public ValidationException(String message, Throwable cause) {
        super(message, DEFAULT_ERROR_CODE, HTTP_STATUS, cause);
    }

    /**
     * Constructs a ValidationException with error code and cause.
     *
     * @param message   Human-readable validation error message
     * @param errorCode Specific error code
     * @param cause     The underlying cause
     */
    public ValidationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, HTTP_STATUS, cause);
    }

    /**
     * Factory method for missing required field validation errors.
     *
     * @param fieldName The name of the missing required field
     * @return A ValidationException configured for missing field
     */
    public static ValidationException missingField(String fieldName) {
        return (ValidationException) new ValidationException(
                "Missing required field: " + fieldName,
                "MISSING_REQUIRED_FIELD"
        ).addContext("field", fieldName);
    }

    /**
     * Factory method for invalid field format errors.
     *
     * @param fieldName     The name of the field with invalid format
     * @param expectedFormat Description of the expected format
     * @return A ValidationException configured for invalid format
     */
    public static ValidationException invalidFormat(String fieldName, String expectedFormat) {
        return (ValidationException) new ValidationException(
                "Invalid format for field '" + fieldName + "'. Expected: " + expectedFormat,
                "INVALID_FORMAT"
        ).addContext("field", fieldName)
         .addContext("expectedFormat", expectedFormat);
    }

    /**
     * Factory method for field length violation errors.
     *
     * @param fieldName The name of the field
     * @param minLength Minimum allowed length
     * @param maxLength Maximum allowed length
     * @param actual    Actual length provided
     * @return A ValidationException configured for length violation
     */
    public static ValidationException invalidLength(String fieldName, int minLength, int maxLength, int actual) {
        return (ValidationException) new ValidationException(
                "Field '" + fieldName + "' length must be between " + minLength + " and " + maxLength + 
                " characters. Actual: " + actual,
                "INVALID_LENGTH"
        ).addContext("field", fieldName)
         .addContext("minLength", minLength)
         .addContext("maxLength", maxLength)
         .addContext("actualLength", actual);
    }

    /**
     * Factory method for invalid enum value errors.
     *
     * @param fieldName    The name of the field
     * @param providedValue The invalid value provided
     * @param allowedValues Comma-separated list of allowed values
     * @return A ValidationException configured for invalid enum
     */
    public static ValidationException invalidEnumValue(String fieldName, String providedValue, String allowedValues) {
        return (ValidationException) new ValidationException(
                "Invalid value '" + providedValue + "' for field '" + fieldName + 
                "'. Allowed values: " + allowedValues,
                "INVALID_ENUM_VALUE"
        ).addContext("field", fieldName)
         .addContext("providedValue", providedValue)
         .addContext("allowedValues", allowedValues);
    }
}
