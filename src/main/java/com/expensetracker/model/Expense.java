package com.expensetracker.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Model class representing an Expense record.
 *
 * Demonstrates Object-Oriented Programming (OOP) principles:
 * - Encapsulation (private fields with controlled getters/setters)
 * - Multiple constructors
 * - Input validation
 * - Value object equality (equals, hashCode, toString)
 */
public class Expense {

    private Integer id;
    private double amount;
    private String category;
    private String description;
    private LocalDate date;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Default constructor.
     */
    public Expense() {
    }

    /**
     * Constructor used when creating a new unsaved expense.
     */
    public Expense(
            double amount,
            String category,
            String description,
            LocalDate date) {

        setAmount(amount);
        setCategory(category);
        setDescription(description);
        setDate(date);
    }

    /**
     * Full constructor used when retrieving an expense from the database.
     */
    public Expense(
            Integer id,
            double amount,
            String category,
            String description,
            LocalDate date) {

        this.id = id;

        setAmount(amount);
        setCategory(category);
        setDescription(description);
        setDate(date);
    }

    // =========================
    // Getters and Setters
    // =========================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public java.math.BigDecimal getAmountBigDecimal() {
        return com.expensetracker.util.MoneyUtil.toBigDecimal(amount);
    }

    public void setAmount(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException(
                    "Expense amount cannot be negative."
            );
        }
        // Round to 2 decimal places to prevent floating-point discrepancies
        this.amount = com.expensetracker.util.MoneyUtil.toBigDecimal(amount).doubleValue();
    }

    public void setAmount(java.math.BigDecimal amountBigDecimal) {
        if (amountBigDecimal == null || amountBigDecimal.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Expense amount cannot be negative."
            );
        }
        this.amount = amountBigDecimal.setScale(com.expensetracker.util.MoneyUtil.SCALE, com.expensetracker.util.MoneyUtil.ROUNDING_MODE).doubleValue();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category =
                category != null && !category.trim().isEmpty()
                        ? category.trim()
                        : "Uncategorized";
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (description != null && description.trim().length() > 255) {
            throw new IllegalArgumentException("Description cannot exceed 255 characters.");
        }
        this.description =
                description != null
                        ? description.trim()
                        : "";
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date =
                date != null
                        ? date
                        : LocalDate.now();
    }

    /**
     * Validates that the expense is ready for persistence.
     * @throws IllegalArgumentException with a clear user-facing message if invalid
     */
    public void validate() {
        if (amount <= 0) {
            throw new IllegalArgumentException("Expense amount must be greater than ₹0.00.");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty.");
        }
        if (description.trim().length() > 255) {
            throw new IllegalArgumentException("Description cannot exceed 255 characters.");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date must be selected.");
        }
        if (date.isAfter(LocalDate.now().plusDays(365))) {
            throw new IllegalArgumentException("Date cannot be more than one year in the future.");
        }
    }

    // =========================
    // Display Helper Methods
    // =========================

    /**
     * Returns the expense amount formatted as Indian Rupees with 2 decimal places.
     */
    public String getFormattedAmount() {
        return com.expensetracker.util.MoneyUtil.formatRupees(amount);
    }

    /**
     * Returns the date in yyyy-MM-dd format.
     */
    public String getFormattedDate() {
        return date != null
                ? date.format(DATE_FORMATTER)
                : "";
    }

    // =========================
    // Object Methods
    // =========================

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Expense expense = (Expense) o;

        return Double.compare(expense.amount, amount) == 0
                && Objects.equals(id, expense.id)
                && Objects.equals(category, expense.category)
                && Objects.equals(description, expense.description)
                && Objects.equals(date, expense.date);
    }

    @Override
    public int hashCode() {

        return Objects.hash(
                id,
                amount,
                category,
                description,
                date
        );
    }

    @Override
    public String toString() {

        return "Expense{" +
                "id=" + id +
                ", amount=" + amount +
                ", category='" + category + '\'' +
                ", description='" + description + '\'' +
                ", date=" + date +
                '}';
    }
}