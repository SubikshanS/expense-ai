package com.expensetracker.model;

import com.expensetracker.util.MoneyUtil;

import java.math.BigDecimal;

/**
 * Encapsulates aggregated spending data for a single expense category.
 */
public class CategorySpendingPoint {

    private final String category;
    private final BigDecimal amount;
    private final double percentage;
    private final int transactionCount;

    public CategorySpendingPoint(String category, BigDecimal amount, double percentage, int transactionCount) {
        this.category = category != null ? category : "Uncategorized";
        this.amount = amount != null ? amount : MoneyUtil.ZERO;
        this.percentage = Math.max(0.0, percentage);
        this.transactionCount = Math.max(0, transactionCount);
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public double getAmountDouble() {
        return amount.doubleValue();
    }

    public String getFormattedAmount() {
        return MoneyUtil.formatRupees(amount);
    }

    public double getPercentage() {
        return percentage;
    }

    public String getFormattedPercentage() {
        return String.format("%.1f%%", percentage);
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public String getTooltipText() {
        return String.format("🏷️ %s\n💰 Total: %s\n📊 Share: %.1f%% of total\n📝 %d %s",
                category,
                getFormattedAmount(),
                percentage,
                transactionCount,
                transactionCount == 1 ? "transaction" : "transactions");
    }
}
