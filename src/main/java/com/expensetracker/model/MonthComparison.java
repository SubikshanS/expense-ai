package com.expensetracker.model;

import com.expensetracker.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates month-over-month financial comparison metrics and category growth drivers.
 */
public class MonthComparison {

    private final YearMonth currentMonth;
    private final YearMonth previousMonth;
    private final BigDecimal currentTotal;
    private final BigDecimal previousTotal;
    private final BigDecimal netDifference;
    private final Double percentageChange;
    private final BigDecimal currentDailyAverage;
    private final BigDecimal previousDailyAverage;
    private final List<CategoryTrend> categoryTrends;
    private final List<CategoryTrend> topIncreaseContributors;
    private final boolean sufficientHistory;
    private final String statusMessage;

    public MonthComparison(
            YearMonth currentMonth,
            YearMonth previousMonth,
            BigDecimal currentTotal,
            BigDecimal previousTotal,
            BigDecimal netDifference,
            Double percentageChange,
            BigDecimal currentDailyAverage,
            BigDecimal previousDailyAverage,
            List<CategoryTrend> categoryTrends,
            List<CategoryTrend> topIncreaseContributors,
            boolean sufficientHistory,
            String statusMessage) {

        this.currentMonth = currentMonth;
        this.previousMonth = previousMonth;
        this.currentTotal = currentTotal != null ? currentTotal : MoneyUtil.ZERO;
        this.previousTotal = previousTotal != null ? previousTotal : MoneyUtil.ZERO;
        this.netDifference = netDifference != null ? netDifference : MoneyUtil.ZERO;
        this.percentageChange = percentageChange;
        this.currentDailyAverage = currentDailyAverage != null ? currentDailyAverage : MoneyUtil.ZERO;
        this.previousDailyAverage = previousDailyAverage != null ? previousDailyAverage : MoneyUtil.ZERO;
        this.categoryTrends = categoryTrends != null ? Collections.unmodifiableList(categoryTrends) : Collections.emptyList();
        this.topIncreaseContributors = topIncreaseContributors != null ? Collections.unmodifiableList(topIncreaseContributors) : Collections.emptyList();
        this.sufficientHistory = sufficientHistory;
        this.statusMessage = statusMessage;
    }

    public static MonthComparison insufficient(String message) {
        return new MonthComparison(
                null, null, MoneyUtil.ZERO, MoneyUtil.ZERO, MoneyUtil.ZERO,
                null, MoneyUtil.ZERO, MoneyUtil.ZERO,
                Collections.emptyList(), Collections.emptyList(),
                false, message
        );
    }

    public static MonthComparison singleMonth(
            YearMonth currentMonth,
            BigDecimal currentTotal,
            BigDecimal currentDailyAverage,
            List<CategoryTrend> categoryTrends,
            String message) {
        return new MonthComparison(
                currentMonth,
                null,
                currentTotal,
                MoneyUtil.ZERO,
                MoneyUtil.ZERO,
                null,
                currentDailyAverage,
                MoneyUtil.ZERO,
                categoryTrends,
                Collections.emptyList(),
                false,
                message
        );
    }

    public YearMonth getCurrentMonth() {
        return currentMonth;
    }

    public YearMonth getPreviousMonth() {
        return previousMonth;
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
        if (percentageChange == null) {
            return "0.0%";
        }
        String sign = percentageChange >= 0 ? "+" : "";
        return String.format("%s%.1f%%", sign, percentageChange);
    }

    public BigDecimal getCurrentDailyAverage() {
        return currentDailyAverage;
    }

    public String getFormattedCurrentDailyAverage() {
        return MoneyUtil.formatRupees(currentDailyAverage);
    }

    public BigDecimal getPreviousDailyAverage() {
        return previousDailyAverage;
    }

    public String getFormattedPreviousDailyAverage() {
        return MoneyUtil.formatRupees(previousDailyAverage);
    }

    public List<CategoryTrend> getCategoryTrends() {
        return categoryTrends;
    }

    public List<CategoryTrend> getTopIncreaseContributors() {
        return topIncreaseContributors;
    }

    public boolean isSufficientHistory() {
        return sufficientHistory;
    }

    public String getStatusMessage() {
        return statusMessage;
    }
}
