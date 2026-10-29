package com.expensetracker.model;

import com.expensetracker.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Aggregates complete personal financial intelligence metrics, historical time aggregations,
 * month-over-month comparisons, and detected spending anomalies.
 */
public class FinancialIntelligenceResult {

    private final MonthComparison monthComparison;
    private final List<SpendingAnomaly> anomalies;
    private final Map<LocalDate, BigDecimal> dailySpending;
    private final Map<String, BigDecimal> weeklySpending;
    private final Map<YearMonth, BigDecimal> monthlySpending;
    private final BigDecimal averageDailySpendLast30Days;
    private final int totalActiveSpendingDays;
    private final boolean hasData;
    private final String summaryInsight;

    public FinancialIntelligenceResult(
            MonthComparison monthComparison,
            List<SpendingAnomaly> anomalies,
            Map<LocalDate, BigDecimal> dailySpending,
            Map<String, BigDecimal> weeklySpending,
            Map<YearMonth, BigDecimal> monthlySpending,
            BigDecimal averageDailySpendLast30Days,
            int totalActiveSpendingDays,
            boolean hasData,
            String summaryInsight) {

        this.monthComparison = monthComparison != null ? monthComparison : MonthComparison.insufficient("No comparison data");
        this.anomalies = anomalies != null ? Collections.unmodifiableList(anomalies) : Collections.emptyList();
        this.dailySpending = dailySpending != null ? Collections.unmodifiableMap(dailySpending) : Collections.emptyMap();
        this.weeklySpending = weeklySpending != null ? Collections.unmodifiableMap(weeklySpending) : Collections.emptyMap();
        this.monthlySpending = monthlySpending != null ? Collections.unmodifiableMap(monthlySpending) : Collections.emptyMap();
        this.averageDailySpendLast30Days = averageDailySpendLast30Days != null ? averageDailySpendLast30Days : MoneyUtil.ZERO;
        this.totalActiveSpendingDays = Math.max(0, totalActiveSpendingDays);
        this.hasData = hasData;
        this.summaryInsight = summaryInsight;
    }

    public static FinancialIntelligenceResult empty() {
        return new FinancialIntelligenceResult(
                MonthComparison.insufficient("No transaction history available. Record expenses to generate financial intelligence."),
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                MoneyUtil.ZERO,
                0,
                false,
                "Insufficient transaction data to analyze spending patterns."
        );
    }

    public MonthComparison getMonthComparison() {
        return monthComparison;
    }

    public List<SpendingAnomaly> getAnomalies() {
        return anomalies;
    }

    public Map<LocalDate, BigDecimal> getDailySpending() {
        return dailySpending;
    }

    public Map<String, BigDecimal> getWeeklySpending() {
        return weeklySpending;
    }

    public Map<YearMonth, BigDecimal> getMonthlySpending() {
        return monthlySpending;
    }

    public BigDecimal getAverageDailySpendLast30Days() {
        return averageDailySpendLast30Days;
    }

    public String getFormattedAverageDailySpendLast30Days() {
        return MoneyUtil.formatRupees(averageDailySpendLast30Days);
    }

    public int getTotalActiveSpendingDays() {
        return totalActiveSpendingDays;
    }

    public boolean isHasData() {
        return hasData;
    }

    public String getSummaryInsight() {
        return summaryInsight;
    }
}
