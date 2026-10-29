package com.expensetracker.model;

import com.expensetracker.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Represents a statistically detected spending anomaly backed by actual database records.
 * Does not claim fraud detection; flags significant deviations from historical baselines.
 */
public class SpendingAnomaly {

    public enum AnomalyType {
        TRANSACTION_SPIKE,   // Single expense significantly above historical category baseline
        PERIODIC_SURGE       // Category total over a period significantly above historical monthly baseline
    }

    private final AnomalyType type;
    private final String title;
    private final String category;
    private final BigDecimal observedAmount;
    private final BigDecimal baselineAmount;
    private final BigDecimal difference;
    private final double deviationScore;
    private final String explanation;
    private final LocalDate date;
    private final List<Expense> supportingTransactions;

    public SpendingAnomaly(
            AnomalyType type,
            String title,
            String category,
            BigDecimal observedAmount,
            BigDecimal baselineAmount,
            BigDecimal difference,
            double deviationScore,
            String explanation,
            LocalDate date,
            List<Expense> supportingTransactions) {

        this.type = type;
        this.title = title;
        this.category = category != null ? category : "Uncategorized";
        this.observedAmount = observedAmount != null ? observedAmount : MoneyUtil.ZERO;
        this.baselineAmount = baselineAmount != null ? baselineAmount : MoneyUtil.ZERO;
        this.difference = difference != null ? difference : MoneyUtil.ZERO;
        this.deviationScore = deviationScore;
        this.explanation = explanation;
        this.date = date;
        this.supportingTransactions = supportingTransactions != null
                ? Collections.unmodifiableList(supportingTransactions)
                : Collections.emptyList();
    }

    public AnomalyType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getObservedAmount() {
        return observedAmount;
    }

    public String getFormattedObservedAmount() {
        return MoneyUtil.formatRupees(observedAmount);
    }

    public BigDecimal getBaselineAmount() {
        return baselineAmount;
    }

    public String getFormattedBaselineAmount() {
        return MoneyUtil.formatRupees(baselineAmount);
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public String getFormattedDifference() {
        return MoneyUtil.formatRupees(difference);
    }

    public double getDeviationScore() {
        return deviationScore;
    }

    public String getExplanation() {
        return explanation;
    }

    public LocalDate getDate() {
        return date;
    }

    public List<Expense> getSupportingTransactions() {
        return supportingTransactions;
    }

    @Override
    public String toString() {
        return "SpendingAnomaly{" +
                "type=" + type +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", observedAmount=" + observedAmount +
                ", baselineAmount=" + baselineAmount +
                ", difference=" + difference +
                ", deviationScore=" + deviationScore +
                '}';
    }
}
