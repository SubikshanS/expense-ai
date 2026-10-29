package com.expensetracker.model;

import com.expensetracker.util.MoneyUtil;

import java.math.BigDecimal;

/**
 * Encapsulates a category comparison point between the current period and previous period.
 */
public class CategoryMoMPoint {

    private final String category;
    private final BigDecimal currentAmount;
    private final BigDecimal previousAmount;
    private final BigDecimal difference;
    private final Double percentageChange;
    private final String currentPeriodLabel;
    private final String previousPeriodLabel;

    public CategoryMoMPoint(
            String category,
            BigDecimal currentAmount,
            BigDecimal previousAmount,
            BigDecimal difference,
            Double percentageChange,
            String currentPeriodLabel,
            String previousPeriodLabel) {
        this.category = category != null ? category : "Uncategorized";
        this.currentAmount = currentAmount != null ? currentAmount : MoneyUtil.ZERO;
        this.previousAmount = previousAmount != null ? previousAmount : MoneyUtil.ZERO;
        this.difference = difference != null ? difference : MoneyUtil.ZERO;
        this.percentageChange = percentageChange;
        this.currentPeriodLabel = currentPeriodLabel != null ? currentPeriodLabel : "Current";
        this.previousPeriodLabel = previousPeriodLabel != null ? previousPeriodLabel : "Previous";
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public double getCurrentAmountDouble() {
        return currentAmount.doubleValue();
    }

    public String getFormattedCurrentAmount() {
        return MoneyUtil.formatRupees(currentAmount);
    }

    public BigDecimal getPreviousAmount() {
        return previousAmount;
    }

    public double getPreviousAmountDouble() {
        return previousAmount.doubleValue();
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

    public String getCurrentTooltipText() {
        return String.format("📅 %s\n🏷️ %s\n💰 Current: %s\n📊 Shift vs Prior: %s (%s)",
                currentPeriodLabel,
                category,
                getFormattedCurrentAmount(),
                getFormattedDifference(),
                getFormattedPercentageChange());
    }

    public String getPreviousTooltipText() {
        return String.format("⏮️ %s\n🏷️ %s\n💰 Prior: %s",
                previousPeriodLabel,
                category,
                getFormattedPreviousAmount());
    }
}
