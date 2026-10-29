package com.expensetracker.model;

/**
 * Predefined time ranges for spending analytics aggregation.
 */
public enum TimeRange {
    LAST_7_DAYS("Last 7 Days", "Daily Spending", true),
    LAST_30_DAYS("Last 30 Days", "Daily Spending", true),
    LAST_6_MONTHS("Last 6 Months", "Monthly Spending", false),
    LAST_12_MONTHS("Last 12 Months", "Monthly Spending", false),
    ALL_TIME("All Time", "Monthly Spending", false);

    private final String displayName;
    private final String aggregationLabel;
    private final boolean isDaily;

    TimeRange(String displayName, String aggregationLabel, boolean isDaily) {
        this.displayName = displayName;
        this.aggregationLabel = aggregationLabel;
        this.isDaily = isDaily;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAggregationLabel() {
        return aggregationLabel;
    }

    public boolean isDaily() {
        return isDaily;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
