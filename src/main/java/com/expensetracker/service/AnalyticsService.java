package com.expensetracker.service;

import com.expensetracker.model.CategoryMoMPoint;
import com.expensetracker.model.CategorySpendingPoint;
import com.expensetracker.model.Expense;
import com.expensetracker.model.MoMComparisonResult;
import com.expensetracker.model.TimeRange;
import com.expensetracker.model.TimeSeriesPoint;
import com.expensetracker.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Service dedicated to calculating rich financial analytics, time-series projections,
 * category distributions, and fair Month-over-Month normalized comparisons.
 */
public class AnalyticsService {

    /**
     * Aggregates expenses chronologically into time-series points based on the selected range.
     * Guarantees continuous date sequences with zero-filled gaps for inactive days/months.
     */
    public List<TimeSeriesPoint> calculateSpendingOverTime(List<Expense> expenses, TimeRange range, LocalDate anchorDate) {
        if (range == null) {
            range = TimeRange.LAST_30_DAYS;
        }

        List<Expense> valid = expenses != null
                ? expenses.stream().filter(e -> e != null && e.getDate() != null && e.getAmount() > 0).toList()
                : Collections.emptyList();

        LocalDate anchor = resolveAnchorDate(valid, anchorDate);

        return switch (range) {
            case LAST_7_DAYS -> calculateDailyRange(valid, anchor.minusDays(6), anchor);
            case LAST_30_DAYS -> calculateDailyRange(valid, anchor.minusDays(29), anchor);
            case LAST_6_MONTHS -> calculateMonthlyRange(valid, YearMonth.from(anchor).minusMonths(5), YearMonth.from(anchor));
            case LAST_12_MONTHS -> calculateMonthlyRange(valid, YearMonth.from(anchor).minusMonths(11), YearMonth.from(anchor));
            case ALL_TIME -> calculateAllTimeRange(valid, anchor);
        };
    }

    private List<TimeSeriesPoint> calculateDailyRange(List<Expense> expenses, LocalDate start, LocalDate end) {
        Map<LocalDate, BigDecimal> sumMap = new HashMap<>();
        Map<LocalDate, Integer> countMap = new HashMap<>();

        for (Expense exp : expenses) {
            LocalDate d = exp.getDate();
            if (d != null && !d.isBefore(start) && !d.isAfter(end)) {
                sumMap.put(d, sumMap.getOrDefault(d, MoneyUtil.ZERO).add(exp.getAmountBigDecimal()));
                countMap.put(d, countMap.getOrDefault(d, 0) + 1);
            }
        }

        List<TimeSeriesPoint> points = new ArrayList<>();
        LocalDate curr = start;
        while (!curr.isAfter(end)) {
            BigDecimal amt = sumMap.getOrDefault(curr, MoneyUtil.ZERO);
            int count = countMap.getOrDefault(curr, 0);
            points.add(TimeSeriesPoint.ofDaily(curr, amt, count));
            curr = curr.plusDays(1);
        }
        return points;
    }

    private List<TimeSeriesPoint> calculateMonthlyRange(List<Expense> expenses, YearMonth start, YearMonth end) {
        Map<YearMonth, BigDecimal> sumMap = new HashMap<>();
        Map<YearMonth, Integer> countMap = new HashMap<>();

        for (Expense exp : expenses) {
            if (exp.getDate() == null) continue;
            YearMonth ym = YearMonth.from(exp.getDate());
            if (!ym.isBefore(start) && !ym.isAfter(end)) {
                sumMap.put(ym, sumMap.getOrDefault(ym, MoneyUtil.ZERO).add(exp.getAmountBigDecimal()));
                countMap.put(ym, countMap.getOrDefault(ym, 0) + 1);
            }
        }

        List<TimeSeriesPoint> points = new ArrayList<>();
        YearMonth curr = start;
        while (!curr.isAfter(end)) {
            BigDecimal amt = sumMap.getOrDefault(curr, MoneyUtil.ZERO);
            int count = countMap.getOrDefault(curr, 0);
            points.add(TimeSeriesPoint.ofMonthly(curr, amt, count));
            curr = curr.plusMonths(1);
        }
        return points;
    }

    private List<TimeSeriesPoint> calculateAllTimeRange(List<Expense> expenses, LocalDate anchor) {
        if (expenses.isEmpty()) {
            return calculateMonthlyRange(expenses, YearMonth.from(anchor).minusMonths(5), YearMonth.from(anchor));
        }

        LocalDate minDate = expenses.stream().map(Expense::getDate).filter(Objects::nonNull).min(LocalDate::compareTo).orElse(anchor);
        LocalDate maxDate = expenses.stream().map(Expense::getDate).filter(Objects::nonNull).max(LocalDate::compareTo).orElse(anchor);

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(minDate, maxDate);
        if (daysBetween <= 45) {
            return calculateDailyRange(expenses, minDate, maxDate);
        } else {
            YearMonth startYm = YearMonth.from(minDate);
            YearMonth endYm = YearMonth.from(maxDate);
            return calculateMonthlyRange(expenses, startYm, endYm);
        }
    }

    /**
     * Calculates ranked category spending sorted strictly descending by spending amount.
     */
    public List<CategorySpendingPoint> calculateCategorySpending(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, BigDecimal> sumMap = new HashMap<>();
        Map<String, Integer> countMap = new HashMap<>();
        BigDecimal grandTotal = MoneyUtil.ZERO;

        for (Expense exp : expenses) {
            if (exp == null || exp.getAmount() <= 0) continue;
            String cat = exp.getCategory() != null && !exp.getCategory().isBlank() ? exp.getCategory() : "Uncategorized";
            BigDecimal amt = exp.getAmountBigDecimal();

            sumMap.put(cat, sumMap.getOrDefault(cat, MoneyUtil.ZERO).add(amt));
            countMap.put(cat, countMap.getOrDefault(cat, 0) + 1);
            grandTotal = grandTotal.add(amt);
        }

        if (grandTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.emptyList();
        }

        List<CategorySpendingPoint> points = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : sumMap.entrySet()) {
            String cat = entry.getKey();
            BigDecimal amt = entry.getValue();
            int count = countMap.getOrDefault(cat, 0);

            double pct = amt.multiply(new BigDecimal("100"))
                    .divide(grandTotal, 2, RoundingMode.HALF_UP)
                    .doubleValue();

            points.add(new CategorySpendingPoint(cat, amt, pct, count));
        }

        // Sort descending: highest spending category first
        points.sort(Comparator.comparing(CategorySpendingPoint::getAmount).reversed());
        return points;
    }

    /**
     * Compares category expenditure between the current month and prior month.
     * When normalizeMtd is true and current month is ongoing, filters prior month to the
     * identical day-of-month window to guarantee fair, unbiased comparison.
     */
    public MoMComparisonResult calculateMoMComparison(List<Expense> expenses, boolean normalizeMtd, LocalDate anchorDate) {
        if (expenses == null || expenses.isEmpty()) {
            return MoMComparisonResult.insufficient("No transaction history available to perform Month-over-Month comparison.");
        }

        List<Expense> valid = expenses.stream()
                .filter(e -> e != null && e.getDate() != null && e.getAmount() > 0)
                .toList();

        if (valid.isEmpty()) {
            return MoMComparisonResult.insufficient("No valid expense transactions available.");
        }

        LocalDate anchor = resolveAnchorDate(valid, anchorDate);
        YearMonth currentYm = resolveCurrentMonth(valid, anchor);
        YearMonth previousYm = resolvePreviousMonth(valid, currentYm);

        if (previousYm == null) {
            return MoMComparisonResult.insufficient("At least 2 distinct calendar months are required for Month-over-Month comparison.");
        }

        // Determine MTD normalization window
        int maxDaysInCurMonth = currentYm.lengthOfMonth();
        int curDays = maxDaysInCurMonth;
        boolean isCurrentMonthOngoing = (anchor.getYear() == currentYm.getYear() && anchor.getMonth() == currentYm.getMonth());
        if (isCurrentMonthOngoing && anchor.getDayOfMonth() < maxDaysInCurMonth) {
            curDays = anchor.getDayOfMonth();
        }

        int prevDays = previousYm.lengthOfMonth();
        boolean doNormalize = normalizeMtd && isCurrentMonthOngoing && curDays < maxDaysInCurMonth;
        if (doNormalize) {
            prevDays = Math.min(curDays, previousYm.lengthOfMonth());
        }

        final int curCutoff = curDays;
        final int prevCutoff = prevDays;

        List<Expense> curExpenses = valid.stream()
                .filter(e -> YearMonth.from(e.getDate()).equals(currentYm))
                .filter(e -> !doNormalize || e.getDate().getDayOfMonth() <= curCutoff)
                .toList();

        List<Expense> prevExpenses = valid.stream()
                .filter(e -> YearMonth.from(e.getDate()).equals(previousYm))
                .filter(e -> !doNormalize || e.getDate().getDayOfMonth() <= prevCutoff)
                .toList();

        Map<String, BigDecimal> curCatSums = sumByCategory(curExpenses);
        Map<String, BigDecimal> prevCatSums = sumByCategory(prevExpenses);

        Set<String> allCats = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        allCats.addAll(curCatSums.keySet());
        allCats.addAll(prevCatSums.keySet());

        BigDecimal curTotal = curExpenses.stream().map(Expense::getAmountBigDecimal).reduce(MoneyUtil.ZERO, BigDecimal::add);
        BigDecimal prevTotal = prevExpenses.stream().map(Expense::getAmountBigDecimal).reduce(MoneyUtil.ZERO, BigDecimal::add);
        BigDecimal netDiff = curTotal.subtract(prevTotal);

        Double pctChange = null;
        if (prevTotal.compareTo(BigDecimal.ZERO) > 0) {
            pctChange = netDiff.multiply(new BigDecimal("100")).divide(prevTotal, 2, RoundingMode.HALF_UP).doubleValue();
        } else if (curTotal.compareTo(BigDecimal.ZERO) > 0) {
            pctChange = 100.0;
        }

        String curLabel = currentYm.format(DateTimeFormatter.ofPattern("MMM yyyy"));
        if (doNormalize) {
            curLabel += String.format(" (Day 1–%d)", curDays);
        }

        String prevLabel = previousYm.format(DateTimeFormatter.ofPattern("MMM yyyy"));
        if (doNormalize) {
            prevLabel += String.format(" (Day 1–%d)", prevDays);
        }

        List<CategoryMoMPoint> points = new ArrayList<>();
        for (String cat : allCats) {
            BigDecimal cAmt = curCatSums.getOrDefault(cat, MoneyUtil.ZERO);
            BigDecimal pAmt = prevCatSums.getOrDefault(cat, MoneyUtil.ZERO);
            BigDecimal diff = cAmt.subtract(pAmt);

            Double catPct = null;
            if (pAmt.compareTo(BigDecimal.ZERO) > 0) {
                catPct = diff.multiply(new BigDecimal("100")).divide(pAmt, 2, RoundingMode.HALF_UP).doubleValue();
            } else if (cAmt.compareTo(BigDecimal.ZERO) > 0) {
                catPct = 100.0;
            }

            points.add(new CategoryMoMPoint(cat, cAmt, pAmt, diff, catPct, curLabel, prevLabel));
        }

        // Sort points descending by maximum of current or previous amount so major categories are prominent
        points.sort(Comparator.comparing((CategoryMoMPoint p) -> p.getCurrentAmount().max(p.getPreviousAmount())).reversed());

        String statusMsg = String.format("Comparison successfully evaluated between %s and %s.", curLabel, prevLabel);

        return new MoMComparisonResult(
                currentYm,
                previousYm,
                curDays,
                prevDays,
                doNormalize,
                curTotal,
                prevTotal,
                netDiff,
                pctChange,
                points,
                true,
                statusMsg
        );
    }

    private Map<String, BigDecimal> sumByCategory(List<Expense> expenses) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (Expense exp : expenses) {
            String cat = exp.getCategory() != null && !exp.getCategory().isBlank() ? exp.getCategory() : "Uncategorized";
            map.put(cat, map.getOrDefault(cat, MoneyUtil.ZERO).add(exp.getAmountBigDecimal()));
        }
        return map;
    }

    private LocalDate resolveAnchorDate(List<Expense> expenses, LocalDate anchorDate) {
        if (anchorDate != null) {
            return anchorDate;
        }
        LocalDate anchor = LocalDate.now();
        if (expenses != null && !expenses.isEmpty()) {
            LocalDate maxDate = expenses.stream()
                    .map(Expense::getDate)
                    .filter(Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(anchor);

            if (maxDate.isAfter(anchor) || maxDate.isBefore(anchor.minusDays(30))) {
                anchor = maxDate;
            }
        }
        return anchor;
    }

    private YearMonth resolveCurrentMonth(List<Expense> expenses, LocalDate anchor) {
        YearMonth anchorYm = YearMonth.from(anchor);
        boolean anchorMonthHasData = expenses.stream().anyMatch(e -> YearMonth.from(e.getDate()).equals(anchorYm));
        if (anchorMonthHasData) {
            return anchorYm;
        }

        // Fall back to latest month in database
        return expenses.stream()
                .map(e -> YearMonth.from(e.getDate()))
                .max(YearMonth::compareTo)
                .orElse(anchorYm);
    }

    private YearMonth resolvePreviousMonth(List<Expense> expenses, YearMonth currentMonth) {
        YearMonth directPrior = currentMonth.minusMonths(1);
        boolean priorHasData = expenses.stream().anyMatch(e -> YearMonth.from(e.getDate()).equals(directPrior));
        if (priorHasData) {
            return directPrior;
        }

        // Otherwise find closest prior recorded month
        return expenses.stream()
                .map(e -> YearMonth.from(e.getDate()))
                .filter(ym -> ym.isBefore(currentMonth))
                .max(YearMonth::compareTo)
                .orElse(null);
    }
}
