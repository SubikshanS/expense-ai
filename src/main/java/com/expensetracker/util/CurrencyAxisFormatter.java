package com.expensetracker.util;

import javafx.util.StringConverter;

/**
 * Axis tick formatter converting raw numeric amounts into clean, human-readable
 * Indian Rupee (₹) currency labels with smart magnitude abbreviations.
 */
public class CurrencyAxisFormatter extends StringConverter<Number> {

    @Override
    public String toString(Number object) {
        if (object == null) {
            return "₹0";
        }
        double val = object.doubleValue();
        if (Math.abs(val) < 0.001) {
            return "₹0";
        }

        boolean negative = val < 0;
        double abs = Math.abs(val);
        String prefix = negative ? "-₹" : "₹";

        if (abs >= 10_000_000) {
            double cr = abs / 10_000_000.0;
            if (cr == (long) cr) {
                return String.format("%s%dCr", prefix, (long) cr);
            } else {
                return String.format("%s%.1fCr", prefix, cr);
            }
        } else if (abs >= 100_000) {
            double l = abs / 100_000.0;
            if (l == (long) l) {
                return String.format("%s%dL", prefix, (long) l);
            } else {
                return String.format("%s%.1fL", prefix, l);
            }
        } else if (abs >= 1_000) {
            double k = abs / 1_000.0;
            if (k == (long) k) {
                return String.format("%s%dk", prefix, (long) k);
            } else {
                return String.format("%s%.1fk", prefix, k);
            }
        } else {
            return String.format("%s%.0f", prefix, abs);
        }
    }

    @Override
    public Number fromString(String string) {
        if (string == null || string.isBlank()) return 0.0;
        try {
            String clean = string.replace("₹", "").replace(",", "").trim();
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
