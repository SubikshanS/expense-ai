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

    public void setAmount(double amount) {

        if (amount < 0) {
            throw new IllegalArgumentException(
                    "Expense amount cannot be negative."
            );
        }

        this.amount = amount;
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

    // =========================
    // Display Helper Methods
    // =========================

    /**
     * Returns the expense amount formatted as Indian Rupees.
     */
    public String getFormattedAmount() {
        return String.format("₹%.2f", amount);
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