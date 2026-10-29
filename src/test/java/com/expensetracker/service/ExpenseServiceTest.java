package com.expensetracker.service;

import com.expensetracker.dao.ExpenseDAO;
import com.expensetracker.exception.ValidationException;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ExpenseServiceTest {

    private ExpenseService service;

    @BeforeEach
    void setUp() {
        service = new ExpenseService();
    }

    @Test
    void testCalculateSummaryWithValidExpenses() {
        List<Expense> expenses = Arrays.asList(
                new Expense(1, 150.00, "Food & Dining", "Dinner", LocalDate.of(2026, 10, 1)),
                new Expense(2, 200.00, "Food & Dining", "Groceries", LocalDate.of(2026, 10, 2)),
                new Expense(3, 100.00, "Transport", "Cab fare", LocalDate.of(2026, 10, 3))
        );

        ExpenseSummary summary = service.calculateSummary(expenses);
        assertEquals(new BigDecimal("450.00"), summary.getTotalSpending());
        assertEquals(3, summary.getTransactionCount());
        assertEquals(new BigDecimal("150.00"), summary.getAverageExpense());
        assertEquals("Food & Dining", summary.getTopCategory());
        assertEquals(new BigDecimal("350.00"), summary.getTopCategoryAmount());
        assertEquals("₹450.00", summary.getFormattedTotalSpending());
    }

    @Test
    void testCalculateSummaryEmptyList() {
        ExpenseSummary summary = service.calculateSummary(Collections.emptyList());
        assertEquals(BigDecimal.ZERO.setScale(2), summary.getTotalSpending());
        assertEquals(0, summary.getTransactionCount());
        assertEquals("None", summary.getTopCategory());
    }

    @Test
    void testCalculateMonthlySpending() {
        List<Expense> expenses = Arrays.asList(
                new Expense(1, 150.00, "Food & Dining", "Snack", LocalDate.of(2026, 9, 15)),
                new Expense(2, 250.00, "Transport", "Taxi", LocalDate.of(2026, 9, 20)),
                new Expense(3, 400.00, "Shopping", "Shoes", LocalDate.of(2026, 10, 5))
        );

        Map<YearMonth, BigDecimal> monthly = service.calculateMonthlySpending(expenses);
        assertEquals(2, monthly.size());
        assertEquals(new BigDecimal("400.00"), monthly.get(YearMonth.of(2026, 9)));
        assertEquals(new BigDecimal("400.00"), monthly.get(YearMonth.of(2026, 10)));
    }

    @Test
    void testValidationFailsForInvalidInputs() {
        assertThrows(ValidationException.class, () -> service.addExpense(-50, "Food", "Dinner", LocalDate.now()));
        assertThrows(ValidationException.class, () -> service.addExpense(100, "Food", "", LocalDate.now()));
    }
}
