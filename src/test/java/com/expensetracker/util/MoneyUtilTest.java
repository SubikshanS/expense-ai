package com.expensetracker.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoneyUtilTest {

    @Test
    void testFormatRupees() {
        assertEquals("₹150.50", MoneyUtil.formatRupees(150.5));
        assertEquals("₹1,250.00", MoneyUtil.formatRupees(1250.0));
        assertEquals("₹0.00", MoneyUtil.formatRupees(0.0));
    }

    @Test
    void testSumDoublesAvoidsPrecisionErrors() {
        // In IEEE 754 floating point: 0.1 + 0.2 != 0.3
        List<Double> doubles = Arrays.asList(0.1, 0.2, 0.3);
        BigDecimal sum = MoneyUtil.sumDoubles(doubles);
        assertEquals(new BigDecimal("0.60"), sum);
    }

    @Test
    void testParseAmountValid() {
        BigDecimal amount = MoneyUtil.parseAmount(" 1,450.75 ");
        assertEquals(new BigDecimal("1450.75"), amount);

        BigDecimal rupeesPrefixed = MoneyUtil.parseAmount("₹250.00");
        assertEquals(new BigDecimal("250.00"), rupeesPrefixed);
    }

    @Test
    void testParseAmountInvalid() {
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.parseAmount(""));
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.parseAmount("abc"));
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.parseAmount("-50"));
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.parseAmount("0"));
    }
}
