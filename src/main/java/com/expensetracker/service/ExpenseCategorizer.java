package com.expensetracker.service;

import java.util.List;

/**
 * Strategy interface for expense categorization.
 * Enables plugging in external AI API categorizers while retaining
 * rule-based local NLP as a robust fallback.
 */
public interface ExpenseCategorizer {

    /**
     * Predicts the most likely category for an expense description.
     *
     * @param description Raw expense description
     * @return PredictionResult containing predicted category and confidence
     */
    AICategorizer.PredictionResult predictCategory(String description);

    /**
     * Returns all available expense categories.
     *
     * @return Immutable list of category names
     */
    List<String> getAvailableCategories();
}
