# 🤖 AI-Powered Full-Stack Expense Tracker

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blue.svg)](https://openjfx.io/)
[![Database](https://img.shields.io/badge/Database-MySQL_8%20%7C%20SQLite_Fallback-blue.svg)](https://www.mysql.com/)
[![Build](https://img.shields.io/badge/Build-Maven_3.8+-red.svg)](https://maven.apache.org/)
[![Tests](https://img.shields.io/badge/Tests-48_Passed-brightgreen.svg)]()
[![Offline First](https://img.shields.io/badge/Architecture-Offline--First-success.svg)]()

A modern, high-performance JavaFX desktop financial application built with **Java 21, JavaFX, JDBC, MySQL 8 (with embedded SQLite fallback), Maven, and an Offline-First AI Categorization Engine with optional API support**.

The application provides an end-to-end personal finance management suite: automatic transaction categorization, responsive search and filtering, visual analytics, financial intelligence (anomaly detection and spending trends), and export capabilities for CSV and Excel.

---

## 📑 Table of Contents

- [Key Highlights](#-key-highlights)
- [Architecture & Design](#-architecture--design)
- [Categorization Engine: Offline-First + Optional API](#-categorization-engine-offline-first--optional-api)
- [Core Features](#-core-features)
  - [1. Dashboard Overview & Visual Analytics](#1-dashboard-overview--visual-analytics)
  - [2. Transactions Management](#2-transactions-management)
  - [3. Deep-Dive Analytics](#3-deep-dive-analytics)
  - [4. Financial Intelligence & Spending Insights](#4-financial-intelligence--spending-insights)
  - [5. Reports & Data Export (CSV & Excel)](#5-reports--data-export-csv--excel)
- [Dual-Database Persistence](#-dual-database-persistence)
- [Technology Stack](#-technology-stack)
- [Project Structure](#-project-structure)
- [Configuration & Environment Variables](#-configuration--environment-variables)
- [Automated Testing](#-automated-testing)
- [GitHub Safety & Security Rules](#-github-safety--security-rules)
- [Getting Started](#-getting-started)
- [License](#-license)

---

## 🌟 Key Highlights

- **🔒 Offline-First Architecture**: Operates 100% offline without requiring internet access, external API subscriptions, or cloud keys. Your financial data stays private on your local machine.
- **🤖 Extensible AI Categorization Strategy**: Uses a lightweight local NLP engine (tokenization, phrase matching, stemming, weighted category scoring) with an extensible `ExpenseCategorizer` interface ready to connect to cloud or local LLM APIs (e.g. Gemini, OpenAI, or local Ollama).
- **🛡️ Dual Database Resilience**: Connects to **MySQL 8** for production relational storage, and seamlessly falls back to embedded **SQLite (`expense_tracker.db`)** if MySQL is unavailable.
- **📊 Symmetrical Visual Analytics**: Clean, modern dashboard featuring balanced 50/50 visual cards with a centered Category Distribution Pie Chart and a 6-Month Monthly Spending Bar Chart.
- **💡 Financial Intelligence**: Automatic anomaly detection, category growth/shrinkage trends, and dynamic budgeting recommendations.
- **📈 Comprehensive Test Suite**: 48 automated JUnit 5 tests covering business logic, monetary math (`BigDecimal`), categorization, analytics, and file exporters.

---

## 🏗️ Architecture & Design

The application follows clean architectural principles with strict separation of concerns, background concurrency, and dependency inversion via the Strategy pattern:

```text
                               JavaFX Desktop UI
                        ┌──────────────────────────────┐
                        │        MainDashboard         │
                        │ (Multi-View Navigation Hub)  │
                        └──────────────┬───────────────┘
                                       │ (Async Tasks / Platform.runLater)
                                       ▼
                                 ExpenseService
                  ┌────────────────────┼───────────────────┐
                  ▼                    ▼                   ▼
          ExpenseCategorizer       ExpenseDAO          Exporters
          (Strategy Pattern)           │             (CSV & Excel)
                  │                    ▼                   │
       ┌──────────┴──────────┐   DBConnection              ▼
       ▼                     ▼         │            Generated Files
  AICategorizer         Optional API   │           (.csv / .xlsx)
(Local NLP Engine)     (Gemini/Ollama) ┌───────┴───────┐
                                       ▼               ▼
                                    MySQL 8      SQLite Fallback
```

### Multi-View Navigation Flow

The dashboard is structured into five dedicated views accessible via the sidebar:

1. **Dashboard Overview**: High-level financial health cards and aligned visual distribution charts.
2. **Transactions**: Complete transaction records with live auto-categorization, search, date filtering, and CRUD operations.
3. **Analytics**: Time-series spending trends (7D, 30D, 3M, 6M, 1Y, ALL) and Month-over-Month (MoM) normalized category comparisons.
4. **Spending Insights**: Automated financial intelligence detecting spending spikes, abnormal expenses, and budgeting alerts.
5. **Reports & Export**: Category breakdown reports and one-click exports to CSV and Excel `.xlsx`.

---

## 🧠 Categorization Engine: Offline-First + Optional API

### Design Philosophy

Financial management applications require **high reliability, low latency, and strict user privacy**. The categorization system is built around an **Offline-First** core with an extensible **Strategy Pattern** for optional external AI enhancements.

```text
                         Expense Description (User Input)
                                       │
                                       ▼
                   ┌───────────────────────────────────────┐
                   │  Tier 1: Local Transaction Memory     │
                   │  (Matches past user overrides in DB)  │
                   └───────────────────┬───────────────────┘
                                       │ (Unmatched)
                                       ▼
                   ┌───────────────────────────────────────┐
                   │  Tier 2: Local Rule-Based NLP Engine  │
                   │  • Text normalization & lowercasing   │
                   │  • Word stemming & tokenization       │
                   │  • Keyword & multi-word phrase match  │
                   │  • Weighted scoring & confidence check│
                   └───────────────────┬───────────────────┘
                                       │ (Ambiguous / Unknown)
                                       ▼
                   ┌───────────────────────────────────────┐
                   │  Tier 3: Optional Cloud / Local API   │
                   │  (e.g., Google Gemini, Ollama Local)  │
                   └───────────────────┬───────────────────┘
                                       │
                        ┌──────────────┴──────────────┐
                        │ (Success)                   │ (API Offline / Timeout)
                        ▼                             ▼
                Predicted Category            Instant Fallback to
                                               Tier 2 "Other" / Best
```

### 1. Offline Rule-Based NLP Engine (Default)

The built-in [`AICategorizer`](file:///c:/Users/SUBIKSHAN/.gemini/antigravity/scratch/ExpenseTrackerAI/src/main/java/com/expensetracker/service/AICategorizer.java) runs entirely within the JVM without external calls:

- **Text Normalization**: Strips punctuation, trims whitespace, and converts to lowercase.
- **Stemming & Morpheme Handling**: Recognizes root stems (e.g., `medic-` matches `medicine`, `medication`, `medical`).
- **Phrase Matching**: Prioritizes multi-token phrases over single words (e.g., `ice cream` or `grocery shopping` $\to$ Food & Dining).
- **Weighted Scoring**: Assigns higher confidence scores to unambiguous keywords (e.g., `netflix` $\to$ Subscriptions) over generic words.
- **Prediction Result**: Produces both a predicted category and a confidence level (`HIGH`, `MEDIUM`, `LOW`).

#### Supported Categories:

- 🚗 **Transport** (e.g., *uber, bus ticket, metro, petrol, flight, train*)
- 🍔 **Food & Dining** (e.g., *restaurant dinner, starbucks, pizza, grocery, cafe*)
- 🛍️ **Shopping** (e.g., *amazon, clothing, shoes, electronics, apparel*)
- 🎬 **Entertainment** (e.g., *cinema, movie, concert, spotify, steam, game*)
- 💡 **Housing & Utilities** (e.g., *electricity bill, rent, water, wifi, gas*)
- 🏥 **Health & Medical** (e.g., *pharmacy, doctor, medicine, clinic, dentist*)
- 💼 **Salary & Income** (e.g., *payroll, bonus, dividend, freelance*)
- 🎓 **Education** (e.g., *tuition, books, course, udemy, seminar*)
- 🔄 **Subscriptions** (e.g., *netflix, gym membership, prime, icloud*)
- 📦 **Other** (unclassified fallback)

### 2. Optional AI API Integration

Through the [`ExpenseCategorizer`](file:///c:/Users/SUBIKSHAN/.gemini/antigravity/scratch/ExpenseTrackerAI/src/main/java/com/expensetracker/service/ExpenseCategorizer.java) interface, developers can seamlessly plug in cloud LLMs or local models without modifying the UI or database layers:

```java
public interface ExpenseCategorizer {
    AICategorizer.PredictionResult predictCategory(String description);
    List<String> getAvailableCategories();
}
```

- **Cloud AI (e.g. Google Gemini API / OpenAI)**: Enables zero-shot semantic categorization for novel or regional slang descriptions when an internet connection and API key are configured.
- **Local AI (e.g. Ollama / LLaMA on `localhost:11434`)**: Enables offline LLM inference with complete privacy and zero data leakage.
- **Zero-Error Fallback Guarantee**: If an external API is slow, rate-limited, or unavailable, the application immediately falls back to the local offline NLP engine, ensuring a smooth and uninterrupted user experience.

---

## ✨ Core Features

### 1. Dashboard Overview & Visual Analytics

- **KPI Cards**: Displays real-time aggregate figures for **Total Spending**, **Top Category**, **Total Transactions**, and **Average Expense**.
- **Category Distribution (Pie Chart)**: Clean 50/50 symmetrical visual card with bottom legend and percentage tooltips, providing an instant view of budget allocation.
- **Monthly Spending (Bar Chart)**: Chronological breakdown of expenses over the latest six active months, formatted with currency labels (`$`, `€`, `₹`).

### 2. Transactions Management

- **Live Auto-Categorization**: Categorizer listens to description input changes in real-time, instantly suggesting the category and updating the form.
- **CRUD Operations**: Add, Edit, Delete, and Clear fields with full `BigDecimal` precision.
- **Instant Search**: Real-time filtering by description and category keywords.
- **Month Filtering & Sorting**: Filter transactions by specific months, or sort chronologically and by expense amount.

### 3. Deep-Dive Analytics

- **Time-Series Spending Trends**: Line chart with dynamic time range selectors (`7D`, `30D`, `3M`, `6M`, `1Y`, `ALL`) to visualize spending momentum over time.
- **Month-over-Month (MoM) Comparison**: Side-by-side normalized category comparison bars illustrating spending shifts between adjacent months.

### 4. Financial Intelligence & Spending Insights

- **Anomaly Detection**: Flags transactions that deviate significantly from average spending habits (e.g., sudden large purchases).
- **Trend Analysis**: Highlights fastest-growing expense categories and identifies areas with noticeable savings.
- **Budgeting Suggestions**: Context-aware advice based on recurring high-expense categories.

### 5. Reports & Data Export (CSV & Excel)

- **Detailed Summary Tables**: Category totals, transaction counts, and percentage shares.
- **CSV Export**: Clean, standard RFC-4180 UTF-8 formatted CSV file with expense records, category totals, and grand totals.
- **Excel (`.xlsx`) Export**: Rich spreadsheets generated via **Apache POI** featuring styled headers, bold summary rows, currency number formatting, and auto-adjusted column widths.

---

## 🗄️ Dual-Database Persistence

The application employs a dual-database architecture managed by [`DBConnection`](file:///c:/Users/SUBIKSHAN/.gemini/antigravity/scratch/ExpenseTrackerAI/src/main/java/com/expensetracker/config/DBConnection.java):

```text
                  DBConnection.getConnection()
                               │
             ┌─────────────────┴─────────────────┐
             ▼ (Attempt 1)                       ▼ (Fallback)
         MySQL 8                             SQLite
 (Primary Relational DB)            (Zero-Config Embedded)
  jdbc:mysql://localhost:3306        jdbc:sqlite:expense_tracker.db
```

1. **MySQL 8 (Primary)**:
   - Configured for multi-user, production-grade relational storage.
   - Schema auto-initializes on startup with prepared statements to prevent SQL injection.
2. **SQLite (Embedded Fallback)**:
   - If MySQL is not installed or unreachable, the system automatically switches to local embedded SQLite (`expense_tracker.db`).
   - Ensures zero-setup out of the box for quick evaluation, testing, or offline-only workstations.

---

## 🛠️ Technology Stack

| Component | Technology | Description |
|---|---|---|
| **Runtime & Language** | Java 21 (LTS) | Modern Java with records, pattern matching, and enhanced switch expressions |
| **GUI Framework** | JavaFX 21.0.2 | Hardware-accelerated desktop UI framework |
| **Primary Database** | MySQL 8.3.0 | Relational database accessed via JDBC prepared statements |
| **Embedded Database** | SQLite JDBC 3.45.1.0 | Zero-configuration local database fallback |
| **Spreadsheet Engine** | Apache POI 5.2.5 | Generation and styling of `.xlsx` Excel workbooks |
| **Monetary Math** | `BigDecimal` & `MoneyUtil` | High-precision arithmetic preventing floating-point rounding errors |
| **Build & Dependencies** | Maven 3.8+ | Dependency management and build automation |
| **Testing Framework** | JUnit 5 (Jupiter 5.10.2) | Comprehensive unit, service, and integration testing |

---

## 📁 Project Structure

```text
ExpenseTrackerAI/
├── pom.xml                               # Maven project dependencies and build configuration
├── schema.sql                            # Relational database schema script
├── README.md                             # Comprehensive project documentation
├── .gitignore                            # Git exclusion rules (sensitive data & binaries)
│
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── expensetracker/
    │   │           ├── MainApp.java                    # JavaFX application entry point
    │   │           │
    │   │           ├── config/
    │   │           │   └── DBConnection.java           # Dual-backend JDBC manager (MySQL/SQLite)
    │   │           │
    │   │           ├── dao/
    │   │           │   └── ExpenseDAO.java             # SQL data access object with prepared statements
    │   │           │
    │   │           ├── exception/
    │   │           │   ├── DatabaseOperationException.java
    │   │           │   └── ValidationException.java
    │   │           │
    │   │           ├── model/
    │   │           │   ├── Expense.java                # Core expense domain model
    │   │           │   ├── ExpenseSummary.java         # Financial aggregate model
    │   │           │   ├── CategorySpendingPoint.java  # Category analytics data point
    │   │           │   ├── TimeSeriesPoint.java        # Time-series trend point
    │   │           │   ├── CategoryMoMPoint.java       # Month-over-month category model
    │   │           │   ├── SpendingAnomaly.java        # Anomaly detection model
    │   │           │   ├── FinancialReport.java        # Comprehensive financial report
    │   │           │   └── ...                         # Other reporting & filter models
    │   │           │
    │   │           ├── service/
    │   │           │   ├── ExpenseCategorizer.java     # Strategy interface for categorizers
    │   │           │   ├── AICategorizer.java          # Offline-first rule-based NLP categorizer
    │   │           │   ├── ExpenseService.java         # Business logic & transaction orchestration
    │   │           │   ├── AnalyticsService.java       # Aggregation & statistical calculations
    │   │           │   └── FinancialIntelligenceService.java # Anomaly & trend detection
    │   │           │
    │   │           ├── util/
    │   │           │   ├── CSVExporter.java            # RFC-4180 compliant CSV export
    │   │           │   ├── ExcelExporter.java          # Apache POI spreadsheet generator
    │   │           │   ├── CurrencyAxisFormatter.java  # Robust JavaFX chart axis label formatter
    │   │           │   └── MoneyUtil.java              # BigDecimal rounding and formatting utility
    │   │           │
    │   │           └── view/
    │   │               └── MainDashboard.java          # JavaFX responsive UI controller
    │   │
    │   └── resources/
    │       └── styles/
    │           └── app.css                             # Modern theme stylesheet
    │
    └── test/
        └── java/
            └── com/
                └── expensetracker/
                    ├── model/
                    │   └── ExpenseTest.java            # Model validation tests
                    ├── service/
                    │   ├── AICategorizerTest.java      # NLP categorization tests
                    │   ├── AnalyticsServiceTest.java   # Data aggregation tests
                    │   ├── ExpenseServiceTest.java     # Business service layer tests
                    │   └── FinancialIntelligenceServiceTest.java # Anomaly & trend tests
                    └── util/
                        ├── CurrencyAxisFormatterTest.java # Axis formatting precision tests
                        ├── ExporterTest.java           # CSV & Excel file export tests
                        └── MoneyUtilTest.java          # BigDecimal math & currency tests
```

---

## 🔐 Configuration & Environment Variables

Database connectivity can be configured without modifying code using environment variables or JVM system properties:

| Variable | JVM Property | Default | Description |
|---|---|---|---|
| `DB_HOST` | `db.host` | `localhost` | MySQL hostname or IP address |
| `DB_PORT` | `db.port` | `3306` | MySQL port number |
| `DB_NAME` | `db.name` | `expense_tracker_db` | Database catalog name |
| `DB_USER` | `db.user` | `root` | Database username |
| `DB_PASSWORD` | `db.password` | `""` | Database password |

If connecting to MySQL fails or credentials are omitted, the application will automatically initialize the local `expense_tracker.db` SQLite database in the working directory.

---

## 🧪 Automated Testing

The project includes **48 automated unit and integration tests** built on JUnit 5:

```bash
# Run all unit and integration tests
mvn test
```

### Test Suite Breakdown:

- **[`ExpenseTest`](file:///c:/Users/SUBIKSHAN/.gemini/antigravity/scratch/ExpenseTrackerAI/src/test/java/com/expensetracker/model/ExpenseTest.java)** (6 tests): Validates amount constraints, date integrity, and input validation.
- **[`AICategorizerTest`](file:///c:/Users/SUBIKSHAN/.gemini/antigravity/scratch/ExpenseTrackerAI/src/test/java/com/expensetracker/service/AICategorizerTest.java)** (5 tests): Tests stemming, tokenization, phrase matching, and category confidence scores.
- **[`AnalyticsServiceTest`](file:///c:/Users/SUBIKSHAN/.gemini/antigravity/scratch/ExpenseTrackerAI/src/test/java/com/expensetracker/service/AnalyticsServiceTest.java)** (9 tests): Validates category spending distributions, time-series aggregation, and MoM calculations.
- **[`FinancialIntelligenceServiceTest`](file:///c:/Users/SUBIKSHAN/.gemini/antigravity/scratch/ExpenseTrackerAI/src/test/java/com/expensetracker/service/FinancialIntelligenceServiceTest.java)** (10 tests): Verifies anomaly detection algorithms, category spending growth trends, and advisory generation.
- **[`ExpenseServiceTest`](file:///c:/Users/SUBIKSHAN/.gemini/antigravity/scratch/ExpenseTrackerAI/src/test/java/com/expensetracker/service/ExpenseServiceTest.java)** (4 tests): Tests CRUD workflows, business rule validation, and error propagation.
- **[`CurrencyAxisFormatterTest`](file:///c:/Users/SUBIKSHAN/.gemini/antigravity/scratch/ExpenseTrackerAI/src/test/java/com/expensetracker/util/CurrencyAxisFormatterTest.java)** (8 tests): Ensures numeric and double-precision axis labels format accurately without JavaFX layout exceptions.
- **[`ExporterTest`](file:///c:/Users/SUBIKSHAN/.gemini/antigravity/scratch/ExpenseTrackerAI/src/test/java/com/expensetracker/util/ExporterTest.java)** (2 tests): Verifies that CSV and Excel `.xlsx` files are generated with accurate data and valid structures.
- **[`MoneyUtilTest`](file:///c:/Users/SUBIKSHAN/.gemini/antigravity/scratch/ExpenseTrackerAI/src/test/java/com/expensetracker/util/MoneyUtilTest.java)** (4 tests): Verifies standard monetary rounding, negative amount checks, and currency symbols.

---

## 🔒 GitHub Safety & Security Rules

To ensure privacy and avoid committing sensitive data, [`.gitignore`](file:///c:/Users/SUBIKSHAN/.gemini/antigravity/scratch/ExpenseTrackerAI/.gitignore) is strictly configured to exclude:

- **Local Databases**: `*.db`, `*.db-journal`, `expense_tracker.db`
- **User Financial Reports**: `*.csv`, `*.xlsx`
- **Secrets & Credentials**: `.env`, `*.env`, `*.log`
- **Compiled Binaries & Build Output**: `target/`, `*.class`
- **IDE & OS Metadata**: `.idea/`, `*.iml`, `.vscode/`, `.DS_Store`, `Thumbs.db`

> [!IMPORTANT]
> Never commit real database passwords or external API keys to git. Always use environment variables or JVM system properties.

---

## 🚀 Getting Started

### Prerequisites

- **Java Development Kit (JDK) 21** or higher.
- **Apache Maven 3.8** or higher.
- *(Optional)* **MySQL 8.x** running locally (if MySQL is not running, the app automatically switches to SQLite).

### Installation & Run

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Subikshan/ExpenseTrackerAI.git
   cd ExpenseTrackerAI
   ```

2. **Compile the application:**
   ```bash
   mvn clean compile
   ```

3. **Run the automated test suite:**
   ```bash
   mvn test
   ```

4. **Launch the application:**
   ```bash
   mvn javafx:run
   ```

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
