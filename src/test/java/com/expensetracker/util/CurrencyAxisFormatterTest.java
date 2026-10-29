package com.expensetracker.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class CurrencyAxisFormatterTest {

    private final CurrencyAxisFormatter formatter = new CurrencyAxisFormatter();

    @Test
    public void testZeroAndNearZero() {
        assertEquals("₹0", formatter.toString(0));
        assertEquals("₹0", formatter.toString(0.0));
        assertEquals("₹0", formatter.toString(0.00001));
        assertEquals("₹0", formatter.toString(null));
    }

    @Test
    public void testUnderOneThousand() {
        assertEquals("₹50", formatter.toString(50));
        assertEquals("₹500", formatter.toString(500.0));
        assertEquals("₹999", formatter.toString(999.4));
    }

    @Test
    public void testThousandsMagnitude() {
        assertEquals("₹1k", formatter.toString(1000.0));
        assertEquals("₹1k", formatter.toString(1000));
        assertEquals("₹1.5k", formatter.toString(1500.0));
        assertEquals("₹12k", formatter.toString(12000.0));
        assertEquals("₹12.2k", formatter.toString(12212.0));
    }

    @Test
    public void testLakhsMagnitude() {
        assertEquals("₹1L", formatter.toString(100000.0));
        assertEquals("₹2.5L", formatter.toString(250000.0));
        assertEquals("₹10L", formatter.toString(1000000.0));
    }

    @Test
    public void testCroresMagnitude() {
        assertEquals("₹1Cr", formatter.toString(10000000.0));
        assertEquals("₹2.5Cr", formatter.toString(25000000.0));
    }

    @Test
    public void testNegativeNumbers() {
        assertEquals("-₹500", formatter.toString(-500.0));
        assertEquals("-₹1k", formatter.toString(-1000.0));
        assertEquals("-₹2.5L", formatter.toString(-250000.0));
    }

    @Test
    public void testVariousNumberTypes() {
        assertEquals("₹1k", formatter.toString(1000L));
        assertEquals("₹1k", formatter.toString(1000));
        assertEquals("₹1k", formatter.toString(new BigDecimal("1000")));
    }

    @Test
    public void testFromString() {
        assertEquals(1000.0, formatter.fromString("₹1,000").doubleValue(), 0.01);
        assertEquals(0.0, formatter.fromString("").doubleValue(), 0.01);
        assertEquals(0.0, formatter.fromString(null).doubleValue(), 0.01);
    }
}

