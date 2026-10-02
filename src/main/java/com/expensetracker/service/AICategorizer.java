package com.expensetracker.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Categorization Service.
 *
 * Uses lightweight Natural Language Processing (NLP) techniques:
 * - Text normalization
 * - Tokenization
 * - Keyword matching
 * - Phrase matching
 * - Weighted category scoring
 * - Basic word normalization
 *
 * No external AI API is required.
 */
public class AICategorizer {

    private final Map<String, String> keywordMap;
    private final Map<String, Integer> keywordWeights;
    private final Map<String, List<String>> categoryKeywords;
    private final List<String> availableCategories;

    public AICategorizer() {

        keywordMap = new HashMap<>();
        keywordWeights = new HashMap<>();
        categoryKeywords = new HashMap<>();

        availableCategories = Collections.unmodifiableList(
                Arrays.asList(
                        "Transport",
                        "Food & Dining",
                        "Shopping",
                        "Entertainment",
                        "Housing & Utilities",
                        "Health & Medical",
                        "Salary & Income",
                        "Education",
                        "Subscriptions",
                        "Other"
                )
        );

        initializeKeywordDictionary();
    }

    /**
     * Initializes the keyword dictionary.
     *
     * Each category contains common words and phrases
     * that are likely to appear in expense descriptions.
     */
    private void initializeKeywordDictionary() {

        // =========================
        // Transport
        // =========================

        registerKeywords(
                "Transport",
                3,
                "uber", "lyft", "ola", "rapido",
                "cab", "taxi", "auto", "rickshaw",
                "flight", "airline", "airport",
                "train", "railway", "rail",
                "bus", "metro", "subway",
                "transport", "transit",
                "parking", "toll",
                "petrol", "diesel", "fuel",
                "gas", "gasoline",
                "shell", "chevron", "bp",
                "carwash", "car wash",
                "vehicle", "bike", "bicycle",
                "scooter", "motorcycle",
                "charging", "ev",
                "travel", "trip"
        );

        // =========================
        // Food & Dining
        // =========================

        registerKeywords(
                "Food & Dining",
                3,
                "starbucks", "coffee", "tea",
                "mcdonalds", "burger", "pizza",
                "restaurant", "cafe", "cafeteria",
                "lunch", "dinner", "breakfast",
                "bistro", "bakery", "diner",
                "snack", "food", "dining",
                "takeout", "takeaway",
                "delivery", "ubereats",
                "doordash", "grubhub",
                "dominos", "kfc",
                "subway", "bar", "pub",
                "grocery", "groceries",
                "supermarket", "walmart",
                "wholefoods", "costco", "aldi",
                "vegetables", "vegetable",
                "fruit", "fruits",
                "milk", "bread",
                "meat", "chicken",
                "fish", "restaurant",
                "meal", "canteen"
        );

        // =========================
        // Shopping
        // =========================

        registerKeywords(
                "Shopping",
                3,
                "amazon", "ebay",
                "shopping", "mall",
                "apparel", "clothing",
                "clothes", "shirt",
                "tshirt", "pants",
                "jeans", "shoes",
                "sneakers", "nike",
                "adidas", "zara",
                "electronics", "laptop",
                "computer", "keyboard",
                "mouse", "monitor",
                "phone", "mobile",
                "iphone", "samsung",
                "headphones", "earphones",
                "charger", "cable",
                "accessory", "accessories",
                "camera", "television",
                "tv", "watch",
                "jewelry", "jewellery",
                "cosmetics", "sephora",
                "boutique", "fashion",
                "bag", "backpack",
                "wallet", "store",
                "gift", "present",
                "furniture", "decor"
        );

        // =========================
        // Entertainment
        // =========================

        registerKeywords(
                "Entertainment",
                3,
                "netflix", "spotify",
                "cinema", "movie",
                "movies", "film",
                "ticket", "tickets",
                "hulu", "disney",
                "prime video",
                "gaming", "game",
                "steam", "playstation",
                "xbox", "nintendo",
                "concert", "theater",
                "theatre", "show",
                "event", "bowling",
                "amusement", "park",
                "stadium", "theme park",
                "arcade", "festival",
                "party"
        );

        // =========================
        // Housing & Utilities
        // =========================

        registerKeywords(
                "Housing & Utilities",
                3,
                "rent", "rental",
                "lease", "mortgage",
                "electric", "electricity",
                "power", "water",
                "sewer", "utility",
                "utilities", "internet",
                "wifi", "broadband",
                "comcast", "verizon",
                "att", "spectrum",
                "trash", "waste",
                "hoa", "maintenance",
                "repair", "plumber",
                "plumbing", "apartment",
                "house", "home",
                "gas bill", "electric bill",
                "water bill"
        );

        // =========================
        // Health & Medical
        // =========================

        registerKeywords(
                "Health & Medical",
                3,
                "doctor", "hospital",
                "pharmacy", "medicine",
                "medication", "clinic",
                "dentist", "dental",
                "medical", "therapy",
                "physio", "health",
                "cvs", "walgreens",
                "optician", "glasses",
                "spectacles", "eyeglasses",
                "contact lens",
                "lens", "gym",
                "fitness", "workout",
                "supplement", "supplements",
                "vitamin", "vitamins",
                "protein", "protein powder",
                "checkup", "check-up",
                "surgery", "treatment",
                "doctor visit"
        );

        // =========================
        // Salary & Income
        // =========================

        registerKeywords(
                "Salary & Income",
                4,
                "salary", "wage",
                "paycheck", "pay",
                "bonus", "stipend",
                "dividend", "freelance",
                "freelancing", "commission",
                "interest", "income",
                "payout", "reimbursement",
                "refund", "earnings",
                "earning", "payment received",
                "salary received"
        );

        // =========================
        // Education
        // =========================

        registerKeywords(
                "Education",
                3,
                "tuition", "course",
                "udemy", "coursera",
                "edx", "books",
                "book", "textbook",
                "college", "school",
                "exam", "examination",
                "university", "academy",
                "training", "seminar",
                "workshop", "education",
                "study", "studies",
                "student", "class",
                "learning", "leetcode",
                "coding course"
        );

        // =========================
        // Subscriptions
        // =========================

        registerKeywords(
                "Subscriptions",
                4,
                "subscription", "subscribe",
                "subscribed", "monthly",
                "annual", "membership",
                "icloud", "dropbox",
                "github", "openai",
                "chatgpt", "canva",
                "adobe", "saas",
                "software", "premium",
                "pro plan", "membership fee"
        );
    }

    /**
     * Registers keywords for a category.
     */
    private void registerKeywords(
            String category,
            int weight,
            String... keywords) {

        List<String> keywordsForCategory =
                categoryKeywords.computeIfAbsent(
                        category,
                        key -> new ArrayList<>()
                );

        for (String keyword : keywords) {

            String normalizedKeyword =
                    normalize(keyword);

            keywordMap.put(
                    normalizedKeyword,
                    category
            );

            keywordWeights.put(
                    normalizedKeyword,
                    weight
            );

            keywordsForCategory.add(
                    normalizedKeyword
            );
        }
    }

    /**
     * Predicts the most likely category for an expense description.
     *
     * NLP pipeline:
     * 1. Normalize text
     * 2. Tokenize text
     * 3. Match exact keywords
     * 4. Match phrases
     * 5. Match related word forms
     * 6. Calculate category scores
     * 7. Return highest scoring category
     *
     * @param description raw expense description
     * @return prediction result containing category and confidence
     */
    public PredictionResult predictCategory(String description) {

        if (description == null ||
                description.trim().isEmpty()) {

            return new PredictionResult(
                    "Other",
                    0.0
            );
        }

        String normalized =
                normalize(description);

        String[] tokens =
                normalized.split("\\s+");

        Map<String, Integer> categoryScores =
                new HashMap<>();

        // =========================
        // Exact and word-based matching
        // =========================

        for (String token : tokens) {

            if (token.length() < 2) {
                continue;
            }

            String exactCategory =
                    keywordMap.get(token);

            if (exactCategory != null) {

                int weight =
                        keywordWeights.getOrDefault(
                                token,
                                2
                        );

                addScore(
                        categoryScores,
                        exactCategory,
                        weight
                );

                continue;
            }

            // Related-word matching
            for (Map.Entry<String, String> entry :
                    keywordMap.entrySet()) {

                String keyword =
                        entry.getKey();

                if (keyword.contains(" ")) {
                    continue;
                }

                if (isRelatedWord(token, keyword)) {

                    int weight =
                            keywordWeights.getOrDefault(
                                    keyword,
                                    1
                            );

                    addScore(
                            categoryScores,
                            entry.getValue(),
                            Math.max(1, weight - 1)
                    );
                }
            }
        }

        // =========================
        // Phrase matching
        // =========================

        for (Map.Entry<String, String> entry :
                keywordMap.entrySet()) {

            String keyword =
                    entry.getKey();

            if (!keyword.contains(" ")) {
                continue;
            }

            if (normalized.contains(keyword)) {

                int weight =
                        keywordWeights.getOrDefault(
                                keyword,
                                3
                        );

                addScore(
                        categoryScores,
                        entry.getValue(),
                        weight + 2
                );
            }
        }

        // =========================
        // Find best category
        // =========================

        String bestCategory = "Other";
        int bestScore = 0;

        for (Map.Entry<String, Integer> entry :
                categoryScores.entrySet()) {

            if (entry.getValue() > bestScore) {

                bestScore =
                        entry.getValue();

                bestCategory =
                        entry.getKey();
            }
        }

        if (bestScore == 0) {

            return new PredictionResult(
                    "Other",
                    0.0
            );
        }

        // =========================
        // Confidence calculation
        // =========================

        int secondBestScore = 0;

        for (Map.Entry<String, Integer> entry :
                categoryScores.entrySet()) {

            if (!entry.getKey().equals(bestCategory)
                    && entry.getValue() > secondBestScore) {

                secondBestScore =
                        entry.getValue();
            }
        }

        double confidence;

        if (bestScore >= 8) {

            confidence = 0.95;

        } else if (bestScore >= 6) {

            confidence = 0.90;

        } else if (bestScore >= 4) {

            confidence = 0.80;

        } else {

            confidence = 0.65;
        }

        // Reduce confidence when categories are very close.
        if (secondBestScore > 0 &&
                bestScore - secondBestScore <= 1) {

            confidence =
                    Math.max(
                            0.50,
                            confidence - 0.15
                    );
        }

        return new PredictionResult(
                bestCategory,
                confidence
        );
    }

    /**
     * Checks whether two words are related.
     *
     * Handles simple variations such as:
     * - movies -> movie
     * - groceries -> grocery
     * - subscriptions -> subscription
     * - shopping -> shop
     */
    private boolean isRelatedWord(
            String token,
            String keyword) {

        if (token.equals(keyword)) {
            return true;
        }

        if (token.length() >= 5 &&
                keyword.length() >= 4) {

            if (token.startsWith(keyword)
                    || keyword.startsWith(token)) {

                return true;
            }
        }

        // Simple plural handling
        if (token.endsWith("s")
                && token.substring(
                        0,
                        token.length() - 1
                ).equals(keyword)) {

            return true;
        }

        if (keyword.endsWith("s")
                && keyword.substring(
                        0,
                        keyword.length() - 1
                ).equals(token)) {

            return true;
        }

        // Common -ing variation
        if (token.endsWith("ing")
                && token.length() > 5) {

            String stem =
                    token.substring(
                            0,
                            token.length() - 3
                    );

            if (stem.equals(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds a score to a category.
     */
    private void addScore(
            Map<String, Integer> scores,
            String category,
            int score) {

        scores.put(
                category,
                scores.getOrDefault(
                        category,
                        0
                ) + score
        );
    }

    /**
     * Normalizes text before processing.
     */
    private String normalize(String text) {

        return text
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Returns all available expense categories.
     */
    public List<String> getAvailableCategories() {

        return availableCategories;
    }

    /**
     * Immutable result of a category prediction.
     */
    public static class PredictionResult {

        private final String category;
        private final double confidence;

        public PredictionResult(
                String category,
                double confidence) {

            this.category = category;
            this.confidence = confidence;
        }

        public String getCategory() {
            return category;
        }

        public double getConfidence() {
            return confidence;
        }

        public boolean isConfident() {
            return confidence >= 0.65;
        }

        @Override
        public String toString() {

            return "PredictionResult{" +
                    "category='" + category + '\'' +
                    ", confidence=" + confidence +
                    '}';
        }
    }
}
