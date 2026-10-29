package com.expensetracker.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;

/**
 * Utility class providing precise monetary operations and formatting using BigDecimal.
 * Prevents IEEE-754 binary floating-point rounding inaccuracies in financial calculations.
 */
public final class MoneyUtil {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);

    private static final NumberFormat CURRENCY_FORMAT;

    static {
        CURRENCY_FORMAT = NumberFormat.getNumberInstance(Locale.US);
        CURRENCY_FORMAT.setMinimumFractionDigits(SCALE);
        CURRENCY_FORMAT.setMaximumFractionDigits(SCALE);
        CURRENCY_FORMAT.setGroupingUsed(true);
    }

    private MoneyUtil() {
        // Utility class
    }
    
    /**
     * Converts a double amount to a safely scaled BigDecimal.
     */
    public static BigDecimal toBigDecimal(double amount) {
        return BigDecimal.valueOf(amount).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Formats a monetary amount with Indian Rupee symbol (₹) and 2 decimal places.
     */
    public static synchronized String formatRupees(double amount) {
        return formatRupees(toBigDecimal(amount));
    }

    /**
     * Formats a BigDecimal monetary amount with Indian Rupee symbol (₹) and 2 decimal places.
     */
    public static synchronized String formatRupees(BigDecimal amount) {
        if (amount == null) {
            return "₹0.00";
        }
        return "₹" + CURRENCY_FORMAT.format(amount.setScale(SCALE, ROUNDING_MODE));
    }

    /**
     * Formats plain numeric value without currency symbol for CSV/Excel/Textfields.
     */
    public static synchronized String formatPlain(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return String.format(Locale.US, "%.2f", amount.setScale(SCALE, ROUNDING_MODE));
    }

    /**
     * Safely sums a collection of doubles using BigDecimal precision.
     */
    public static BigDecimal sumDoubles(Collection<Double> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (Double amt : amounts) {
            if (amt != null) {
                sum = sum.add(BigDecimal.valueOf(amt));
            }
        }
        return sum.setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Parses a string input into a validated positive BigDecimal amount.
     *
     * @param input Raw user input string
     * @return Validated BigDecimal amount with 2 decimal places
     * @throws IllegalArgumentException if null, empty, non-numeric, negative, or zero
     */
    public static BigDecimal parseAmount(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount cannot be empty.");
        }

        String cleaned = input.trim().replace(",", "").replace("₹", "");
        try {
            BigDecimal parsed = new BigDecimal(cleaned).setScale(SCALE, ROUNDING_MODE);
            if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Expense amount must be strictly greater than ₹0.00.");
            }
            if (parsed.compareTo(new BigDecimal("1000000000")) > 0) {
                throw new IllegalArgumentException("Expense amount exceeds realistic limit (₹1,000,000,000).");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a valid numeric amount (e.g. 150.50).");
        }
    }
}
