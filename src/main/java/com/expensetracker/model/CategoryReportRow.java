package com.expensetracker.model;

import com.expensetracker.util.MoneyUtil;

import java.math.BigDecimal;

/**
 * Encapsulates aggregated metrics for a single category within a generated financial report.
 * Provides properties compatible with JavaFX TableView PropertyValueFactory bindings.
 */
public class CategoryReportRow {

    private final String category;
    private final int transactionCount;
    private final BigDecimal totalAmount;
    private final double percentage;

    public CategoryReportRow(String category, int transactionCount, BigDecimal totalAmount, double percentage) {
        this.category = category != null ? category : "Uncategorized";
        this.transactionCount = transactionCount;
        this.totalAmount = totalAmount != null ? totalAmount : MoneyUtil.ZERO;
        this.percentage = percentage;
    }

    public String getCategory() {
        return category;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public String getFormattedTransactionCount() {
        return transactionCount + (transactionCount == 1 ? " entry" : " entries");
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public double getTotalAmountDouble() {
        return totalAmount.doubleValue();
    }

    public String getFormattedTotalAmount() {
        return MoneyUtil.formatRupees(totalAmount);
    }

    public double getPercentage() {
        return percentage;
    }

    public String getFormattedPercentage() {
        return String.format("%.1f%%", percentage);
    }
}
