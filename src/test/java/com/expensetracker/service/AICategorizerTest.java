package com.expensetracker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AICategorizerTest {

    private AICategorizer categorizer;

    @BeforeEach
    void setUp() {
        categorizer = new AICategorizer();
    }

    @Test
    void testTransportPrediction() {
        AICategorizer.PredictionResult result = categorizer.predictCategory("Uber ride to airport");
        assertEquals("Transport", result.getCategory());
        assertTrue(result.getConfidence() >= 0.65);
    }

    @Test
    void testFoodDiningPrediction() {
        AICategorizer.PredictionResult result = categorizer.predictCategory("Starbucks morning coffee");
        assertEquals("Food & Dining", result.getCategory());
        assertTrue(result.isConfident());
    }

    @Test
    void testHousingUtilitiesPrediction() {
        AICategorizer.PredictionResult result = categorizer.predictCategory("Electricity bill payment");
        assertEquals("Housing & Utilities", result.getCategory());
    }

    @Test
    void testSubscriptionsPrediction() {
        AICategorizer.PredictionResult result = categorizer.predictCategory("Netflix monthly subscription");
        assertEquals("Subscriptions", result.getCategory());
    }

    @Test
    void testEmptyOrUnknownDescriptionDefaultsToOther() {
        AICategorizer.PredictionResult empty = categorizer.predictCategory("");
        assertEquals("Other", empty.getCategory());
        assertFalse(empty.isConfident());

        AICategorizer.PredictionResult nullDesc = categorizer.predictCategory(null);
        assertEquals("Other", nullDesc.getCategory());
    }
}
