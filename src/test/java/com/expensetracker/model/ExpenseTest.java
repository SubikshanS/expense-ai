package com.expensetracker.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ExpenseTest {

    @Test
    void testExpenseCreationAndDefaults() {
        Expense expense = new Expense(150.75, "Food & Dining", "Team lunch", LocalDate.of(2026, 10, 15));
        assertEquals(150.75, expense.getAmount());
        assertEquals("Food & Dining", expense.getCategory());
        assertEquals("Team lunch", expense.getDescription());
        assertEquals(LocalDate.of(2026, 10, 15), expense.getDate());
        assertEquals("₹150.75", expense.getFormattedAmount());
        assertEquals("2026-10-15", expense.getFormattedDate());
    }

    @Test
    void testExpenseValidationSuccess() {
        Expense expense = new Expense(50.00, "Transport", "Metro ride", LocalDate.now());
        assertDoesNotThrow(expense::validate);
    }
    
    @Test
    void testExpenseValidationRejectsZeroOrNegative() {
        Expense expenseZero = new Expense(0.0, "Transport", "Metro ride", LocalDate.now());
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, expenseZero::validate);
        assertTrue(ex1.getMessage().contains("greater than ₹0.00"));

        assertThrows(IllegalArgumentException.class, () -> new Expense(-10.0, "Transport", "Metro ride", LocalDate.now()));
    }

    @Test
    void testExpenseValidationRejectsEmptyDescription() {
        Expense expense = new Expense(50.00, "Transport", "   ", LocalDate.now());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, expense::validate);
        assertTrue(ex.getMessage().contains("Description cannot be empty"));
    }

    @Test
    void testExpenseValidationRejectsLongDescription() {
        String longText = "A".repeat(260);
        assertThrows(IllegalArgumentException.class, () -> new Expense(50.00, "Transport", longText, LocalDate.now()));
    }

    @Test
    void testCategoryDefaultsToUncategorizedWhenNullOrEmpty() {
        Expense expense = new Expense(100.0, "", "Test", LocalDate.now());
        assertEquals("Uncategorized", expense.getCategory());

        expense.setCategory(null);
        assertEquals("Uncategorized", expense.getCategory());
    }
}
