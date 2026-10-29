package com.expensetracker.service;

import com.expensetracker.model.CategoryMoMPoint;
import com.expensetracker.model.CategorySpendingPoint;
import com.expensetracker.model.Expense;
import com.expensetracker.model.MoMComparisonResult;
import com.expensetracker.model.TimeRange;
import com.expensetracker.model.TimeSeriesPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AnalyticsServiceTest {

    private AnalyticsService analyticsService;
    private LocalDate anchor;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService();
        anchor = LocalDate.of(2026, 10, 29);
    }

    @Test
    void testSpendingOverTimeLast7Days() {
        List<Expense> expenses = List.of(
                new Expense(1, 100.0, "Food", "Lunch", anchor.minusDays(1)),
                new Expense(2, 250.0, "Food", "Dinner", anchor.minusDays(1)),
                new Expense(3, 50.0, "Transport", "Bus", anchor.minusDays(5))
        );

        List<TimeSeriesPoint> points = analyticsService.calculateSpendingOverTime(expenses, TimeRange.LAST_7_DAYS, anchor);

        assertEquals(7, points.size(), "Should have exactly 7 daily points for Last 7 Days");

        // The day minus 1 should have 350.00 (100 + 250)
        TimeSeriesPoint dayMinus1 = points.stream()
                .filter(p -> p.getDate().equals(anchor.minusDays(1)))
                .findFirst()
                .orElseThrow();

        assertEquals(0, new BigDecimal("350.00").compareTo(dayMinus1.getAmount()));
        assertEquals(2, dayMinus1.getTransactionCount());

        // Inactive day should be exactly 0
        TimeSeriesPoint dayMinus2 = points.stream()
                .filter(p -> p.getDate().equals(anchor.minusDays(2)))
                .findFirst()
                .orElseThrow();

        assertEquals(0, BigDecimal.ZERO.compareTo(dayMinus2.getAmount()));
        assertEquals(0, dayMinus2.getTransactionCount());
    }

    @Test
    void testSpendingOverTimeLast30Days() {
        List<Expense> expenses = List.of(
                new Expense(1, 500.0, "Shopping", "Shoes", anchor.minusDays(10))
        );

        List<TimeSeriesPoint> points = analyticsService.calculateSpendingOverTime(expenses, TimeRange.LAST_30_DAYS, anchor);
        assertEquals(30, points.size(), "Should have exactly 30 daily points");
    }

    @Test
    void testSpendingOverTimeMonthly6Months() {
        List<Expense> expenses = List.of(
                new Expense(1, 1000.0, "Rent", "October Rent", LocalDate.of(2026, 10, 5)),
                new Expense(2, 1000.0, "Rent", "September Rent", LocalDate.of(2026, 9, 5)),
                new Expense(3, 1000.0, "Rent", "August Rent", LocalDate.of(2026, 8, 5))
        );

        List<TimeSeriesPoint> points = analyticsService.calculateSpendingOverTime(expenses, TimeRange.LAST_6_MONTHS, anchor);
        assertEquals(6, points.size(), "Should have exactly 6 monthly points");

        TimeSeriesPoint oct = points.get(points.size() - 1);
        assertEquals(YearMonth.of(2026, 10), oct.getYearMonth());
        assertEquals(0, new BigDecimal("1000.00").compareTo(oct.getAmount()));
    }

    @Test
    void testSpendingOverTimeEmptyList() {
        List<TimeSeriesPoint> points = analyticsService.calculateSpendingOverTime(Collections.emptyList(), TimeRange.LAST_7_DAYS, anchor);
        assertEquals(7, points.size());
        for (TimeSeriesPoint p : points) {
            assertEquals(0, BigDecimal.ZERO.compareTo(p.getAmount()));
            assertEquals(0, p.getTransactionCount());
        }
    }

    @Test
    void testCategorySpendingSortedDescending() {
        List<Expense> expenses = List.of(
                new Expense(1, 200.0, "Transport", "Taxi", anchor),
                new Expense(2, 800.0, "Rent", "Room", anchor),
                new Expense(3, 500.0, "Food", "Groceries", anchor)
        );

        List<CategorySpendingPoint> points = analyticsService.calculateCategorySpending(expenses);

        assertEquals(3, points.size());
        // Rent (800) > Food (500) > Transport (200)
        assertEquals("Rent", points.get(0).getCategory());
        assertEquals("Food", points.get(1).getCategory());
        assertEquals("Transport", points.get(2).getCategory());

        // Total = 1500; Rent = 800 / 1500 = 53.33%
        assertEquals(53.33, points.get(0).getPercentage(), 0.1);
    }

    @Test
    void testCategorySpendingEmpty() {
        List<CategorySpendingPoint> points = analyticsService.calculateCategorySpending(Collections.emptyList());
        assertTrue(points.isEmpty());
    }

    @Test
    void testMoMComparisonNormalizedMtd() {
        // Today is 2026-10-29. Day 29 cutoff.
        // October: Expense on Day 10 (300), and Day 30 (which shouldn't be counted if cutoff is 29!)
        // September: Expense on Day 10 (200), and Day 30 (should be excluded because day 30 > 29!)
        List<Expense> expenses = List.of(
                new Expense(1, 300.0, "Food", "Oct Mid", LocalDate.of(2026, 10, 10)),
                new Expense(2, 500.0, "Food", "Oct Late", LocalDate.of(2026, 10, 31)), // after anchor day 29
                new Expense(3, 200.0, "Food", "Sep Mid", LocalDate.of(2026, 9, 10)),
                new Expense(4, 900.0, "Food", "Sep End", LocalDate.of(2026, 9, 30))  // after anchor day 29
        );

        MoMComparisonResult result = analyticsService.calculateMoMComparison(expenses, true, anchor);

        assertTrue(result.isSufficientHistory());
        assertTrue(result.isDayNormalized());
        assertEquals(29, result.getCurrentDaysEvaluated());
        assertEquals(29, result.getPreviousDaysEvaluated());

        // Current MTD evaluated should be only Day 10 (300.00)
        assertEquals(0, new BigDecimal("300.00").compareTo(result.getCurrentTotal()));
        // Previous MTD evaluated should be only Day 10 (200.00)
        assertEquals(0, new BigDecimal("200.00").compareTo(result.getPreviousTotal()));

        assertEquals(1, result.getCategoryPoints().size());
        CategoryMoMPoint foodPoint = result.getCategoryPoints().get(0);
        assertEquals("Food", foodPoint.getCategory());
        assertEquals(0, new BigDecimal("300.00").compareTo(foodPoint.getCurrentAmount()));
        assertEquals(0, new BigDecimal("200.00").compareTo(foodPoint.getPreviousAmount()));
        assertEquals(0, new BigDecimal("100.00").compareTo(foodPoint.getDifference()));
    }

    @Test
    void testMoMComparisonInsufficientHistory() {
        List<Expense> expenses = List.of(
                new Expense(1, 100.0, "Food", "Snack", anchor)
        );

        MoMComparisonResult result = analyticsService.calculateMoMComparison(expenses, true, anchor);
        assertFalse(result.isSufficientHistory());
        assertTrue(result.getStatusMessage().contains("distinct calendar months"));
    }

    @Test
    void testYearCrossoverBoundary() {
        // January 2026 vs December 2025
        LocalDate janAnchor = LocalDate.of(2026, 1, 15);
        List<Expense> expenses = List.of(
                new Expense(1, 500.0, "Utilities", "Power", LocalDate.of(2026, 1, 5)),
                new Expense(2, 400.0, "Utilities", "Power", LocalDate.of(2025, 12, 5))
        );

        MoMComparisonResult result = analyticsService.calculateMoMComparison(expenses, true, janAnchor);
        assertTrue(result.isSufficientHistory());
        assertEquals(YearMonth.of(2026, 1), result.getCurrentMonth());
        assertEquals(YearMonth.of(2025, 12), result.getPreviousMonth());
    }
}
