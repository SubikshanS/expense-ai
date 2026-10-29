package com.expensetracker.model;

import com.expensetracker.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Encapsulates a single aggregated point along a chronological time-series chart.
 */
public class TimeSeriesPoint {

    private final String label;
    private final LocalDate date;
    private final YearMonth yearMonth;
    private final BigDecimal amount;
    private final int transactionCount;

    public TimeSeriesPoint(String label, LocalDate date, YearMonth yearMonth, BigDecimal amount, int transactionCount) {
        this.label = label != null ? label : "";
        this.date = date;
        this.yearMonth = yearMonth;
        this.amount = amount != null ? amount : MoneyUtil.ZERO;
        this.transactionCount = Math.max(0, transactionCount);
    }

    public static TimeSeriesPoint ofDaily(LocalDate date, BigDecimal amount, int count) {
        String lbl = date != null ? date.format(DateTimeFormatter.ofPattern("d MMM")) : "";
        return new TimeSeriesPoint(lbl, date, null, amount, count);
    }

    public static TimeSeriesPoint ofMonthly(YearMonth yearMonth, BigDecimal amount, int count) {
        String lbl = yearMonth != null ? yearMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")) : "";
        return new TimeSeriesPoint(lbl, null, yearMonth, amount, count);
    }

    public String getLabel() {
        return label;
    }

    public LocalDate getDate() {
        return date;
    }

    public YearMonth getYearMonth() {
        return yearMonth;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public double getAmountDouble() {
        return amount.doubleValue();
    }

    public String getFormattedAmount() {
        return MoneyUtil.formatRupees(amount);
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public String getTooltipText() {
        if (date != null) {
            String fullDate = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"));
            return String.format("📅 %s\n💰 Total: %s\n📝 %d %s",
                    fullDate,
                    getFormattedAmount(),
                    transactionCount,
                    transactionCount == 1 ? "transaction" : "transactions");
        } else if (yearMonth != null) {
            String monthName = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            return String.format("📅 %s\n💰 Total: %s\n📝 %d %s",
                    monthName,
                    getFormattedAmount(),
                    transactionCount,
                    transactionCount == 1 ? "transaction" : "transactions");
        } else {
            return String.format("📅 %s\n💰 Total: %s", label, getFormattedAmount());
        }
    }
}
