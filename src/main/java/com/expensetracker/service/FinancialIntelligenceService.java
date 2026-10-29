package com.expensetracker.service;

import com.expensetracker.model.CategoryTrend;
import com.expensetracker.model.Expense;
import com.expensetracker.model.FinancialIntelligenceResult;
import com.expensetracker.model.MonthComparison;
import com.expensetracker.model.SpendingAnomaly;
import com.expensetracker.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Personal Financial Intelligence Engine.
 *
 * Implements:
 * 1. Historical spending aggregation across days, weeks, and months.
 * 2. Month-over-month comparative analysis and category growth attribution.
 * 3. Statistical spending anomaly detection using sample mean (μ) and standard deviation (σ)
 *    with robust safeguards for small datasets, zero variance, and minimum monetary difference.
 * 4. Transparent explanations with traceability to actual database records.
 */
public class FinancialIntelligenceService {

    // Statistical Anomaly Thresholds
    private static final int MIN_SAMPLE_SIZE_FOR_CATEGORY_STATISTICS = 3;
    private static final double Z_SCORE_THRESHOLD = 2.0; // 2 standard deviations (~97.7th percentile)
    private static final BigDecimal MIN_ANOMALY_ABSOLUTE_DIFFERENCE = new BigDecimal("150.00");
    private static final double MIN_ANOMALY_PERCENTAGE_SURGE = 1.50; // At least 50% above mean

    // Periodic Surge Thresholds
    private static final BigDecimal MIN_MONTHLY_SURGE_DIFFERENCE = new BigDecimal("500.00");
    private static final double MONTHLY_SURGE_RATIO = 1.60; // 60% higher than historical monthly average

    /**
     * Executes complete financial intelligence analysis over the given transactions.
     *
     * @param expenses List of recorded expenses
     * @return Comprehensive FinancialIntelligenceResult
     */
    public FinancialIntelligenceResult analyze(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return FinancialIntelligenceResult.empty();
        }

        // Filter out null or corrupted entries
        List<Expense> validExpenses = expenses.stream()
                .filter(e -> e != null && e.getDate() != null && e.getAmount() > 0)
                .toList();

        if (validExpenses.isEmpty()) {
            return FinancialIntelligenceResult.empty();
        }

        Map<LocalDate, BigDecimal> daily = calculateDailySpending(validExpenses);
        Map<String, BigDecimal> weekly = calculateWeeklySpending(validExpenses);
        Map<YearMonth, BigDecimal> monthly = calculateMonthlySpending(validExpenses);

        MonthComparison monthComp = analyzeMonthOverMonth(validExpenses, monthly);
        List<SpendingAnomaly> anomalies = detectAnomalies(validExpenses, monthly);

        BigDecimal avgDaily30 = calculateAverageDailySpendLast30Days(validExpenses);
        int activeDays = daily.size();

        String summary = generateExecutiveSummary(monthComp, anomalies, validExpenses.size());

        return new FinancialIntelligenceResult(
                monthComp,
                anomalies,
                daily,
                weekly,
                monthly,
                avgDaily30,
                activeDays,
                true,
                summary
        );
    }

    // =========================================================================
    // 1. Historical Spending Analysis (Day, Week, Month)
    // =========================================================================

    /**
     * Aggregates spending by individual calendar day.
     */
    public Map<LocalDate, BigDecimal> calculateDailySpending(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) return Collections.emptyMap();

        Map<LocalDate, BigDecimal> map = new TreeMap<>();
        for (Expense exp : expenses) {
            if (exp == null || exp.getDate() == null) continue;
            BigDecimal current = map.getOrDefault(exp.getDate(), MoneyUtil.ZERO);
            map.put(exp.getDate(), current.add(exp.getAmountBigDecimal()));
        }
        return map;
    }

    /**
     * Aggregates spending by ISO calendar week (e.g. "2026-W41").
     */
    public Map<String, BigDecimal> calculateWeeklySpending(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) return Collections.emptyMap();

        Map<String, BigDecimal> map = new TreeMap<>();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        for (Expense exp : expenses) {
            if (exp == null || exp.getDate() == null) continue;
            int year = exp.getDate().get(weekFields.weekBasedYear());
            int week = exp.getDate().get(weekFields.weekOfWeekBasedYear());
            String key = String.format("%04d-W%02d", year, week);

            BigDecimal current = map.getOrDefault(key, MoneyUtil.ZERO);
            map.put(key, current.add(exp.getAmountBigDecimal()));
        }
        return map;
    }

    /**
     * Aggregates spending chronologically by YearMonth.
     */
    public Map<YearMonth, BigDecimal> calculateMonthlySpending(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) return Collections.emptyMap();

        Map<YearMonth, BigDecimal> map = new TreeMap<>();
        for (Expense exp : expenses) {
            if (exp == null || exp.getDate() == null) continue;
            YearMonth ym = YearMonth.from(exp.getDate());
            BigDecimal current = map.getOrDefault(ym, MoneyUtil.ZERO);
            map.put(ym, current.add(exp.getAmountBigDecimal()));
        }
        return map;
    }

    /**
     * Calculates the average daily burn rate over the last 30 calendar days.
     */
    public BigDecimal calculateAverageDailySpendLast30Days(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) return MoneyUtil.ZERO;

        LocalDate now = LocalDate.now();
        LocalDate maxDate = expenses.stream().map(Expense::getDate).filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(now);
        LocalDate anchor = maxDate.isAfter(now) || maxDate.isBefore(now.minusDays(30)) ? maxDate : now;
        LocalDate cutoff = anchor.minusDays(30);

        BigDecimal sum = BigDecimal.ZERO;
        for (Expense exp : expenses) {
            if (exp != null && exp.getDate() != null && !exp.getDate().isBefore(cutoff) && !exp.getDate().isAfter(anchor)) {
                sum = sum.add(exp.getAmountBigDecimal());
            }
        }
        return sum.divide(BigDecimal.valueOf(30), MoneyUtil.SCALE, RoundingMode.HALF_UP);
    }

    // =========================================================================
    // 2. Month-over-Month Comparison & Growth Contributors
    // =========================================================================

    /**
     * Compares the most recent month with transactions against the preceding month.
     */
    public MonthComparison analyzeMonthOverMonth(List<Expense> expenses, Map<YearMonth, BigDecimal> monthlyTotals) {
        if (monthlyTotals == null || monthlyTotals.isEmpty()) {
            return MonthComparison.insufficient("No transaction data available.");
        }

        List<YearMonth> sortedMonths = new ArrayList<>(monthlyTotals.keySet());
        if (sortedMonths.size() < 2) {
            YearMonth singleMonth = sortedMonths.get(0);
            BigDecimal currentTotal = monthlyTotals.getOrDefault(singleMonth, MoneyUtil.ZERO);
            BigDecimal currentDailyAvg = currentTotal.divide(BigDecimal.valueOf(singleMonth.lengthOfMonth()), MoneyUtil.SCALE, RoundingMode.HALF_UP);

            // Group category spending for this single month
            Map<String, BigDecimal> catMap = new HashMap<>();
            Map<String, List<Expense>> catExpenses = new HashMap<>();
            for (Expense exp : expenses) {
                if (exp == null || exp.getDate() == null) continue;
                String cat = exp.getCategory() != null ? exp.getCategory() : "Uncategorized";
                catMap.put(cat, catMap.getOrDefault(cat, MoneyUtil.ZERO).add(exp.getAmountBigDecimal()));
                catExpenses.computeIfAbsent(cat, k -> new ArrayList<>()).add(exp);
            }

            List<CategoryTrend> trends = new ArrayList<>();
            for (Map.Entry<String, BigDecimal> entry : catMap.entrySet()) {
                String cat = entry.getKey();
                BigDecimal amt = entry.getValue();
                Double contrib = currentTotal.compareTo(BigDecimal.ZERO) > 0
                        ? amt.multiply(new BigDecimal("100")).divide(currentTotal, 2, RoundingMode.HALF_UP).doubleValue()
                        : 0.0;
                trends.add(new CategoryTrend(
                        cat, amt, MoneyUtil.ZERO, amt, null, contrib,
                        catExpenses.getOrDefault(cat, Collections.emptyList()),
                        Collections.emptyList()
                ));
            }
            trends.sort(Comparator.comparing(CategoryTrend::getCurrentAmount).reversed());

            String msg = String.format(
                    "Only 1 month of transactions (%s: %s) is currently recorded. At least 2 distinct months are required for comparative trend analysis.",
                    singleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    MoneyUtil.formatRupees(currentTotal)
            );

            return MonthComparison.singleMonth(singleMonth, currentTotal, currentDailyAvg, trends, msg);
        }

        YearMonth currentMonth = sortedMonths.get(sortedMonths.size() - 1);
        YearMonth previousMonth = sortedMonths.get(sortedMonths.size() - 2);

        BigDecimal currentTotal = monthlyTotals.getOrDefault(currentMonth, MoneyUtil.ZERO);
        BigDecimal previousTotal = monthlyTotals.getOrDefault(previousMonth, MoneyUtil.ZERO);
        BigDecimal netDifference = currentTotal.subtract(previousTotal);

        Double pctChange = null;
        if (previousTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pct = netDifference.multiply(new BigDecimal("100"))
                    .divide(previousTotal, 2, RoundingMode.HALF_UP);
            pctChange = pct.doubleValue();
        }

        // Daily average for each month based on calendar days in month
        BigDecimal currentDailyAvg = currentTotal.divide(BigDecimal.valueOf(currentMonth.lengthOfMonth()), MoneyUtil.SCALE, RoundingMode.HALF_UP);
        BigDecimal previousDailyAvg = previousTotal.divide(BigDecimal.valueOf(previousMonth.lengthOfMonth()), MoneyUtil.SCALE, RoundingMode.HALF_UP);

        // Group category spending for both months
        Map<String, BigDecimal> currentCatMap = new HashMap<>();
        Map<String, List<Expense>> currentCatExpenses = new HashMap<>();
        Map<String, BigDecimal> prevCatMap = new HashMap<>();
        Map<String, List<Expense>> prevCatExpenses = new HashMap<>();

        for (Expense exp : expenses) {
            if (exp == null || exp.getDate() == null) continue;
            YearMonth ym = YearMonth.from(exp.getDate());
            String cat = exp.getCategory() != null ? exp.getCategory() : "Uncategorized";

            if (ym.equals(currentMonth)) {
                currentCatMap.put(cat, currentCatMap.getOrDefault(cat, MoneyUtil.ZERO).add(exp.getAmountBigDecimal()));
                currentCatExpenses.computeIfAbsent(cat, k -> new ArrayList<>()).add(exp);
            } else if (ym.equals(previousMonth)) {
                prevCatMap.put(cat, prevCatMap.getOrDefault(cat, MoneyUtil.ZERO).add(exp.getAmountBigDecimal()));
                prevCatExpenses.computeIfAbsent(cat, k -> new ArrayList<>()).add(exp);
            }
        }

        Set<String> allCategories = new HashSet<>();
        allCategories.addAll(currentCatMap.keySet());
        allCategories.addAll(prevCatMap.keySet());

        List<CategoryTrend> allTrends = new ArrayList<>();
        List<CategoryTrend> increaseContributors = new ArrayList<>();

        for (String cat : allCategories) {
            BigDecimal cAmt = currentCatMap.getOrDefault(cat, MoneyUtil.ZERO);
            BigDecimal pAmt = prevCatMap.getOrDefault(cat, MoneyUtil.ZERO);
            BigDecimal diff = cAmt.subtract(pAmt);

            Double catPct = null;
            if (pAmt.compareTo(BigDecimal.ZERO) > 0) {
                catPct = diff.multiply(new BigDecimal("100")).divide(pAmt, 2, RoundingMode.HALF_UP).doubleValue();
            }

            Double contribution = null;
            if (netDifference.compareTo(BigDecimal.ZERO) > 0 && diff.compareTo(BigDecimal.ZERO) > 0) {
                contribution = diff.multiply(new BigDecimal("100")).divide(netDifference, 2, RoundingMode.HALF_UP).doubleValue();
            }

            CategoryTrend trend = new CategoryTrend(
                    cat,
                    cAmt,
                    pAmt,
                    diff,
                    catPct,
                    contribution,
                    currentCatExpenses.getOrDefault(cat, Collections.emptyList()),
                    prevCatExpenses.getOrDefault(cat, Collections.emptyList())
            );

            allTrends.add(trend);
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                increaseContributors.add(trend);
            }
        }

        // Sort trends: largest absolute difference first
        allTrends.sort(Comparator.comparing((CategoryTrend t) -> t.getDifference().abs()).reversed());
        // Sort increase contributors: largest increase first
        increaseContributors.sort(Comparator.comparing(CategoryTrend::getDifference).reversed());

        return new MonthComparison(
                currentMonth,
                previousMonth,
                currentTotal,
                previousTotal,
                netDifference,
                pctChange,
                currentDailyAvg,
                previousDailyAvg,
                allTrends,
                increaseContributors,
                true,
                "Comparison successfully evaluated based on actual transaction records."
        );
    }

    // =========================================================================
    // 3. Statistical Spending Anomaly Detection
    // =========================================================================

    /**
     * Detects statistically anomalous expenses and periodic surges.
     *
     * Methodology:
     * - Group expenses by category.
     * - Compute sample mean (μ) and standard deviation (σ).
     * - Flag individual expenses that exceed (μ + 2.0*σ), exceed μ by at least 50%, and exceed by ₹150+.
     * - Cross-category high-value outlier detection for major purchases.
     * - Flag periodic monthly category surges that exceed historical monthly averages by 60%+ and ₹500+.
     */
    public List<SpendingAnomaly> detectAnomalies(List<Expense> expenses, Map<YearMonth, BigDecimal> monthlyTotals) {
        if (expenses == null || expenses.size() < MIN_SAMPLE_SIZE_FOR_CATEGORY_STATISTICS) {
            return Collections.emptyList();
        }

        List<SpendingAnomaly> results = new ArrayList<>();
        Set<Integer> flaggedExpenseIds = new HashSet<>();

        // Group transactions by category
        Map<String, List<Expense>> categoryGroups = new HashMap<>();
        for (Expense exp : expenses) {
            if (exp == null || exp.getAmount() <= 0) continue;
            String cat = exp.getCategory() != null ? exp.getCategory() : "Uncategorized";
            categoryGroups.computeIfAbsent(cat, k -> new ArrayList<>()).add(exp);
        }

        // Part A: Transaction-Level Statistical Anomalies (Leave-One-Out Baseline)
        for (Map.Entry<String, List<Expense>> entry : categoryGroups.entrySet()) {
            String category = entry.getKey();
            List<Expense> list = entry.getValue();

            if (list.size() < MIN_SAMPLE_SIZE_FOR_CATEGORY_STATISTICS) {
                // Insufficient category sample size; skip to avoid false positives on small data
                continue;
            }

            for (int i = 0; i < list.size(); i++) {
                Expense exp = list.get(i);
                double amount = exp.getAmount();

                // Compute baseline of all OTHER transactions in this category
                List<Expense> others = new ArrayList<>();
                for (int j = 0; j < list.size(); j++) {
                    if (j != i) others.add(list.get(j));
                }

                if (others.size() < 2) {
                    continue;
                }

                int nOthers = others.size();
                double sumOthers = 0.0;
                for (Expense o : others) {
                    sumOthers += o.getAmount();
                }
                double meanOthers = sumOthers / nOthers;

                double sumSq = 0.0;
                for (Expense o : others) {
                    double d = o.getAmount() - meanOthers;
                    sumSq += d * d;
                }
                double varOthers = sumSq / (nOthers - 1);
                double stdDevOthers = Math.sqrt(varOthers);

                if (stdDevOthers == 0.0) {
                    // Zero Variance in baseline: All other transactions were identical
                    if (amount >= meanOthers * MIN_ANOMALY_PERCENTAGE_SURGE && (amount - meanOthers) >= 100.0) {
                        BigDecimal baseline = MoneyUtil.toBigDecimal(meanOthers);
                        BigDecimal observed = exp.getAmountBigDecimal();
                        BigDecimal diff = observed.subtract(baseline);

                        String expl = String.format(
                                "Historical baseline for '%s' was identical with zero variance (%d records were %s). This transaction of %s is %.1fx higher than the baseline.",
                                category, nOthers, MoneyUtil.formatRupees(baseline), MoneyUtil.formatRupees(observed), amount / meanOthers
                        );

                        if (exp.getId() != null) flaggedExpenseIds.add(exp.getId());
                        results.add(new SpendingAnomaly(
                                SpendingAnomaly.AnomalyType.TRANSACTION_SPIKE,
                                "Identical Pattern Outlier: " + exp.getDescription(),
                                category,
                                observed,
                                baseline,
                                diff,
                                amount / meanOthers,
                                expl,
                                exp.getDate(),
                                Collections.singletonList(exp)
                        ));
                    }
                } else {
                    double zScore = (amount - meanOthers) / stdDevOthers;
                    double excess = amount - meanOthers;

                    if (zScore >= Z_SCORE_THRESHOLD
                            && excess >= MIN_ANOMALY_ABSOLUTE_DIFFERENCE.doubleValue()
                            && amount >= meanOthers * MIN_ANOMALY_PERCENTAGE_SURGE) {

                        BigDecimal baseline = MoneyUtil.toBigDecimal(meanOthers);
                        BigDecimal observed = exp.getAmountBigDecimal();
                        BigDecimal diff = observed.subtract(baseline);

                        String expl = String.format(
                                "Transaction of %s is %.1f standard deviations above the historical '%s' baseline of %s (baseline size: %d, std dev: %s).",
                                MoneyUtil.formatRupees(observed), zScore, category, MoneyUtil.formatRupees(baseline), nOthers, MoneyUtil.formatRupees(stdDevOthers)
                        );

                        if (exp.getId() != null) flaggedExpenseIds.add(exp.getId());
                        results.add(new SpendingAnomaly(
                                SpendingAnomaly.AnomalyType.TRANSACTION_SPIKE,
                                "Statistical Outlier: " + exp.getDescription(),
                                category,
                                observed,
                                baseline,
                                diff,
                                zScore,
                                expl,
                                exp.getDate(),
                                Collections.singletonList(exp)
                        ));
                    }
                }
            }
        }

        // Part B: Global Outlier Detection (Cross-Category High-Value Purchases)
        // Detects massive purchases in categories that have few items (e.g. 1 major electronic purchase)
        if (expenses.size() >= 5) {
            double grandSum = 0.0;
            for (Expense e : expenses) {
                grandSum += e.getAmount();
            }
            double grandMean = grandSum / expenses.size();

            double grandSqDiff = 0.0;
            for (Expense e : expenses) {
                double diff = e.getAmount() - grandMean;
                grandSqDiff += diff * diff;
            }
            double grandStdDev = Math.sqrt(grandSqDiff / (expenses.size() - 1));

            if (grandStdDev > 0) {
                for (Expense exp : expenses) {
                    if (exp.getId() != null && flaggedExpenseIds.contains(exp.getId())) {
                        continue;
                    }
                    double amt = exp.getAmount();
                    double z = (amt - grandMean) / grandStdDev;
                    if (z >= 2.0 && amt >= grandMean * 2.5 && amt >= 1000.0) {
                        BigDecimal baseline = MoneyUtil.toBigDecimal(grandMean);
                        BigDecimal observed = exp.getAmountBigDecimal();
                        BigDecimal diff = observed.subtract(baseline);

                        String expl = String.format(
                                "Transaction of %s in '%s' is %.1f standard deviations above your overall transaction average of %s across all categories.",
                                MoneyUtil.formatRupees(observed), exp.getCategory(), z, MoneyUtil.formatRupees(baseline)
                        );

                        if (exp.getId() != null) flaggedExpenseIds.add(exp.getId());
                        results.add(new SpendingAnomaly(
                                SpendingAnomaly.AnomalyType.TRANSACTION_SPIKE,
                                "High-Value Transaction: " + exp.getDescription(),
                                exp.getCategory(),
                                observed,
                                baseline,
                                diff,
                                z,
                                expl,
                                exp.getDate(),
                                Collections.singletonList(exp)
                        ));
                    }
                }
            }
        }

        // Part C: Periodic Surge Anomaly (Category Monthly Surge & New Category Surges)
        if (monthlyTotals != null && monthlyTotals.size() >= 2) {
            List<YearMonth> months = new ArrayList<>(monthlyTotals.keySet());
            YearMonth latestMonth = months.get(months.size() - 1);
            List<YearMonth> priorMonths = months.subList(0, months.size() - 1);
            BigDecimal latestMonthGrandTotal = monthlyTotals.getOrDefault(latestMonth, MoneyUtil.ZERO);

            for (Map.Entry<String, List<Expense>> entry : categoryGroups.entrySet()) {
                String cat = entry.getKey();
                List<Expense> catExpenses = entry.getValue();

                BigDecimal currentMonthSum = BigDecimal.ZERO;
                List<Expense> currentMonthItems = new ArrayList<>();

                Map<YearMonth, BigDecimal> priorMonthTotals = new HashMap<>();

                for (Expense exp : catExpenses) {
                    if (exp.getDate() == null) continue;
                    YearMonth ym = YearMonth.from(exp.getDate());
                    if (ym.equals(latestMonth)) {
                        currentMonthSum = currentMonthSum.add(exp.getAmountBigDecimal());
                        currentMonthItems.add(exp);
                    } else if (priorMonths.contains(ym)) {
                        priorMonthTotals.put(ym, priorMonthTotals.getOrDefault(ym, MoneyUtil.ZERO).add(exp.getAmountBigDecimal()));
                    }
                }

                if (!priorMonthTotals.isEmpty() && currentMonthSum.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal priorSum = BigDecimal.ZERO;
                    for (BigDecimal val : priorMonthTotals.values()) {
                        priorSum = priorSum.add(val);
                    }
                    BigDecimal historicalMonthlyAvg = priorSum.divide(BigDecimal.valueOf(priorMonths.size()), MoneyUtil.SCALE, RoundingMode.HALF_UP);

                    BigDecimal surgeThreshold = historicalMonthlyAvg.multiply(BigDecimal.valueOf(MONTHLY_SURGE_RATIO));
                    BigDecimal surgeDiff = currentMonthSum.subtract(historicalMonthlyAvg);

                    if (currentMonthSum.compareTo(surgeThreshold) >= 0 && surgeDiff.compareTo(MIN_MONTHLY_SURGE_DIFFERENCE) >= 0) {
                        double multiple = historicalMonthlyAvg.compareTo(BigDecimal.ZERO) > 0
                                ? currentMonthSum.divide(historicalMonthlyAvg, 2, RoundingMode.HALF_UP).doubleValue()
                                : 2.0;

                        String expl = String.format(
                                "Monthly spending in '%s' reached %s for %s, which is %s (+%.0f%%) above the historical monthly baseline of %s across %d prior month(s).",
                                cat, MoneyUtil.formatRupees(currentMonthSum), latestMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                                MoneyUtil.formatRupees(surgeDiff), (multiple - 1.0) * 100, MoneyUtil.formatRupees(historicalMonthlyAvg), priorMonths.size()
                        );

                        results.add(new SpendingAnomaly(
                                SpendingAnomaly.AnomalyType.PERIODIC_SURGE,
                                "Category Monthly Surge: " + cat,
                                cat,
                                currentMonthSum,
                                historicalMonthlyAvg,
                                surgeDiff,
                                multiple,
                                expl,
                                currentMonthItems.isEmpty() ? LocalDate.now() : currentMonthItems.get(0).getDate(),
                                currentMonthItems
                        ));
                    }
                } else if (priorMonthTotals.isEmpty() && currentMonthSum.compareTo(MIN_MONTHLY_SURGE_DIFFERENCE) >= 0 && latestMonthGrandTotal.compareTo(BigDecimal.ZERO) > 0) {
                    // New Category with significant spend this month
                    double share = currentMonthSum.divide(latestMonthGrandTotal, 4, RoundingMode.HALF_UP).doubleValue();
                    if (share >= 0.20) {
                        String expl = String.format(
                                "New spending category '%s' accounted for %s (%.1f%% of total monthly spending in %s) with no prior recorded transactions.",
                                cat, MoneyUtil.formatRupees(currentMonthSum), share * 100, latestMonth.format(DateTimeFormatter.ofPattern("MMM yyyy"))
                        );

                        results.add(new SpendingAnomaly(
                                SpendingAnomaly.AnomalyType.PERIODIC_SURGE,
                                "New Category Surge: " + cat,
                                cat,
                                currentMonthSum,
                                MoneyUtil.ZERO,
                                currentMonthSum,
                                share * 100,
                                expl,
                                currentMonthItems.isEmpty() ? LocalDate.now() : currentMonthItems.get(0).getDate(),
                                currentMonthItems
                        ));
                    }
                }
            }
        }

        // Sort anomalies: largest difference first
        results.sort(Comparator.comparing(SpendingAnomaly::getDifference).reversed());
        return results;
    }

    private String generateExecutiveSummary(MonthComparison comp, List<SpendingAnomaly> anomalies, int totalCount) {
        StringBuilder sb = new StringBuilder();

        if (comp.isSufficientHistory()) {
            if (comp.getNetDifference().compareTo(BigDecimal.ZERO) > 0) {
                sb.append(String.format("Spending increased by %s (%s) compared to %s.",
                        comp.getFormattedNetDifference(),
                        comp.getFormattedPercentageChange(),
                        comp.getPreviousMonth().format(DateTimeFormatter.ofPattern("MMM yyyy"))
                ));
            } else if (comp.getNetDifference().compareTo(BigDecimal.ZERO) < 0) {
                sb.append(String.format("Spending decreased by %s (%s) compared to %s.",
                        comp.getFormattedNetDifference(),
                        comp.getFormattedPercentageChange(),
                        comp.getPreviousMonth().format(DateTimeFormatter.ofPattern("MMM yyyy"))
                ));
            } else {
                sb.append(String.format("Spending remained flat compared to %s.",
                        comp.getPreviousMonth().format(DateTimeFormatter.ofPattern("MMM yyyy"))
                ));
            }

            if (!comp.getTopIncreaseContributors().isEmpty()) {
                CategoryTrend top = comp.getTopIncreaseContributors().get(0);
                sb.append(String.format(" Largest contributor to increase: %s (%s, %s of increase).",
                        top.getCategory(), top.getFormattedDifference(), top.getFormattedContribution()
                ));
            }
        } else {
            sb.append(comp.getStatusMessage());
        }

        if (!anomalies.isEmpty()) {
            sb.append(String.format(" Detected %d potential spending %s requiring review.",
                    anomalies.size(), anomalies.size() == 1 ? "anomaly" : "anomalies"
            ));
        }

        return sb.toString();
    }
}
