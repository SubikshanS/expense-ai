package com.expensetracker.model;

import java.time.LocalDate;

/**
 * Filter criteria for report generation, supporting predefined periods,
 * custom date bounds, and category scoping.
 */
public class ReportFilter {

    public enum PeriodPreset {
        THIS_MONTH("📅 This Month"),
        LAST_MONTH("⏮️ Last Month"),
        LAST_30_DAYS("⚡ Last 30 Days"),
        LAST_90_DAYS("📊 Last 90 Days"),
        THIS_YEAR("📆 This Year (YTD)"),
        ALL_TIME("🌐 All Records"),
        CUSTOM("🎯 Custom Date Range");

        private final String displayName;

        PeriodPreset(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final PeriodPreset preset;
    private final LocalDate customStartDate;
    private final LocalDate customEndDate;
    private final String categoryFilter;

    public ReportFilter(PeriodPreset preset, LocalDate customStartDate, LocalDate customEndDate, String categoryFilter) {
        this.preset = preset != null ? preset : PeriodPreset.THIS_MONTH;
        this.customStartDate = customStartDate;
        this.customEndDate = customEndDate;
        this.categoryFilter = categoryFilter != null ? categoryFilter : "All Categories";
    }

    public ReportFilter(PeriodPreset preset, String categoryFilter, LocalDate customStartDate, LocalDate customEndDate) {
        this(preset, customStartDate, customEndDate, categoryFilter);
    }

    public static ReportFilter ofPreset(PeriodPreset preset) {
        return new ReportFilter(preset, null, null, "All Categories");
    }

    public static ReportFilter ofPresetAndCategory(PeriodPreset preset, String category) {
        return new ReportFilter(preset, null, null, category);
    }

    public static ReportFilter custom(LocalDate start, LocalDate end, String category) {
        return new ReportFilter(PeriodPreset.CUSTOM, start, end, category);
    }

    public PeriodPreset getPreset() {
        return preset;
    }

    public LocalDate getCustomStartDate() {
        return customStartDate;
    }

    public LocalDate getCustomEndDate() {
        return customEndDate;
    }

    public String getCategoryFilter() {
        return categoryFilter;
    }

    public boolean isAllCategories() {
        return categoryFilter == null || categoryFilter.isBlank() || "All Categories".equalsIgnoreCase(categoryFilter.trim());
    }
}
