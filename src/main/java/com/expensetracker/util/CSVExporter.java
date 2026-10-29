package com.expensetracker.util;

import com.expensetracker.model.CategoryReportRow;
import com.expensetracker.model.Expense;
import com.expensetracker.model.FinancialReport;

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
     * Exports a financial report to the specified CSV file with metadata and category rollups.
     */
    public static boolean exportReportToCSV(File file, FinancialReport report) throws IOException {
        if (file == null || report == null) {
            return false;
        }

        try (BufferedWriter writer = new BufferedWriter(
                new java.io.OutputStreamWriter(new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {

            writer.write("\uFEFF"); // UTF-8 Byte Order Mark for Excel compatibility
            writer.write("# " + escapeCSV(report.getReportTitle()));
            writer.newLine();
            writer.write("# Date Range: " + escapeCSV(report.getDateRangeDescription()));
            writer.newLine();
            writer.write("# Generated At: " + report.getFormattedGeneratedAt());
            writer.newLine();
            writer.write(String.format(Locale.US, "# Total Records: %d | Total Spend: ₹%.2f | Avg per Tx: ₹%.2f",
                    report.getTransactionCount(), report.getTotalSpendingDouble(), report.getAverageExpense().doubleValue()));
            writer.newLine();
            writer.write(String.format(Locale.US, "# Top Category: %s | Peak Spending: %s",
                    report.getTopCategory(), report.getPeakSpendingDay()));
            writer.newLine();
            writer.newLine();

            // Transaction Table
            writer.write("ID,Date,Category,Description,Amount");
            writer.newLine();

            for (Expense expense : report.getExpenses()) {
                if (expense == null) continue;
                String idStr = expense.getId() != null ? String.valueOf(expense.getId()) : "";
                String dateStr = expense.getFormattedDate();
                String categoryStr = escapeCSV(expense.getCategory());
                String descriptionStr = escapeCSV(expense.getDescription());
                String amountStr = String.format(Locale.US, "%.2f", expense.getAmount());

                writer.write(String.format(Locale.US, "%s,%s,%s,%s,%s", idStr, dateStr, categoryStr, descriptionStr, amountStr));
                writer.newLine();
            }

            // Category Breakdown Table
            writer.newLine();
            writer.write("# CATEGORY BREAKDOWN ROLLUP");
            writer.newLine();
            writer.write("Category,Transactions,Total Spent,Share (%)");
            writer.newLine();

            for (CategoryReportRow row : report.getCategoryRows()) {
                writer.write(String.format(Locale.US, "%s,%d,%.2f,%.1f%%",
                        escapeCSV(row.getCategory()),
                        row.getTransactionCount(),
                        row.getTotalAmountDouble(),
                        row.getPercentage()
                ));
                writer.newLine();
            }

            return true;
        }
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

        try (BufferedWriter writer = new BufferedWriter(
                new java.io.OutputStreamWriter(new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {

            writer.write("\uFEFF"); // UTF-8 Byte Order Mark
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