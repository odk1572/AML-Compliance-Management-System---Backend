package com.app.aml.feature.ingestion.batch.util;

import com.app.aml.feature.ingestion.batch.ValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class BatchValidationUtils {

    private BatchValidationUtils() {}

    public static String require(String value, int line, String field, Integer maxLength) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(line, field, "Cannot be null or blank");
        }
        String trimmed = value.trim();
        if (maxLength != null && trimmed.length() > maxLength) {
            throw new ValidationException(line, field, "Exceeds maximum length of " + maxLength);
        }
        return trimmed;
    }

    public static String require(String value, int line, String field) {
        // Calls the strict version but passes null for maxLength
        return require(value, line, field, null);
    }


    public static String safe(String value, Integer maxLength) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (maxLength != null && trimmed.length() > maxLength) {
            trimmed = trimmed.substring(0, maxLength);
        }
        return trimmed;
    }

    public static String safe(String value) {
        return safe(value, null);
    }

    public static BigDecimal parseBigDecimal(String value, int line, String field) {
        if (value == null || value.isBlank()) throw new ValidationException(line, field, "Cannot be null");
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(line, field, "Invalid decimal format");
        }
    }

    public static LocalDate parseLocalDate(String value, int line, String field) {
        if (value == null || value.isBlank()) throw new ValidationException(line, field, "Cannot be null");
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException(line, field, "Invalid date format. Must be yyyy-MM-dd");
        }
    }

    public static Instant parseInstant(String value, int line, String field) {
        if (value == null || value.isBlank()) throw new ValidationException(line, field, "Cannot be null");
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException(line, field, "Invalid timestamp format. Must be ISO-8601");
        }
    }

    public static Boolean parseBooleanStrict(String value, int line, String field) {
        if (value == null) return false;
        String v = value.trim().toLowerCase();
        if (v.equals("true") || v.equals("1") || v.equals("yes")) return true;
        if (v.equals("false") || v.equals("0") || v.equals("no")) return false;
        throw new ValidationException(line, field, "Invalid boolean value");
    }

    public static <T extends Enum<T>> T parseEnum(
            Class<T> enumType,
            String value,
            int line,
            String field,
            T defaultValue
    ) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}