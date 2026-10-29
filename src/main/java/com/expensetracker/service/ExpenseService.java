package com.expensetracker.service;

import com.expensetracker.dao.ExpenseDAO;
import com.expensetracker.exception.DatabaseOperationException;
import com.expensetracker.exception.ValidationException;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseSummary;
import com.expensetracker.model.FinancialIntelligenceResult;
import com.expensetracker.model.SpendingAnomaly;
import com.expensetracker.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.expensetracker.model.CategoryReportRow;
import com.expensetracker.model.CategorySpendingPoint;
import com.expensetracker.model.FinancialReport;
import com.expensetracker.model.MoMComparisonResult;
import com.expensetracker.model.ReportFilter;
import com.expensetracker.model.TimeRange;
import com.expensetracker.model.TimeSeriesPoint;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

/**
 * Service layer coordinating expense persistence, business validation,
 * aggregation calculations, and financial analytics.
 * Keeps business logic and database access isolated from JavaFX controllers.
 */
public class ExpenseService {

    private final ExpenseDAO expenseDAO;
    private final ExpenseCategorizer categorizer;
    private final FinancialIntelligenceService financialIntelligenceService;
    private final AnalyticsService analyticsService;

    public ExpenseService() {
        this(new ExpenseDAO(), new AICategorizer(), new FinancialIntelligenceService(), new AnalyticsService());
    }

    public ExpenseService(ExpenseDAO expenseDAO, ExpenseCategorizer categorizer) {
        this(expenseDAO, categorizer, new FinancialIntelligenceService(), new AnalyticsService());
    }

    public ExpenseService(ExpenseDAO expenseDAO, ExpenseCategorizer categorizer, FinancialIntelligenceService financialIntelligenceService) {
        this(expenseDAO, categorizer, financialIntelligenceService, new AnalyticsService());
    }

    public ExpenseService(ExpenseDAO expenseDAO, ExpenseCategorizer categorizer, FinancialIntelligenceService financialIntelligenceService, AnalyticsService analyticsService) {
        this.expenseDAO = expenseDAO;
        this.categorizer = categorizer;
        this.financialIntelligenceService = financialIntelligenceService;
        this.analyticsService = analyticsService != null ? analyticsService : new AnalyticsService();
    }

    /**
     * Returns the analytics service for time-series and comparative metrics.
     */
    public AnalyticsService getAnalyticsService() {
        return analyticsService;
    }

    /**
     * Aggregates expenses chronologically for the selected time range.
     */
    public List<TimeSeriesPoint> getSpendingOverTime(List<Expense> expenses, TimeRange range) {
        return analyticsService.calculateSpendingOverTime(expenses, range, LocalDate.now());
    }

    /**
     * Calculates ranked category spending sorted descending by total amount.
     */
    public List<CategorySpendingPoint> getCategorySpendingRanked(List<Expense> expenses) {
        return analyticsService.calculateCategorySpending(expenses);
    }

    /**
     * Calculates fair Month-over-Month comparison with optional Day-Normalized MTD.
     */
    public MoMComparisonResult getMoMComparison(List<Expense> expenses, boolean normalizeMtd) {
        return analyticsService.calculateMoMComparison(expenses, normalizeMtd, LocalDate.now());
    }

    /**
     * Executes comprehensive financial intelligence analysis on the provided expenses.
     */
    public com.expensetracker.model.FinancialIntelligenceResult getFinancialIntelligence(List<Expense> expenses) {
        return financialIntelligenceService.analyze(expenses);
    }

    /**
     * Retrieves all recorded expenses ordered chronologically descending.
     */
    public List<Expense> getAllExpenses() {
        try {
            return expenseDAO.getAllExpenses();
        } catch (Exception e) {
            throw new DatabaseOperationException("Failed to retrieve expenses from database: " + e.getMessage(), e);
        }
    }

    /**
     * Validates and inserts a new expense.
     *
     * @return Saved expense entity containing the newly generated database ID
     */
    public Expense addExpense(double amount, String category, String description, LocalDate date) {
        Expense expense;
        try {
            expense = new Expense(amount, category, description, date);
            expense.validate();
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage(), e);
        }

        int generatedId = expenseDAO.insert(expense);
        if (generatedId == -1) {
            throw new DatabaseOperationException("Failed to persist expense to database. Please check database connectivity.");
        }
        expense.setId(generatedId);
        return expense;
    }

    /**
     * Validates and updates an existing expense record.
     */
    public void updateExpense(Expense expense) {
        if (expense == null || expense.getId() == null) {
            throw new ValidationException("Cannot update an expense without a valid database ID.");
        }
        try {
            expense.validate();
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage(), e);
        }

        boolean updated = expenseDAO.update(expense);
        if (!updated) {
            throw new DatabaseOperationException("Failed to update expense ID " + expense.getId() + " in database.");
        }
    }

    /**
     * Deletes an expense by primary key.
     */
    public void deleteExpense(int id) {
        if (id <= 0) {
            throw new ValidationException("Invalid expense ID for deletion: " + id);
        }
        boolean deleted = expenseDAO.delete(id);
        if (!deleted) {
            throw new DatabaseOperationException("Failed to delete expense record ID: " + id);
        }
    }

    /**
     * Calculates summary metrics for the given expense collection using BigDecimal precision.
     */
    public ExpenseSummary calculateSummary(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return ExpenseSummary.empty();
        }

        BigDecimal total = BigDecimal.ZERO;
        Map<String, BigDecimal> breakdown = new HashMap<>();

        for (Expense expense : expenses) {
            if (expense == null) continue;
            BigDecimal amt = expense.getAmountBigDecimal();
            total = total.add(amt);

            String cat = expense.getCategory() != null ? expense.getCategory() : "Uncategorized";
            breakdown.put(cat, breakdown.getOrDefault(cat, BigDecimal.ZERO).add(amt));
        }

        int count = expenses.size();
        BigDecimal avg = count > 0
                ? total.divide(BigDecimal.valueOf(count), MoneyUtil.SCALE, RoundingMode.HALF_UP)
                : MoneyUtil.ZERO;

        String topCat = "None";
        BigDecimal topAmt = MoneyUtil.ZERO;
        for (Map.Entry<String, BigDecimal> entry : breakdown.entrySet()) {
            if (entry.getValue().compareTo(topAmt) > 0) {
                topAmt = entry.getValue();
                topCat = entry.getKey();
            }
        }

        return new ExpenseSummary(
                total.setScale(MoneyUtil.SCALE, RoundingMode.HALF_UP),
                count,
                avg,
                topCat,
                topAmt.setScale(MoneyUtil.SCALE, RoundingMode.HALF_UP),
                breakdown
        );
    }

    /**
     * Calculates monthly spending totals sorted chronologically.
     */
    public Map<YearMonth, BigDecimal> calculateMonthlySpending(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<YearMonth, BigDecimal> monthly = new TreeMap<>();
        for (Expense expense : expenses) {
            if (expense == null || expense.getDate() == null) continue;
            YearMonth ym = YearMonth.from(expense.getDate());
            BigDecimal current = monthly.getOrDefault(ym, BigDecimal.ZERO);
            monthly.put(ym, current.add(expense.getAmountBigDecimal()));
        }
        return monthly;
    }

    /**
     * Returns the active categorizer service.
     */
    public ExpenseCategorizer getCategorizer() {
        return categorizer;
    }

    /**
     * Generates a comprehensive financial report based on the provided filter parameters.
     */
    public FinancialReport generateReport(List<Expense> expenses, ReportFilter filter) {
        return generateReport(expenses, filter, LocalDate.now());
    }

    /**
     * Generates a comprehensive financial report evaluated relative to an anchor date.
     */
    public FinancialReport generateReport(List<Expense> expenses, ReportFilter filter, LocalDate anchorDate) {
        if (filter == null) {
            filter = ReportFilter.ofPreset(ReportFilter.PeriodPreset.THIS_MONTH);
        }

        LocalDate today = anchorDate != null ? anchorDate : LocalDate.now();

        // If expenses exist and anchorDate is today, but today is far from data, anchor to latest expense
        if (anchorDate == null && expenses != null && !expenses.isEmpty()) {
            LocalDate maxDate = expenses.stream().map(Expense::getDate).filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(today);
            if (maxDate.isAfter(today) || maxDate.isBefore(today.minusDays(60))) {
                today = maxDate;
            }
        }

        // Determine date range bounds
        LocalDate startDate = null;
        LocalDate endDate = null;
        String rangeDesc;

        switch (filter.getPreset()) {
            case THIS_MONTH -> {
                startDate = today.withDayOfMonth(1);
                endDate = today.withDayOfMonth(today.lengthOfMonth());
                rangeDesc = today.format(DateTimeFormatter.ofPattern("MMMM yyyy")) + " (" + startDate + " to " + endDate + ")";
            }
            case LAST_MONTH -> {
                LocalDate lastMonthDay = today.minusMonths(1);
                startDate = lastMonthDay.withDayOfMonth(1);
                endDate = lastMonthDay.withDayOfMonth(lastMonthDay.lengthOfMonth());
                rangeDesc = lastMonthDay.format(DateTimeFormatter.ofPattern("MMMM yyyy")) + " (" + startDate + " to " + endDate + ")";
            }
            case LAST_30_DAYS -> {
                startDate = today.minusDays(29);
                endDate = today;
                rangeDesc = "Last 30 Days (" + startDate + " to " + endDate + ")";
            }
            case LAST_90_DAYS -> {
                startDate = today.minusDays(89);
                endDate = today;
                rangeDesc = "Last 90 Days (" + startDate + " to " + endDate + ")";
            }
            case THIS_YEAR -> {
                startDate = LocalDate.of(today.getYear(), 1, 1);
                endDate = LocalDate.of(today.getYear(), 12, 31);
                rangeDesc = "Year " + today.getYear() + " YTD (" + startDate + " to " + endDate + ")";
            }
            case CUSTOM -> {
                startDate = filter.getCustomStartDate();
                endDate = filter.getCustomEndDate();
                if (startDate != null && endDate != null) {
                    rangeDesc = "Custom Period (" + startDate + " to " + endDate + ")";
                } else if (startDate != null) {
                    rangeDesc = "From " + startDate + " onward";
                } else if (endDate != null) {
                    rangeDesc = "Up to " + endDate;
                } else {
                    rangeDesc = "Custom Range (All Available Dates)";
                }
            }
            case ALL_TIME -> {
                rangeDesc = "All Time Historical Ledger";
            }
            default -> rangeDesc = "Financial Report";
        }

        final LocalDate finalStart = startDate;
        final LocalDate finalEnd = endDate;
        final boolean allCategories = filter.isAllCategories();
        final String targetCategory = filter.getCategoryFilter();

        List<Expense> filtered = new ArrayList<>();
        if (expenses != null) {
            for (Expense exp : expenses) {
                if (exp == null || exp.getDate() == null) continue;
                if (finalStart != null && exp.getDate().isBefore(finalStart)) continue;
                if (finalEnd != null && exp.getDate().isAfter(finalEnd)) continue;
                if (!allCategories && (exp.getCategory() == null || !exp.getCategory().equalsIgnoreCase(targetCategory))) continue;
                filtered.add(exp);
            }
        }

        // Sort filtered expenses: date desc, id desc
        filtered.sort(Comparator.comparing(Expense::getDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Expense::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        String reportTitle = filter.getPreset().getDisplayName().replaceAll("[^a-zA-Z0-9 ()-]", "").trim() + " Statement";
        if (!allCategories) {
            reportTitle += " [" + targetCategory + "]";
        }

        if (filtered.isEmpty()) {
            return FinancialReport.empty(reportTitle, rangeDesc);
        }

        BigDecimal grandTotal = MoneyUtil.ZERO;
        Map<String, BigDecimal> catTotals = new HashMap<>();
        Map<String, Integer> catCounts = new HashMap<>();
        Map<LocalDate, BigDecimal> dayTotals = new HashMap<>();

        for (Expense exp : filtered) {
            BigDecimal amt = exp.getAmountBigDecimal();
            grandTotal = grandTotal.add(amt);

            String cat = exp.getCategory() != null ? exp.getCategory() : "Uncategorized";
            catTotals.put(cat, catTotals.getOrDefault(cat, MoneyUtil.ZERO).add(amt));
            catCounts.put(cat, catCounts.getOrDefault(cat, 0) + 1);

            dayTotals.put(exp.getDate(), dayTotals.getOrDefault(exp.getDate(), MoneyUtil.ZERO).add(amt));
        }

        int count = filtered.size();
        BigDecimal avg = grandTotal.divide(BigDecimal.valueOf(count), MoneyUtil.SCALE, RoundingMode.HALF_UP);

        // Find top category
        String topCat = "None";
        BigDecimal topCatAmt = MoneyUtil.ZERO;
        for (Map.Entry<String, BigDecimal> e : catTotals.entrySet()) {
            if (e.getValue().compareTo(topCatAmt) > 0) {
                topCatAmt = e.getValue();
                topCat = e.getKey();
            }
        }

        // Find peak day
        String peakDayStr = "None";
        BigDecimal peakDayAmt = MoneyUtil.ZERO;
        for (Map.Entry<LocalDate, BigDecimal> e : dayTotals.entrySet()) {
            if (e.getValue().compareTo(peakDayAmt) > 0) {
                peakDayAmt = e.getValue();
                peakDayStr = e.getKey().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            }
        }

        // Build category breakdown rows sorted by total amount descending
        List<CategoryReportRow> categoryRows = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : catTotals.entrySet()) {
            String cat = e.getKey();
            BigDecimal cAmt = e.getValue();
            int cCount = catCounts.getOrDefault(cat, 0);
            double pct = grandTotal.compareTo(BigDecimal.ZERO) > 0
                    ? cAmt.multiply(new BigDecimal("100")).divide(grandTotal, 2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            categoryRows.add(new CategoryReportRow(cat, cCount, cAmt, pct));
        }
        categoryRows.sort(Comparator.comparing(CategoryReportRow::getTotalAmount).reversed());

        return new FinancialReport(
                reportTitle,
                rangeDesc,
                LocalDateTime.now(),
                grandTotal,
                count,
                avg,
                topCat,
                topCatAmt,
                peakDayStr,
                peakDayAmt,
                filtered,
                categoryRows
        );
    }

    // =========================================================================
    // Phase 4 Architecture Preparation: Anomaly & Forecast Contracts
    // =========================================================================

    /**
     * Structure hook for Phase 4: Detects potential spending anomalies
     * where an individual expense significantly deviates from the category average.
     */
    public List<Expense> detectPotentialAnomalies(List<Expense> expenses) {
        List<SpendingAnomaly> anomalies = detectSpendingAnomalies(expenses);
        List<Expense> list = new ArrayList<>();
        for (SpendingAnomaly a : anomalies) {
            list.addAll(a.getSupportingTransactions());
        }
        return list;
    }

    /**
     * Identifies statistically unusual expenses and periodic surges backed by supporting records.
     */
    public List<SpendingAnomaly> detectSpendingAnomalies(List<Expense> expenses) {
        return financialIntelligenceService.detectAnomalies(expenses, calculateMonthlySpending(expenses));
    }

    /**
     * Structure hook for Phase 4: Calculates a simple run-rate month-end forecast.
     */
    public BigDecimal calculateSimpleMonthEndForecast(List<Expense> currentMonthExpenses, YearMonth month) {
        if (currentMonthExpenses == null || currentMonthExpenses.isEmpty()) {
            return MoneyUtil.ZERO;
        }
        LocalDate now = LocalDate.now();
        int dayOfMonth = Math.min(now.getDayOfMonth(), month.lengthOfMonth());
        if (dayOfMonth <= 0) return MoneyUtil.ZERO;

        BigDecimal monthTotal = calculateSummary(currentMonthExpenses).getTotalSpending();
        BigDecimal dailyRate = monthTotal.divide(BigDecimal.valueOf(dayOfMonth), 4, RoundingMode.HALF_UP);
        return dailyRate.multiply(BigDecimal.valueOf(month.lengthOfMonth())).setScale(MoneyUtil.SCALE, RoundingMode.HALF_UP);
    }
}
