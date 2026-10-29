package com.expensetracker.model;

import com.expensetracker.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates complete Month-over-Month comparison analysis, including Day-Normalized MTD metrics.
 */
public class MoMComparisonResult {

    private final YearMonth currentMonth;
    private final YearMonth previousMonth;
    private final int currentDaysEvaluated;
    private final int previousDaysEvaluated;
    private final boolean isDayNormalized;
    private final BigDecimal currentTotal;
    private final BigDecimal previousTotal;
    private final BigDecimal netDifference;
    private final Double percentageChange;
    private final List<CategoryMoMPoint> categoryPoints;
    private final boolean sufficientHistory;
    private final String statusMessage;

    public MoMComparisonResult(
            YearMonth currentMonth,
            YearMonth previousMonth,
            int currentDaysEvaluated,
            int previousDaysEvaluated,
            boolean isDayNormalized,
            BigDecimal currentTotal,
            BigDecimal previousTotal,
            BigDecimal netDifference,
            Double percentageChange,
            List<CategoryMoMPoint> categoryPoints,
            boolean sufficientHistory,
            String statusMessage) {
        this.currentMonth = currentMonth;
        this.previousMonth = previousMonth;
        this.currentDaysEvaluated = currentDaysEvaluated;
        this.previousDaysEvaluated = previousDaysEvaluated;
        this.isDayNormalized = isDayNormalized;
        this.currentTotal = currentTotal != null ? currentTotal : MoneyUtil.ZERO;
        this.previousTotal = previousTotal != null ? previousTotal : MoneyUtil.ZERO;
        this.netDifference = netDifference != null ? netDifference : MoneyUtil.ZERO;
        this.percentageChange = percentageChange;
        this.categoryPoints = categoryPoints != null ? Collections.unmodifiableList(categoryPoints) : Collections.emptyList();
        this.sufficientHistory = sufficientHistory;
        this.statusMessage = statusMessage != null ? statusMessage : "";
    }

    public static MoMComparisonResult insufficient(String message) {
        return new MoMComparisonResult(
                null, null, 0, 0, false,
                MoneyUtil.ZERO, MoneyUtil.ZERO, MoneyUtil.ZERO, null,
                Collections.emptyList(), false, message
        );
    }

    public YearMonth getCurrentMonth() {
        return currentMonth;
    }

    public YearMonth getPreviousMonth() {
        return previousMonth;
    }

    public int getCurrentDaysEvaluated() {
        return currentDaysEvaluated;
    }

    public int getPreviousDaysEvaluated() {
        return previousDaysEvaluated;
    }

    public boolean isDayNormalized() {
        return isDayNormalized;
    }

    public BigDecimal getCurrentTotal() {
        return currentTotal;
    }

    public String getFormattedCurrentTotal() {
        return MoneyUtil.formatRupees(currentTotal);
    }

    public BigDecimal getPreviousTotal() {
        return previousTotal;
    }

    public String getFormattedPreviousTotal() {
        return MoneyUtil.formatRupees(previousTotal);
    }

    public BigDecimal getNetDifference() {
        return netDifference;
    }

    public String getFormattedNetDifference() {
        String sign = netDifference.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + MoneyUtil.formatRupees(netDifference);
    }

    public Double getPercentageChange() {
        return percentageChange;
    }

    public String getFormattedPercentageChange() {
        if (percentageChange == null) return "0.0%";
        String sign = percentageChange >= 0 ? "+" : "";
        return String.format("%s%.1f%%", sign, percentageChange);
    }

    public List<CategoryMoMPoint> getCategoryPoints() {
        return categoryPoints;
    }

    public boolean isSufficientHistory() {
        return sufficientHistory;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getCurrentSeriesName() {
        if (currentMonth == null) return "Current Month";
        String monthStr = currentMonth.format(DateTimeFormatter.ofPattern("MMM yyyy"));
        if (isDayNormalized && currentDaysEvaluated < currentMonth.lengthOfMonth()) {
            return String.format("%s (Day 1–%d)", monthStr, currentDaysEvaluated);
        }
        return monthStr;
    }

    public String getPreviousSeriesName() {
        if (previousMonth == null) return "Previous Month";
        String monthStr = previousMonth.format(DateTimeFormatter.ofPattern("MMM yyyy"));
        if (isDayNormalized && previousDaysEvaluated < previousMonth.lengthOfMonth()) {
            return String.format("%s (Day 1–%d)", monthStr, previousDaysEvaluated);
        }
        return monthStr;
    }

    public String getComparisonSummary() {
        if (!sufficientHistory) {
            return statusMessage;
        }
        String fairNotice = isDayNormalized && currentDaysEvaluated < currentMonth.lengthOfMonth()
                ? String.format(" [Fair MTD Comparison: Day 1–%d in both months]", currentDaysEvaluated)
                : "";
        return String.format("%s vs %s: %s (%s)%s",
                getCurrentSeriesName(),
                getPreviousSeriesName(),
                getFormattedNetDifference(),
                getFormattedPercentageChange(),
                fairNotice);
    }
}
