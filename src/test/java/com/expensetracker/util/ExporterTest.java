package com.expensetracker.util;

import com.expensetracker.model.Expense;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void testExportToCSV() throws IOException {
        File csvFile = tempDir.resolve("test_expenses.csv").toFile();
        List<Expense> expenses = Arrays.asList(
                new Expense(1, 150.50, "Transport", "Uber ride", LocalDate.of(2026, 10, 1)),
                new Expense(2, 85.00, "Food & Dining", "Coffee & Snack", LocalDate.of(2026, 10, 2))
        );

        boolean result = CSVExporter.exportToCSV(csvFile, expenses);
        assertTrue(result);
        assertTrue(csvFile.exists());

        String content = Files.readString(csvFile.toPath());
        assertTrue(content.contains("ID,Date,Category,Description,Amount"));
        assertTrue(content.contains("Uber ride"));
        assertTrue(content.contains("Coffee & Snack"));
        assertTrue(content.contains("# Grand Total Spending: ₹235.50"));
    }

    @Test
    void testExportToExcel() throws IOException {
        File excelFile = tempDir.resolve("test_expenses.xlsx").toFile();
        List<Expense> expenses = Arrays.asList(
                new Expense(1, 150.50, "Transport", "Uber ride", LocalDate.of(2026, 10, 1)),
                new Expense(2, 85.00, "Food & Dining", "Coffee & Snack", LocalDate.of(2026, 10, 2))
        );

        boolean result = ExcelExporter.exportToExcel(excelFile, expenses);
        assertTrue(result);
        assertTrue(excelFile.exists());
        assertTrue(excelFile.length() > 0);
    }
}
