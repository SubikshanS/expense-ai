# 🤖 AI-Powered Full-Stack Expense Tracker

A JavaFX-based desktop expense management application built with **Java, JDBC, MySQL/SQLite, Maven, and rule-based NLP** for automatic expense categorization.

The application allows users to record expenses, automatically categorize descriptions, search and filter transactions, view spending analytics, and export expense data to CSV or Excel.

---

## ✨ Features

### 🤖 1. AI Auto-Categorization

The application analyzes the expense description while the user types and suggests a category automatically.

**Examples:**

| Expense Description | Predicted Category |
|---|---|
| `bus ticket` | Transport |
| `restaurant dinner` | Food & Dining |
| `apple` | Shopping |
| `electricity bill` | Housing & Utilities |
| `movie` | Entertainment |
| `medicine` | Health & Medical |

The categorizer uses:

- Text normalization
- Tokenization
- Keyword matching
- Phrase matching
- Weighted scoring
- Category rules

> **Note:** This is a lightweight **rule-based NLP system**, not a machine-learning model.

---

### 💰 2. Expense Management

Users can:

- Add expenses
- Edit selected expenses
- Delete expenses
- Clear input fields
- Select a date
- Select a category manually
- Get automatic category suggestions
- Store expense descriptions and amounts

Each expense contains:

```text
ID
Amount
Category
Description
Date
```

---

### 🔎 3. Search, Filtering & Sorting

The dashboard provides:

- Description/category search
- Month filtering
- Date sorting
- Amount sorting

The selected filters also affect the displayed dashboard analytics.

---

### 📊 4. Dashboard Analytics

The dashboard displays:

- **Total Spending**
- **Top Category**
- **Number of Transactions**
- **Average Expense**

#### 🥧 Spending Breakdown by Category

A Pie Chart displays how spending is distributed across expense categories.

#### 📅 Monthly Spending

A Bar Chart displays spending grouped by month.

The monthly chart displays the latest six available months.

---

### 📄 5. CSV Export

Expense data can be exported to a `.csv` file.

The CSV export includes:

- ID
- Date
- Category
- Description
- Amount
- Summary report
- Grand total
- Category totals

---

### 📗 6. Excel Export

Expense data can also be exported to `.xlsx`.

The Excel report contains:

- Formatted headers
- Expense records
- Currency formatting
- Category totals
- Grand total
- Summary information
- Automatically sized columns

**Apache POI** is used for Excel generation.

---

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| **Java 21** | Main programming language |
| **JavaFX** | Desktop graphical user interface |
| **JDBC** | Database connectivity |
| **MySQL 8** | Primary relational database |
| **SQLite** | Embedded fallback database |
| **Maven** | Project and dependency management |
| **Apache POI** | Excel export |
| **Java Collections** | Data processing and analytics |

---

## 🏗️ Architecture

The project follows a simple layered structure:

```text
                    JavaFX UI
                        │
                        ▼
                 MainDashboard
                  │     │     │
                  │     │     └──────────────┐
                  │     │                    │
                  ▼     ▼                    ▼
          AICategorizer  ExpenseDAO       Exporters
               │             │            │      │
               ▼             ▼            ▼      ▼
        Category        DBConnection    CSV     Excel
        Prediction          │                  Export
                            │
                     ┌──────┴──────┐
                     ▼             ▼
                   MySQL      SQLite Fallback
```

### Application Flow

```text
User Input
    │
    ▼
MainDashboard
    │
    ├── AICategorizer
    │       │
    │       └── Category Prediction
    │
    ├── ExpenseDAO
    │       │
    │       └── CRUD Operations
    │
    └── Exporters
            ├── CSVExporter
            └── ExcelExporter
```

---

## 📁 Project Structure

```text
ExpenseTrackerAI/
│
├── pom.xml
├── schema.sql
├── README.md
│
└── src/
    └── main/
        └── java/
            └── com/
                └── expensetracker/
                    │
                    ├── MainApp.java
                    │
                    ├── config/
                    │   └── DBConnection.java
                    │
                    ├── dao/
                    │   └── ExpenseDAO.java
                    │
                    ├── model/
                    │   └── Expense.java
                    │
                    ├── service/
                    │   └── AICategorizer.java
                    │
                    ├── util/
                    │   ├── CSVExporter.java
                    │   └── ExcelExporter.java
                    │
                    └── view/
                        └── MainDashboard.java
```

---

## 🗄️ Database

The application supports two database options.

### MySQL

**MySQL is the primary database option.**

The application uses **JDBC** and **prepared statements** for database operations.

Main table:

```text
expenses
```

Columns:

```text
id
amount
category
description
date
created_at
```

### SQLite

If a MySQL connection cannot be established, the application can use an embedded SQLite database.

The local SQLite database file is:

```text
expense_tracker.db
```

This file should **not** be committed to GitHub.

---

## 🔐 Database Configuration

The application reads database settings from environment variables or Java system properties.

Supported settings:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USER
DB_PASSWORD
```

Default local configuration:

```text
Host:     localhost
Port:     3306
Database: expense_tracker_db
User:     root
```

The database password should be supplied through configuration and should **never be committed to the repository**.

---

## 📊 Core Data Structures

The project demonstrates Java Collections in practical use.

### ObservableList

```java
ObservableList<Expense>
```

Used for the JavaFX expense table and reactive filtering.

### HashMap

```java
HashMap<String, Double>
```

Used for calculating category spending totals.

### TreeMap

```java
TreeMap<YearMonth, Double>
```

Used for chronological monthly spending aggregation.

### Sorting

Expense records can be sorted by:

- Date
- Amount

---

## 🧠 NLP Categorization

The categorization process follows a lightweight text-processing pipeline:

```text
Expense Description
        │
        ▼
Text Normalization
        │
        ▼
Tokenization
        │
        ▼
Keyword / Phrase Matching
        │
        ▼
Weighted Category Scoring
        │
        ▼
Predicted Category
```

### Example

```text
"bus ticket"
      │
      ▼
Normalized Text
      │
      ▼
Token / Phrase Matching
      │
      ▼
Transport Keyword Detected
      │
      ▼
Transport
```

The system is designed as a lightweight NLP implementation suitable for a Java mini-project.

---

## 📤 Export Architecture

### CSV Export

```text
Expense List
     │
     ▼
CSVExporter
     │
     ▼
.csv File
```

### Excel Export

```text
Expense List
     │
     ▼
ExcelExporter
     │
     ▼
Apache POI
     │
     ▼
.xlsx File
```

---

## ⚙️ Requirements

Before running the project, make sure you have:

- **JDK 21 or newer**
- **Maven 3.8 or newer**
- **MySQL 8.x** — optional because SQLite is available as a fallback
- Internet access during the first Maven dependency download

JavaFX dependencies are managed through Maven.

---

## ▶️ How to Run

### 1. Clone the Repository

```bash
git clone https://github.com/Subikshan/ExpenseTrackerAI.git
cd ExpenseTrackerAI
```

### 2. Open the Project

Open the project folder in IntelliJ IDEA, VS Code, or another Java IDE.

Make sure the project contains:

```text
pom.xml
src/
schema.sql
README.md
```

### 3. Compile the Project

From the project root:

```bash
mvn clean compile
```

### 4. Run the Application

```bash
mvn javafx:run
```

The application will start as a JavaFX desktop application.

---

## 🗃️ Database Schema

The project includes:

```text
schema.sql
```

The main MySQL table is:

```sql
CREATE TABLE IF NOT EXISTS expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    amount DOUBLE NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🔒 GitHub Safety

The following files and directories should **not** be committed:

```text
target/
*.class
*.db
*.db-journal
.idea/
*.iml
.vscode/
.settings/
.project
.classpath
.DS_Store
Thumbs.db
```

The SQLite database file contains local application data and should remain outside the Git repository.

### Never commit:

- Database passwords
- API keys
- Personal credentials
- Local database files
- IDE-specific configuration

---

## 🎯 Project Objective

This project demonstrates how a Java desktop application can combine:

- Object-Oriented Programming
- JavaFX GUI development
- JDBC database connectivity
- SQL CRUD operations
- Basic NLP
- Java Collections and data processing
- Data visualization
- File handling
- CSV generation
- Excel generation
- Maven dependency management

The project is designed as a practical Java mini-project combining **desktop application development, database programming, NLP-based categorization, data visualization, and expense analytics**.

---

## 🚀 Future Improvements

Possible future improvements include:

- More advanced expense categorization
- Improved NLP accuracy
- Additional analytics
- More detailed reports
- Additional filtering options
- More export/report formats

---

## 📌 Project Status

**Current Status:** Functional JavaFX expense management application with database persistence, NLP-based categorization, analytics, filtering, and CSV/Excel export.