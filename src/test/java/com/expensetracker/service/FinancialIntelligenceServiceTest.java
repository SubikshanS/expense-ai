package com.expensetracker.service;

import com.expensetracker.model.CategoryTrend;
import com.expensetracker.model.Expense;
import com.expensetracker.model.FinancialIntelligenceResult;
import com.expensetracker.model.MonthComparison;
import com.expensetracker.model.SpendingAnomaly;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FinancialIntelligenceServiceTest {

    private FinancialIntelligenceService service;

    @BeforeEach
    void setUp() {
        service = new FinancialIntelligenceService();
    }

    // =========================================================================
    // 1. Empty & Small Datasets
    // =========================================================================

    @Test
    void testEmptyDatasetReturnsSafeEmptyResult() {
        FinancialIntelligenceResult result = service.analyze(Collections.emptyList());
        assertFalse(result.isHasData());
        assertTrue(result.getAnomalies().isEmpty());
        assertTrue(result.getDailySpending().isEmpty());
        assertTrue(result.getWeeklySpending().isEmpty());
        assertTrue(result.getMonthlySpending().isEmpty());
        assertFalse(result.getMonthComparison().isSufficientHistory());
        assertTrue(result.getMonthComparison().getStatusMessage().contains("No transaction"));
    }

    @Test
    void testSingleMonthDatasetExplainsInsufficientHistory() {
        List<Expense> expenses = Arrays.asList(
                new Expense(1, 100.0, "Food & Dining", "Dinner", LocalDate.of(2026, 10, 1)),
                new Expense(2, 150.0, "Food & Dining", "Groceries", LocalDate.of(2026, 10, 2))
        );

        FinancialIntelligenceResult result = service.analyze(expenses);
        assertTrue(result.isHasData());
        assertFalse(result.getMonthComparison().isSufficientHistory());
        assertTrue(result.getMonthComparison().getStatusMessage().contains("At least 2 distinct months are required"));
    }

    @Test
    void testSmallCategorySampleSizeDoesNotFlagFalseAnomalies() {
        // Less than 3 transactions in a category: standard deviation should NOT be computed
        List<Expense> expenses = Arrays.asList(
                new Expense(1, 50.0, "Transport", "Bus", LocalDate.of(2026, 10, 1)),
                new Expense(2, 500.0, "Transport", "Flight", LocalDate.of(2026, 10, 2))
        );

        List<SpendingAnomaly> anomalies = service.detectAnomalies(expenses, Collections.emptyMap());
        assertTrue(anomalies.isEmpty(), "Categories with fewer than 3 transactions must not produce statistical false positives.");
    }

    // =========================================================================
    // 2. Normal Spending Patterns
    // =========================================================================

    @Test
    void testNormalSpendingPatternProducesNoAnomalies() {
        // Normal distribution of daily expenses around ₹100-₹150
        List<Expense> expenses = Arrays.asList(
                new Expense(1, 100.0, "Food & Dining", "Lunch", LocalDate.of(2026, 10, 1)),
                new Expense(2, 110.0, "Food & Dining", "Dinner", LocalDate.of(2026, 10, 2)),
                new Expense(3, 105.0, "Food & Dining", "Snack", LocalDate.of(2026, 10, 3)),
                new Expense(4, 95.0, "Food & Dining", "Breakfast", LocalDate.of(2026, 10, 4)),
                new Expense(5, 115.0, "Food & Dining", "Lunch", LocalDate.of(2026, 10, 5))
        );

        List<SpendingAnomaly> anomalies = service.detectAnomalies(expenses, Collections.emptyMap());
        assertTrue(anomalies.isEmpty(), "Normal fluctuations within 1 standard deviation should not be flagged.");
    }

    // =========================================================================
    // 3. Unusually High Spending (Z-Score Outlier)
    // =========================================================================

    @Test
    void testDetectsStatisticalOutlierWithEvidence() {
        // Normal baseline around ₹100, plus an extreme spike of ₹1,500
        List<Expense> expenses = Arrays.asList(
                new Expense(1, 100.0, "Food & Dining", "Regular Lunch", LocalDate.of(2026, 10, 1)),
                new Expense(2, 110.0, "Food & Dining", "Regular Dinner", LocalDate.of(2026, 10, 2)),
                new Expense(3, 105.0, "Food & Dining", "Regular Snack", LocalDate.of(2026, 10, 3)),
                new Expense(4, 95.0, "Food & Dining", "Regular Breakfast", LocalDate.of(2026, 10, 4)),
                new Expense(5, 100.0, "Food & Dining", "Regular Lunch", LocalDate.of(2026, 10, 5)),
                new Expense(6, 1500.0, "Food & Dining", "Grand Banquet Party", LocalDate.of(2026, 10, 6))
        );

        List<SpendingAnomaly> anomalies = service.detectAnomalies(expenses, Collections.emptyMap());
        assertFalse(anomalies.isEmpty());

        SpendingAnomaly spike = anomalies.get(0);
        assertEquals(SpendingAnomaly.AnomalyType.TRANSACTION_SPIKE, spike.getType());
        assertEquals("Food & Dining", spike.getCategory());
        assertEquals(new BigDecimal("1500.00"), spike.getObservedAmount());
        assertTrue(spike.getDeviationScore() >= 2.0);
        assertTrue(spike.getExplanation().contains("standard deviations"));
        assertEquals(1, spike.getSupportingTransactions().size());
        assertEquals("Grand Banquet Party", spike.getSupportingTransactions().get(0).getDescription());
    }

    // =========================================================================
    // 4. Zero Variance Safeguard
    // =========================================================================

    @Test
    void testZeroVarianceHandling() {
        // Exactly identical expenses (std dev = 0)
        List<Expense> expenses = Arrays.asList(
                new Expense(1, 100.0, "Subscriptions", "Netflix", LocalDate.of(2026, 8, 1)),
                new Expense(2, 100.0, "Subscriptions", "Netflix", LocalDate.of(2026, 9, 1)),
                new Expense(3, 100.0, "Subscriptions", "Netflix", LocalDate.of(2026, 10, 1)),
                new Expense(4, 350.0, "Subscriptions", "Annual Cloud Pro Upgrade", LocalDate.of(2026, 10, 15))
        );

        List<SpendingAnomaly> anomalies = service.detectAnomalies(expenses, Collections.emptyMap());
        assertEquals(1, anomalies.size());

        SpendingAnomaly anomaly = anomalies.get(0);
        assertTrue(anomaly.getExplanation().contains("identical"));
        assertEquals(new BigDecimal("350.00"), anomaly.getObservedAmount());
        assertEquals(new BigDecimal("100.00"), anomaly.getBaselineAmount());
    }

    // =========================================================================
    // 5. Month-over-Month Comparison & Contribution Drivers
    // =========================================================================

    @Test
    void testMonthOverMonthComparisonAndAttribution() {
        List<Expense> expenses = Arrays.asList(
                // September (Previous Month): Total = 1,000
                new Expense(1, 400.0, "Housing & Utilities", "Electric bill", LocalDate.of(2026, 9, 5)),
                new Expense(2, 600.0, "Shopping", "Clothes", LocalDate.of(2026, 9, 10)),

                // October (Current Month): Total = 2,000 (+1,000 increase)
                // Housing stayed 400, Shopping jumped from 600 to 1,600 (+1,000 increase)
                new Expense(3, 400.0, "Housing & Utilities", "Electric bill", LocalDate.of(2026, 10, 5)),
                new Expense(4, 1600.0, "Shopping", "New Laptop accessory", LocalDate.of(2026, 10, 15))
        );

        Map<YearMonth, BigDecimal> monthlyTotals = service.calculateMonthlySpending(expenses);
        MonthComparison comparison = service.analyzeMonthOverMonth(expenses, monthlyTotals);

        assertTrue(comparison.isSufficientHistory());
        assertEquals(YearMonth.of(2026, 10), comparison.getCurrentMonth());
        assertEquals(YearMonth.of(2026, 9), comparison.getPreviousMonth());
        assertEquals(new BigDecimal("2000.00"), comparison.getCurrentTotal());
        assertEquals(new BigDecimal("1000.00"), comparison.getPreviousTotal());
        assertEquals(new BigDecimal("1000.00"), comparison.getNetDifference());
        assertEquals(100.0, comparison.getPercentageChange());

        // Verify largest increase driver is Shopping (100% of the increase)
        assertFalse(comparison.getTopIncreaseContributors().isEmpty());
        CategoryTrend topDriver = comparison.getTopIncreaseContributors().get(0);
        assertEquals("Shopping", topDriver.getCategory());
        assertEquals(new BigDecimal("1000.00"), topDriver.getDifference());
        assertEquals(100.0, topDriver.getContributionToIncrease());
    }

    // =========================================================================
    // 6. Date Boundary Logic (Year Crossover Dec -> Jan)
    // =========================================================================

    @Test
    void testYearCrossoverMonthComparison() {
        List<Expense> expenses = Arrays.asList(
                new Expense(1, 500.0, "Food & Dining", "Holiday Dinner", LocalDate.of(2025, 12, 28)),
                new Expense(2, 600.0, "Food & Dining", "New Year Groceries", LocalDate.of(2026, 1, 3))
        );

        Map<YearMonth, BigDecimal> monthlyTotals = service.calculateMonthlySpending(expenses);
        MonthComparison comparison = service.analyzeMonthOverMonth(expenses, monthlyTotals);

        assertTrue(comparison.isSufficientHistory());
        assertEquals(YearMonth.of(2026, 1), comparison.getCurrentMonth());
        assertEquals(YearMonth.of(2025, 12), comparison.getPreviousMonth());
        assertEquals(new BigDecimal("600.00"), comparison.getCurrentTotal());
        assertEquals(new BigDecimal("500.00"), comparison.getPreviousTotal());
        assertEquals(new BigDecimal("100.00"), comparison.getNetDifference());
        assertEquals(20.0, comparison.getPercentageChange());
    }

    // =========================================================================
    // 7. Non-Contiguous Historical Periods (Gaps Between Months)
    // =========================================================================

    @Test
    void testNonContiguousMonthsWithGaps() {
        // July and October (gap in August and September)
        List<Expense> expenses = Arrays.asList(
                new Expense(1, 300.0, "Transport", "Taxi", LocalDate.of(2026, 7, 10)),
                new Expense(2, 600.0, "Transport", "Train", LocalDate.of(2026, 10, 12))
        );

        Map<YearMonth, BigDecimal> monthlyTotals = service.calculateMonthlySpending(expenses);
        MonthComparison comparison = service.analyzeMonthOverMonth(expenses, monthlyTotals);

        assertTrue(comparison.isSufficientHistory());
        assertEquals(YearMonth.of(2026, 10), comparison.getCurrentMonth());
        assertEquals(YearMonth.of(2026, 7), comparison.getPreviousMonth());
        assertEquals(new BigDecimal("300.00"), comparison.getNetDifference());
    }

    // =========================================================================
    // 8. Daily and Weekly Aggregations
    // =========================================================================

    @Test
    void testDailyAndWeeklySpendingAggregation() {
        List<Expense> expenses = Arrays.asList(
                new Expense(1, 100.0, "Food", "Snack", LocalDate.of(2026, 10, 1)),
                new Expense(2, 50.0, "Food", "Coffee", LocalDate.of(2026, 10, 1)),
                new Expense(3, 200.0, "Transport", "Ride", LocalDate.of(2026, 10, 2))
        );

        Map<LocalDate, BigDecimal> daily = service.calculateDailySpending(expenses);
        assertEquals(2, daily.size());
        assertEquals(new BigDecimal("150.00"), daily.get(LocalDate.of(2026, 10, 1)));
        assertEquals(new BigDecimal("200.00"), daily.get(LocalDate.of(2026, 10, 2)));

        Map<String, BigDecimal> weekly = service.calculateWeeklySpending(expenses);
        assertFalse(weekly.isEmpty());
    }
}
