package com.expensetracker.model;

import com.expensetracker.util.MoneyUtil;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates spending trend and comparison metrics for a specific category over time.
 */
public class CategoryTrend {

    private final String category;
    private final BigDecimal currentAmount;
    private final BigDecimal previousAmount;
    private final BigDecimal difference;
    private final Double percentageChange;
    private final Double contributionToIncrease;
    private final List<Expense> currentTransactions;
    private final List<Expense> previousTransactions;

    public CategoryTrend(
            String category,
            BigDecimal currentAmount,
            BigDecimal previousAmount,
            BigDecimal difference,
            Double percentageChange,
            Double contributionToIncrease,
            List<Expense> currentTransactions,
            List<Expense> previousTransactions) {

        this.category = category != null ? category : "Uncategorized";
        this.currentAmount = currentAmount != null ? currentAmount : MoneyUtil.ZERO;
        this.previousAmount = previousAmount != null ? previousAmount : MoneyUtil.ZERO;
        this.difference = difference != null ? difference : MoneyUtil.ZERO;
        this.percentageChange = percentageChange;
        this.contributionToIncrease = contributionToIncrease;
        this.currentTransactions = currentTransactions != null ? Collections.unmodifiableList(currentTransactions) : Collections.emptyList();
        this.previousTransactions = previousTransactions != null ? Collections.unmodifiableList(previousTransactions) : Collections.emptyList();
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public String getFormattedCurrentAmount() {
        return MoneyUtil.formatRupees(currentAmount);
    }

    public BigDecimal getPreviousAmount() {
        return previousAmount;
    }

    public String getFormattedPreviousAmount() {
        return MoneyUtil.formatRupees(previousAmount);
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public String getFormattedDifference() {
        String sign = difference.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + MoneyUtil.formatRupees(difference);
    }

    public Double getPercentageChange() {
        return percentageChange;
    }

    public String getFormattedPercentageChange() {
        if (percentageChange == null) {
            return previousAmount.compareTo(BigDecimal.ZERO) == 0 && currentAmount.compareTo(BigDecimal.ZERO) > 0
                    ? "+100% (New)"
                    : "0.0%";
        }
        String sign = percentageChange >= 0 ? "+" : "";
        return String.format("%s%.1f%%", sign, percentageChange);
    }

    public Double getContributionToIncrease() {
        return contributionToIncrease;
    }

    public String getFormattedContribution() {
        if (contributionToIncrease == null || contributionToIncrease <= 0) {
            return "—";
        }
        return String.format("%.1f%%", contributionToIncrease);
    }

    public List<Expense> getCurrentTransactions() {
        return currentTransactions;
    }

    public List<Expense> getPreviousTransactions() {
        return previousTransactions;
    }
}
