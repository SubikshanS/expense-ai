package com.expensetracker.util;

import com.expensetracker.model.Expense;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class to export Expense records and aggregated data
 * to CSV format using Core Java FileWriter and BufferedWriter.
 */
public final class CSVExporter {

    private CSVExporter() {
        // Utility class - prevent object creation
    }

    /**
     * Exports a list of expenses to the specified CSV file.
     *
     * @param file target CSV file
     * @param expenseList list of Expense records
     * @return true if export succeeded, false for invalid input
     * @throws IOException if file writing fails
     */
    public static boolean exportToCSV(
            File file,
            List<Expense> expenseList) throws IOException {

        if (file == null || expenseList == null) {
            return false;
        }

        try (BufferedWriter writer =
                     new BufferedWriter(new FileWriter(file))) {

            // CSV header
            writer.write("ID,Date,Category,Description,Amount");
            writer.newLine();

            double grandTotal = 0.0;

            // HashMap used for category-wise aggregation
            Map<String, Double> categoryAggregates =
                    new HashMap<>();

            // Export each expense
            for (Expense expense : expenseList) {

                if (expense == null) {
                    continue;
                }

                String idStr =
                        expense.getId() != null
                                ? String.valueOf(expense.getId())
                                : "";

                String dateStr =
                        expense.getFormattedDate();

                String categoryStr =
                        escapeCSV(expense.getCategory());

                String descriptionStr =
                        escapeCSV(expense.getDescription());

                String amountStr =
                        String.format(
                                Locale.US,
                                "%.2f",
                                expense.getAmount()
                        );

                writer.write(
                        String.format(
                                Locale.US,
                                "%s,%s,%s,%s,%s",
                                idStr,
                                dateStr,
                                categoryStr,
                                descriptionStr,
                                amountStr
                        )
                );

                writer.newLine();

                // Calculate total spending
                grandTotal += expense.getAmount();

                // Calculate category totals
                categoryAggregates.put(
                        expense.getCategory(),
                        categoryAggregates.getOrDefault(
                                expense.getCategory(),
                                0.0
                        ) + expense.getAmount()
                );
            }

            // Summary section
            writer.newLine();
            writer.write("# SUMMARY REPORT");
            writer.newLine();

            writer.write(
                    "# Generated At: " +
                    LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern(
                                    "yyyy-MM-dd HH:mm:ss"
                            )
                    )
            );
            writer.newLine();

            writer.write(
                    String.format(
                            Locale.US,
                            "# Total Records: %d",
                            expenseList.size()
                    )
            );
            writer.newLine();

            writer.write(
                    String.format(
                            Locale.US,
                            "# Grand Total Spending: ₹%.2f",
                            grandTotal
                    )
            );
            writer.newLine();

            writer.write("# Category Breakdown:");
            writer.newLine();

            for (Map.Entry<String, Double> entry :
                    categoryAggregates.entrySet()) {

                writer.write(
                        String.format(
                                Locale.US,
                                "# - %s: ₹%.2f",
                                entry.getKey(),
                                entry.getValue()
                        )
                );

                writer.newLine();
            }

            return true;
        }
    }

    /**
     * Escapes CSV special characters.
     *
     * Fields containing commas, double quotes, or newlines
     * are surrounded by double quotes.
     */
    private static String escapeCSV(String input) {

        if (input == null) {
            return "";
        }

        if (input.contains(",")
                || input.contains("\"")
                || input.contains("\n")
                || input.contains("\r")) {

            return "\"" +
                    input.replace("\"", "\"\"") +
                    "\"";
        }

        return input;
    }
}