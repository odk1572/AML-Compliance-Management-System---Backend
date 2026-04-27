package com.app.aml.feature.ruleengine.executor.strategies;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SqlIntervalParser {

    private static final Pattern LOOKBACK_PATTERN = Pattern.compile("^(\\d+)\\s*([a-zA-Z]+)$");

    private SqlIntervalParser() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String parse(String dbLookback) {
        if (dbLookback == null || dbLookback.isBlank()) {
            throw new IllegalArgumentException("Lookback period cannot be null or blank");
        }

        Matcher matcher = LOOKBACK_PATTERN.matcher(dbLookback.trim().toLowerCase());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid lookback format: '" + dbLookback
                    + "'. Expected format: <number><unit> (e.g., 7d, 7 days, 24h).");
        }

        String value = matcher.group(1);
        String unitInput = matcher.group(2);
        String normalizedUnit = normalizeUnit(unitInput);

        return value + " " + normalizedUnit;
    }

    public static void validateCoverage(String primary, String ruleName, String... subIntervals) {
        double primaryDays = convertToDays(primary);
        double totalSubDays = 0;

        for (String sub : subIntervals) {
            if (sub != null) {
                totalSubDays += convertToDays(sub);
            }
        }

        if (primaryDays < totalSubDays) {
            throw new IllegalArgumentException(String.format(
                    "Rule [%s] Validation Failed: The Scenario LOOKBACK_WINDOW (%s) is too short. " +
                            "It must cover the sum of rule timeframes provided (%s days required vs ~%.1f days provided).",
                    ruleName, primary, String.format("%.1f", totalSubDays), primaryDays
            ));
        }
    }
    private static String normalizeUnit(String unit) {
        return switch (unit.toLowerCase()) {
            case "h", "hr", "hour", "hours" -> "hours";
            case "d", "day", "days" -> "days";
            case "w", "wk", "week", "weeks" -> "weeks";
            case "m", "mon", "month", "months" -> "months";
            case "y", "yr", "year", "years" -> "years";
            default -> throw new IllegalArgumentException("Unsupported time unit: " + unit);
        };
    }

    private static double convertToDays(String input) {
        if (input == null || input.isBlank()) return 0;

        Matcher m = LOOKBACK_PATTERN.matcher(input.trim().toLowerCase());
        if (!m.matches()) return 0;

        double value = Double.parseDouble(m.group(1));
        String unit = m.group(2);

        return switch (normalizeUnit(unit)) {
            case "hours" -> value / 24.0;
            case "days" -> value;
            case "weeks" -> value * 7.0;
            case "months" -> value * 30.0;
            case "years" -> value * 365.0;
            default -> 0;
        };
    }

    public static double getDays(String input) {
        return convertToDays(input);
    }
}