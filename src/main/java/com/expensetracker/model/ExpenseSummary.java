package com.expensetracker.model;

import com.expensetracker.util.MoneyUtil;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/**
 * Value object encapsulating calculated financial summary metrics.
 */
public class ExpenseSummary {

    private final BigDecimal totalSpending;
    private final int transactionCount;
    private final BigDecimal averageExpense;
    private final String topCategory;
    private final BigDecimal topCategoryAmount;
    private final Map<String, BigDecimal> categoryBreakdown;

    public ExpenseSummary(
            BigDecimal totalSpending,
            int transactionCount,
            BigDecimal averageExpense,
            String topCategory,
            BigDecimal topCategoryAmount,
            Map<String, BigDecimal> categoryBreakdown) {

        this.totalSpending = totalSpending != null ? totalSpending : MoneyUtil.ZERO;
        this.transactionCount = Math.max(0, transactionCount);
        this.averageExpense = averageExpense != null ? averageExpense : MoneyUtil.ZERO;
        this.topCategory = topCategory != null ? topCategory : "None";
        this.topCategoryAmount = topCategoryAmount != null ? topCategoryAmount : MoneyUtil.ZERO;
        this.categoryBreakdown = categoryBreakdown != null ? Collections.unmodifiableMap(categoryBreakdown) : Collections.emptyMap();
    }

    public static ExpenseSummary empty() {
        return new ExpenseSummary(
                MoneyUtil.ZERO,
                0,
                MoneyUtil.ZERO,
                "None",
                MoneyUtil.ZERO,
                Collections.emptyMap()
        );
    }

    public BigDecimal getTotalSpending() {
        return totalSpending;
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

    public String getFormattedTopCategoryAmount() {
        return MoneyUtil.formatRupees(topCategoryAmount);
    }

    public Map<String, BigDecimal> getCategoryBreakdown() {
        return categoryBreakdown;
    }
}
