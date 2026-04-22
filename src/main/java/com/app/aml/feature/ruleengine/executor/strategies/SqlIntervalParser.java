package com.app.aml.feature.ruleengine.executor.strategies;

public class SqlIntervalParser {
    public static String parse(String dbLookback) {
        if (dbLookback == null) return "24 hours";
        String lower = dbLookback.toLowerCase();
        if (lower.contains("h")) return lower.replace("h", " hours");
        if (lower.contains("d")) return lower.replace("d", " days");
        return "24 hours";
    }
}