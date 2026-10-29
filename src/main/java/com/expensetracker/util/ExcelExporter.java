package com.expensetracker.util;

import com.expensetracker.model.CategoryReportRow;
import com.expensetracker.model.Expense;
import com.expensetracker.model.FinancialReport;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to export Expense records to Microsoft Excel (.xlsx)
 * using Apache POI.
 *
 * Features:
 * - Styled header row
 * - Currency formatting
 * - Expense records
 * - Grand total
 * - Category-wise totals
 * - Summary metrics
 * - Auto-sized columns
 */
public final class ExcelExporter {

    private ExcelExporter() {
        // Utility class - prevent object creation
    }

    /**
     * Exports a financial report to an Excel (.xlsx) workbook with metadata, KPI cards,
     * category breakdown rollups, and transaction details.
     */
    public static boolean exportReportToExcel(File file, FinancialReport report) throws IOException {
        if (file == null || report == null) {
            return false;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Financial Statement");

            DataFormat dataFormat = workbook.createDataFormat();

            // Font & Styles
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            Font subFont = workbook.createFont();
            subFont.setItalic(true);
            subFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            CellStyle subStyle = workbook.createCellStyle();
            subStyle.setFont(subFont);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.BLACK.getIndex());
            headerFont.setFontHeightInPoints((short) 10);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(dataFormat.getFormat("[$₹-en-IN] #,##0.00"));
            currencyStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle boldCurrencyStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldCurrencyStyle.setFont(boldFont);
            boldCurrencyStyle.setDataFormat(dataFormat.getFormat("[$₹-en-IN] #,##0.00"));
            boldCurrencyStyle.setAlignment(HorizontalAlignment.RIGHT);

            int r = 0;

            // 1. Report Title & Info
            Row titleRow = sheet.createRow(r++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Expense AI — " + report.getReportTitle());
            titleCell.setCellStyle(titleStyle);

            Row rangeRow = sheet.createRow(r++);
            Cell rangeCell = rangeRow.createCell(0);
            rangeCell.setCellValue("Period / Date Range: " + report.getDateRangeDescription());
            rangeCell.setCellStyle(subStyle);

            Row genRow = sheet.createRow(r++);
            Cell genCell = genRow.createCell(0);
            genCell.setCellValue("Generated: " + report.getFormattedGeneratedAt());
            genCell.setCellStyle(subStyle);

            r++; // blank row

            // 2. Executive Summary KPIs Block
            Row kpiTitleRow = sheet.createRow(r++);
            Cell kpiTitleCell = kpiTitleRow.createCell(0);
            kpiTitleCell.setCellValue("EXECUTIVE SUMMARY METRICS");
            kpiTitleCell.setCellStyle(headerStyle);

            Row kpiRow1 = sheet.createRow(r++);
            kpiRow1.createCell(0).setCellValue("Total Spending:");
            Cell totalSpendCell = kpiRow1.createCell(1);
            totalSpendCell.setCellValue(report.getTotalSpendingDouble());
            totalSpendCell.setCellStyle(boldCurrencyStyle);

            kpiRow1.createCell(2).setCellValue("Total Transactions:");
            kpiRow1.createCell(3).setCellValue(report.getTransactionCount());

            Row kpiRow2 = sheet.createRow(r++);
            kpiRow2.createCell(0).setCellValue("Average Expense:");
            Cell avgCell = kpiRow2.createCell(1);
            avgCell.setCellValue(report.getAverageExpense().doubleValue());
            avgCell.setCellStyle(currencyStyle);

            kpiRow2.createCell(2).setCellValue("Top Category:");
            kpiRow2.createCell(3).setCellValue(report.getFormattedTopCategorySummary());

            Row kpiRow3 = sheet.createRow(r++);
            kpiRow3.createCell(0).setCellValue("Peak Spending Day:");
            kpiRow3.createCell(1).setCellValue(report.getFormattedPeakSpendingSummary());

            r += 2; // blank rows

            // 3. Category Breakdown Rollup
            Row catHeaderRow = sheet.createRow(r++);
            catHeaderRow.createCell(0).setCellValue("Category");
            catHeaderRow.createCell(1).setCellValue("Transactions");
            catHeaderRow.createCell(2).setCellValue("Total Spent (₹)");
            catHeaderRow.createCell(3).setCellValue("Share (%)");
            for (int c = 0; c < 4; c++) {
                catHeaderRow.getCell(c).setCellStyle(headerStyle);
            }

            for (CategoryReportRow catRow : report.getCategoryRows()) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(catRow.getCategory());

                Cell countCell = row.createCell(1);
                countCell.setCellValue(catRow.getTransactionCount());
                countCell.setCellStyle(centerStyle);

                Cell amtCell = row.createCell(2);
                amtCell.setCellValue(catRow.getTotalAmountDouble());
                amtCell.setCellStyle(currencyStyle);

                Cell pctCell = row.createCell(3);
                pctCell.setCellValue(String.format("%.1f%%", catRow.getPercentage()));
                pctCell.setCellStyle(centerStyle);
            }

            r += 2; // blank rows

            // 4. Detailed Transaction Ledger
            Row ledgerTitleRow = sheet.createRow(r++);
            ledgerTitleRow.createCell(0).setCellValue("TRANSACTION LEDGER DETAILS");
            ledgerTitleRow.getCell(0).setCellStyle(titleStyle);

            Row txHeaderRow = sheet.createRow(r++);
            String[] headers = {"ID", "Date", "Category", "Description", "Amount (₹)"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = txHeaderRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (Expense exp : report.getExpenses()) {
                if (exp == null) continue;
                Row row = sheet.createRow(r++);

                Cell idCell = row.createCell(0);
                idCell.setCellValue(exp.getId() != null ? exp.getId() : 0);
                idCell.setCellStyle(centerStyle);

                Cell dateCell = row.createCell(1);
                dateCell.setCellValue(exp.getFormattedDate());
                dateCell.setCellStyle(centerStyle);

                row.createCell(2).setCellValue(exp.getCategory());
                row.createCell(3).setCellValue(exp.getDescription());

                Cell amountCell = row.createCell(4);
                amountCell.setCellValue(exp.getAmount());
                amountCell.setCellStyle(currencyStyle);
            }

            // Auto-size columns
            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                workbook.write(outputStream);
            }
            return true;
        }
    }

    /**
     * Exports a list of expenses to an Excel (.xlsx) workbook.
     *
     * @param file target Excel file
     * @param expenseList list of Expense records
     * @return true if export succeeds
     * @throws IOException if file writing fails
     */
    public static boolean exportToExcel(
            File file,
            List<Expense> expenseList) throws IOException {

        if (file == null || expenseList == null) {
            return false;
        }

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Expenses Report");

            // -------------------------------------------------
            // Header Style
            // -------------------------------------------------

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(
                    IndexedColors.BLACK.getIndex()
            );
            headerFont.setFontHeightInPoints((short) 11);

            CellStyle headerStyle =
                    workbook.createCellStyle();

            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(
                    IndexedColors.GREY_25_PERCENT.getIndex()
            );
            headerStyle.setFillPattern(
                    FillPatternType.SOLID_FOREGROUND
            );

            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            headerStyle.setAlignment(
                    HorizontalAlignment.CENTER
            );

            // -------------------------------------------------
            // Currency Style
            // -------------------------------------------------

            CellStyle currencyStyle =
                    workbook.createCellStyle();

            DataFormat dataFormat =
                    workbook.createDataFormat();

            currencyStyle.setDataFormat(
                    dataFormat.getFormat("[$₹-en-IN] #,##0.00")
            );

            currencyStyle.setAlignment(
                    HorizontalAlignment.RIGHT
            );

            // -------------------------------------------------
            // Center Style
            // -------------------------------------------------

            CellStyle centerStyle =
                    workbook.createCellStyle();

            centerStyle.setAlignment(
                    HorizontalAlignment.CENTER
            );

            // -------------------------------------------------
            // Header Row
            // -------------------------------------------------

            String[] headers = {
                    "ID",
                    "Date",
                    "Category",
                    "Description",
                    "Amount (₹)"
            };

            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {

                Cell cell = headerRow.createCell(i);

                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // -------------------------------------------------
            // Expense Data
            // -------------------------------------------------

            int rowIndex = 1;

            double grandTotal = 0.0;

            Map<String, Double> categoryTotals =
                    new HashMap<>();

            for (Expense expense : expenseList) {

                if (expense == null) {
                    continue;
                }

                Row row = sheet.createRow(rowIndex++);

                // ID
                Cell idCell = row.createCell(0);

                idCell.setCellValue(
                        expense.getId() != null
                                ? expense.getId()
                                : 0
                );

                idCell.setCellStyle(centerStyle);

                // Date
                Cell dateCell = row.createCell(1);

                dateCell.setCellValue(
                        expense.getFormattedDate()
                );

                dateCell.setCellStyle(centerStyle);

                // Category
                Cell categoryCell = row.createCell(2);

                categoryCell.setCellValue(
                        expense.getCategory()
                );

                // Description
                Cell descriptionCell = row.createCell(3);

                descriptionCell.setCellValue(
                        expense.getDescription()
                );

                // Amount
                Cell amountCell = row.createCell(4);

                amountCell.setCellValue(
                        expense.getAmount()
                );

                amountCell.setCellStyle(
                        currencyStyle
                );

                // Grand total
                grandTotal += expense.getAmount();

                // Category aggregation
                categoryTotals.put(
                        expense.getCategory(),
                        categoryTotals.getOrDefault(
                                expense.getCategory(),
                                0.0
                        ) + expense.getAmount()
                );
            }

            // -------------------------------------------------
            // Summary Section
            // -------------------------------------------------

            rowIndex++;

            Row summaryTitleRow =
                    sheet.createRow(rowIndex++);

            Cell summaryTitleCell =
                    summaryTitleRow.createCell(0);

            summaryTitleCell.setCellValue(
                    "SUMMARY METRICS"
            );

            summaryTitleCell.setCellStyle(
                    headerStyle
            );

            // Total Transactions
            Row countRow =
                    sheet.createRow(rowIndex++);

            countRow.createCell(0)
                    .setCellValue("Total Transactions:");

            countRow.createCell(1)
                    .setCellValue(
                            expenseList.size()
                    );

            // Grand Total
            Row totalRow =
                    sheet.createRow(rowIndex++);

            totalRow.createCell(0)
                    .setCellValue(
                            "Grand Total Spending:"
                    );

            Cell totalCell =
                    totalRow.createCell(1);

            totalCell.setCellValue(grandTotal);
            totalCell.setCellStyle(currencyStyle);

            // -------------------------------------------------
            // Category Breakdown
            // -------------------------------------------------

            rowIndex++;

            Row categoryHeaderRow =
                    sheet.createRow(rowIndex++);

            categoryHeaderRow.createCell(0)
                    .setCellValue("Category");

            categoryHeaderRow.createCell(1)
                    .setCellValue("Total Spent (₹)");

            categoryHeaderRow.getCell(0)
                    .setCellStyle(headerStyle);

            categoryHeaderRow.getCell(1)
                    .setCellStyle(headerStyle);

            for (Map.Entry<String, Double> entry :
                    categoryTotals.entrySet()) {

                Row categoryRow =
                        sheet.createRow(rowIndex++);

                categoryRow.createCell(0)
                        .setCellValue(entry.getKey());

                Cell categoryAmountCell =
                        categoryRow.createCell(1);

                categoryAmountCell.setCellValue(
                        entry.getValue()
                );

                categoryAmountCell.setCellStyle(
                        currencyStyle
                );
            }

            // -------------------------------------------------
            // Auto-size Columns
            // -------------------------------------------------

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // -------------------------------------------------
            // Write Workbook
            // -------------------------------------------------

            try (FileOutputStream outputStream =
                         new FileOutputStream(file)) {

                workbook.write(outputStream);
            }

            return true;
        }
    }
}