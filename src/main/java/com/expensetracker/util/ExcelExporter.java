package com.expensetracker.util;

import com.expensetracker.model.Expense;
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
                    dataFormat.getFormat("₹#,##0.00")
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