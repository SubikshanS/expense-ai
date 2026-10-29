package com.expensetracker.model;

import com.expensetracker.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * Complete immutable financial report encapsulating executive summary metrics,
 * category distribution rollups, and the filtered transaction ledger.
 */
public class FinancialReport {

    private final String reportTitle;
    private final String dateRangeDescription;
    private final LocalDateTime generatedAt;
    private final String filterCategory;
    private final BigDecimal totalSpending;
    private final int transactionCount;
    private final BigDecimal averageExpense;
    private final String topCategory;
    private final BigDecimal topCategoryAmount;
    private final String peakSpendingDay;
    private final BigDecimal peakSpendingAmount;
    private final List<Expense> expenses;
    private final List<CategoryReportRow> categoryRows;

    public FinancialReport(
            String reportTitle,
            String dateRangeDescription,
            LocalDateTime generatedAt,
            BigDecimal totalSpending,
            int transactionCount,
            BigDecimal averageExpense,
            String topCategory,
            BigDecimal topCategoryAmount,
            String peakSpendingDay,
            BigDecimal peakSpendingAmount,
            List<Expense> expenses,
            List<CategoryReportRow> categoryRows) {

        this(reportTitle, dateRangeDescription, "All Categories", generatedAt, totalSpending, transactionCount, averageExpense, topCategory, topCategoryAmount, peakSpendingDay, peakSpendingAmount, expenses, categoryRows);
    }

    public FinancialReport(
            String reportTitle,
            String dateRangeDescription,
            String filterCategory,
            LocalDateTime generatedAt,
            BigDecimal totalSpending,
            int transactionCount,
            BigDecimal averageExpense,
            String topCategory,
            BigDecimal topCategoryAmount,
            String peakSpendingDay,
            BigDecimal peakSpendingAmount,
            List<Expense> expenses,
            List<CategoryReportRow> categoryRows) {

        this.reportTitle = reportTitle != null ? reportTitle : "Financial Report";
        this.dateRangeDescription = dateRangeDescription != null ? dateRangeDescription : "All Records";
        this.filterCategory = filterCategory != null ? filterCategory : "All Categories";
        this.generatedAt = generatedAt != null ? generatedAt : LocalDateTime.now();
        this.totalSpending = totalSpending != null ? totalSpending : MoneyUtil.ZERO;
        this.transactionCount = transactionCount;
        this.averageExpense = averageExpense != null ? averageExpense : MoneyUtil.ZERO;
        this.topCategory = topCategory != null ? topCategory : "None";
        this.topCategoryAmount = topCategoryAmount != null ? topCategoryAmount : MoneyUtil.ZERO;
        this.peakSpendingDay = peakSpendingDay != null ? peakSpendingDay : "None";
        this.peakSpendingAmount = peakSpendingAmount != null ? peakSpendingAmount : MoneyUtil.ZERO;
        this.expenses = expenses != null ? Collections.unmodifiableList(expenses) : Collections.emptyList();
        this.categoryRows = categoryRows != null ? Collections.unmodifiableList(categoryRows) : Collections.emptyList();
    }

    public static FinancialReport empty(String reportTitle, String dateRangeDesc) {
        return new FinancialReport(
                reportTitle,
                dateRangeDesc,
                LocalDateTime.now(),
                MoneyUtil.ZERO,
                0,
                MoneyUtil.ZERO,
                "None",
                MoneyUtil.ZERO,
                "None",
                MoneyUtil.ZERO,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public String getDateRangeDescription() {
        return dateRangeDescription;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public String getFormattedGeneratedAt() {
        return generatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public BigDecimal getTotalSpending() {
        return totalSpending;
    }

    public double getTotalSpendingDouble() {
        return totalSpending.doubleValue();
    }

    public String getFormattedTotalSpending() {
        return MoneyUtil.formatRupees(totalSpending);
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public String getFormattedTransactionCount() {
        return transactionCount + (transactionCount == 1 ? " Record" : " Records");
    }

    public BigDecimal getAverageExpense() {
        return averageExpense;
    }

    public String getFormattedAverageExpense() {
        return MoneyUtil.formatRupees(averageExpense);
    }

    public String getTopCategory() {
        return topCategory;
    }

    public BigDecimal getTopCategoryAmount() {
        return topCategoryAmount;
    }

    public String getFormattedTopCategorySummary() {
        if ("None".equals(topCategory) || topCategoryAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return "None";
        }
        return topCategory + " (" + MoneyUtil.formatRupees(topCategoryAmount) + ")";
    }

    public String getPeakSpendingDay() {
        return peakSpendingDay;
    }

    public BigDecimal getPeakSpendingAmount() {
        return peakSpendingAmount;
    }

    public String getFormattedPeakSpendingSummary() {
        if ("None".equals(peakSpendingDay) || peakSpendingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return "None";
        }
        return peakSpendingDay + " (" + MoneyUtil.formatRupees(peakSpendingAmount) + ")";
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public List<CategoryReportRow> getCategoryRows() {
        return categoryRows;
    }

    public boolean hasRecords() {
        return !expenses.isEmpty();
    }

    public boolean isEmpty() {
        return expenses.isEmpty();
    }

    public String getTitle() {
        return reportTitle;
    }

    public String getPeriodDescription() {
        return dateRangeDescription;
    }

    public String getFilterCategory() {
        return filterCategory;
    }

    public String getFormattedTotalAmount() {
        return getFormattedTotalSpending();
    }

    public int getTotalTransactions() {
        return transactionCount;
    }

    public String getFormattedAverageTransaction() {
        return getFormattedAverageExpense();
    }

    public String getTopCategoryPercentage() {
        if (totalSpending.compareTo(BigDecimal.ZERO) <= 0 || topCategoryAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return "";
        }
        double pct = topCategoryAmount.multiply(new BigDecimal("100")).divide(totalSpending, 1, java.math.RoundingMode.HALF_UP).doubleValue();
        return String.format("%.1f%%", pct);
    }

    public String getFormattedPeakSpendingDay() {
        return getFormattedPeakSpendingSummary();
    }

    public List<Expense> getTransactions() {
        return expenses;
    }

    public List<CategoryReportRow> getCategoryBreakdown() {
        return categoryRows;
    }
}
