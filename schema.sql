-- AI-Powered Expense Tracker Database Schema
-- Target: MySQL 8.0+

CREATE DATABASE IF NOT EXISTS expense_tracker_db;
USE expense_tracker_db;

-- Drop table if re-initializing
-- DROP TABLE IF EXISTS expenses;

CREATE TABLE IF NOT EXISTS expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    amount DOUBLE NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed Initial Data for Demonstration
INSERT INTO expenses (amount, category, description, date) VALUES
(45.50, 'Transport', 'Uber trip to office', '2026-09-28'),
(120.00, 'Shopping', 'Amazon electronics order', '2026-09-29'),
(15.75, 'Food & Dining', 'Starbucks coffee with team', '2026-09-30'),
(85.00, 'Housing & Utilities', 'Electricity bill payment', '2026-10-01'),
(150.00, 'Health & Medical', 'Doctor consultation and medicine', '2026-09-25'),
(25.00, 'Entertainment', 'Movie tickets multiplex', '2026-09-27');
