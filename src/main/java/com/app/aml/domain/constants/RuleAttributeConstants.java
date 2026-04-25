package com.app.aml.domain.constants;

import java.util.Set;

public class RuleAttributeConstants {

    // --- Common Temporal Attributes ---
    public static final String LOOKBACK_WINDOW = "LOOKBACK_WINDOW"; // e.g., "30 days"

    // Variable B: The specific timeframe the rule checks for the breach
    public static final String TIME_WINDOW = "TIME_WINDOW"; // e.g., "7 days"

    // --- Common Numeric Attributes ---
    public static final String TRANSACTION_COUNT = "TRANSACTION_COUNT";
    public static final String TRANSACTION_AMOUNT = "TRANSACTION_AMOUNT";
    public static final String MULTIPLIER = "MULTIPLIER"; // Used for Sudden Spike, Low Income

    // --- Typology Specific Attributes ---

    // Dormant Reactivation
    public static final String DORMANT_PERIOD = "DORMANT_PERIOD";
    public static final String REACTIVATION_WINDOW = "REACTIVATION_WINDOW";

    // Structuring
    public static final String SINGLE_TRANSACTION_LIMIT = "SINGLE_TRANSACTION_LIMIT";
    public static final String TOTAL_STRUCTURED_AMOUNT = "TOTAL_STRUCTURED_AMOUNT";
    public static final String SPLIT_TRANSACTION_COUNT = "SPLIT_TRANSACTION_COUNT";

    // Pass Through
    public static final String MARGIN_PERCENTAGE = "MARGIN_PERCENTAGE";

    // Round Amount
    public static final String ROUNDING_DIVISOR = "ROUNDING_DIVISOR";


    // --- Typology Attribute Validation Sets (Optional, for your Service layer) ---
    public static final Set<String> DORMANT_REACTIVATION_ATTRS = Set.of(DORMANT_PERIOD, REACTIVATION_WINDOW, TRANSACTION_AMOUNT);
    public static final Set<String> FUNNEL_ATTRS = Set.of(TRANSACTION_COUNT, TIME_WINDOW);
    public static final Set<String> LARGE_TRANSACTION_ATTRS = Set.of(TRANSACTION_AMOUNT, TIME_WINDOW);
    public static final Set<String> LOW_INCOME_HIGH_TRANSFER_ATTRS = Set.of(MULTIPLIER, TIME_WINDOW);
    public static final Set<String> PASS_THROUGH_ATTRS = Set.of(MARGIN_PERCENTAGE, TIME_WINDOW);
    public static final Set<String> ROUND_AMOUNT_ATTRS = Set.of(ROUNDING_DIVISOR, TRANSACTION_COUNT, TIME_WINDOW);
    public static final Set<String> SCATTER_ATTRS = Set.of(TRANSACTION_COUNT, TIME_WINDOW);
    public static final Set<String> STRUCTURING_ATTRS = Set.of(SINGLE_TRANSACTION_LIMIT, TOTAL_STRUCTURED_AMOUNT, SPLIT_TRANSACTION_COUNT, TIME_WINDOW);
    public static final Set<String> SUDDEN_SPIKE_ATTRS = Set.of(TIME_WINDOW, LOOKBACK_WINDOW, MULTIPLIER);
    public static final Set<String> VELOCITY_ATTRS = Set.of(TRANSACTION_COUNT, TIME_WINDOW);
}