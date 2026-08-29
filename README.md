# Dairy ERP - Milk Collection Management System

**SRS Dairy ERP System** is an offline desktop-based **Dairy / Milk Collection Management System** developed using **Core Java, Java Swing, AWT, JDBC, and SQLite**.

The system provides an end-to-end solution for dairy milk collection operations, including customer management, milk collection, FAT/SNF-based rate calculation, rate chart management, payments, customer ledger, reports, CSV import/export, printing, database backup/restore, and user login.

> **100% Offline Desktop Application**
> **Single User**
> **Core Java + Swing/AWT**
> **JDBC + SQLite**
> **No Spring Boot**
> **No Maven / Gradle**

---

## Overview

SRS Dairy ERP System is designed for dairy booths and milk collection centers that need a simple, reliable, and fully offline application for managing daily milk collection operations.

The application maintains customer records, milk collection details, milk quality information such as FAT and SNF, configurable milk rates, payments, customer ledgers, reports, and database backups.

The application is designed for **single-user operation** and does not require an internet connection or cloud services.

---

## Tech Stack

| Component             | Technology           |
| --------------------- | -------------------- |
| Programming Language  | Core Java            |
| Java Version          | Java 17+             |
| Verified With         | Java 21              |
| Desktop UI            | Java Swing / AWT     |
| Database Connectivity | JDBC                 |
| Database              | SQLite               |
| SQLite Driver         | xerial `sqlite-jdbc` |
| Logging               | `java.util.logging`  |
| Application Type      | Offline Desktop      |
| User Model            | Single User          |
| Build System          | Shell scripts / IDE  |
| Maven                 | Not Used             |
| Gradle                | Not Used             |
| Spring Boot           | Not Used             |

### Libraries

The SQLite JDBC driver is included locally:

```text
lib/sqlite-jdbc-3.45.3.0.jar
```

Additional libraries:

```text
lib/slf4j-api-1.7.36.jar
lib/slf4j-nop-1.7.36.jar
```

---

# Features

## 🔐 Login & Security

* User login
* Logout
* Password change
* Salted SHA-256 password hashing
* Single-user access
* Default admin account seeded on first startup

## 👤 Customer / Member Management

* Add customer/member
* Update customer
* Edit customer
* Search customer
* Delete customer
* Safe delete using inactive archival
* Automatic customer code generation
* Customer codes such as `CUST001`
* Customer transaction history
* Customer printing

## 🥛 Milk Collection

Record daily milk collection with:

* Customer/member
* Collection date
* Morning / Evening session
* Milk type
* Milk quantity
* FAT
* SNF
* Rate
* Total amount

The milk collection screen is designed for **keyboard-first operation** for faster daily data entry.

### Milk Types

* Cow Milk
* Buffalo Milk
* Mixed Milk

### Collection Features

* Automatic FAT/SNF rate lookup
* Automatic amount calculation
* Duplicate-entry prevention
* Per-record rate snapshot
* Collection history

---

# 🧪 FAT / SNF Rate Calculation

The system supports configurable milk rate charts based on milk quality.

Rate calculation can use:

* Milk type
* FAT
* SNF
* Minimum FAT
* Maximum FAT
* Minimum SNF
* Maximum SNF
* Applicable amount

The rate service supports:

* Exact rate-band matching
* Nearest-FAT fallback

This allows the dairy to configure its own milk rate structure rather than hard-coding rates into the application.

---

# 💰 Rate Chart Management

The Rate Chart module allows the user to configure milk rates for different milk types and quality ranges.

Example:

```text
Milk Type    FAT Range    SNF Range    Rate
------------------------------------------------
Cow          3.0 - 5.0    8.0 - 9.0    ₹XX.XX
Buffalo      5.0 - 8.0    8.5 - 10.0   ₹XX.XX
Mixed        4.0 - 6.0    8.0 - 9.5    ₹XX.XX
```

The actual rates are configurable from the application.

---

# 💵 Payments & Customer Ledger

The payment module provides:

* Customer payments
* Payment history
* Customer ledger
* Customer statement
* Outstanding amount
* Payment reports
* Transaction tracking

The ledger provides a consolidated view of milk collection amounts and payments for each customer.

---

# 📊 Dashboard

The dashboard provides a quick overview of daily dairy operations.

It can display:

* Today's milk collection
* Total milk quantity
* Collection amount
* Customer count
* Payment summary
* Outstanding amount
* Quick actions
* Recent activities

---

# 📈 Reports

The system provides multiple operational reports:

* Daily collection report
* Weekly collection report
* Monthly collection report
* Customer-wise collection report
* Payment report
* Customer statement
* Milk type report
* FAT/SNF information
* Collection amount report
* Outstanding report

Reports support **weighted-average calculations** where applicable.

---

# 📥 CSV Import & Export

The application supports CSV data operations.

### CSV Import

* Transactional import
* Row-level validation
* Invalid-row detection
* Safe database insertion

### CSV Export

Export relevant dairy data into CSV files for:

* Analysis
* Backup
* External processing
* Record keeping

---

# 💾 Database Backup & Restore

The application supports SQLite database backup and restore.

### Runtime Database

```text
data/dairy.db
```

### Backup Location

```text
backup/
```

The backup functionality helps protect important dairy collection and customer records.

Regular backups are recommended.

---

# 🖨️ Printing

The application supports desktop printing using:

```text
java.awt.print
```

Printing can be used for:

* Customer lists
* Milk collection reports
* Payment reports
* Customer statements
* Other application reports

---

# ⌨️ Keyboard Shortcuts

The application provides keyboard shortcuts for frequently used modules:

| Shortcut | Module     |
| -------- | ---------- |
| `F1`     | Customers  |
| `F2`     | Rate Chart |
| `F3`     | Reports    |

The milk collection workflow is also designed for keyboard-first data entry.

---

# 📁 Folder Structure

```text
SRS-Dairy-ERP-System/
│
├── src/
│   └── dairy/
│       └── erp/
│           ├── Main.java
│           │
│           ├── config/
│           │   └── AppConfig
│           │
│           ├── database/
│           │   ├── DatabaseManager
│           │   └── DatabaseInitializer
│           │
│           ├── model/
│           │   ├── Customer
│           │   ├── MilkCollection
│           │   ├── Payment
│           │   └── ...
│           │
│           ├── dao/
│           │   └── JDBC Data Access
│           │
│           ├── service/
│           │   ├── Rate Calculation
│           │   ├── Reports
│           │   ├── Backup
│           │   └── Business Logic
│           │
│           ├── ui/
│           │   ├── Login
│           │   ├── Main Frame
│           │   ├── Panels
│           │   └── Dialogs
│           │
│           ├── report/
│           │   └── Report Computation
│           │
│           └── util/
│               ├── CSV
│               ├── Currency
│               ├── Date
│               ├── Validation
│               ├── Print
│               ├── Hash
│               └── Log
│
├── data/
│   └── dairy.db
│
├── backup/
│
├── export/
│
├── logs/
│   └── dairy-erp.log
│
├── lib/
│   ├── sqlite-jdbc-3.45.3.0.jar
│   ├── slf4j-api-1.7.36.jar
│   └── slf4j-nop-1.7.36.jar
│
├── compile.sh
├── run.sh
├── .gitignore
└── README.md
```

---

# 🗄️ Database

The SQLite database is created automatically during the first application startup.

No manual SQL execution is required.

### Database

```text
data/dairy.db
```

### Tables

The application uses the following primary tables:

```text
users
customers
rate_chart
milk_collection
payments
settings
app_log
```

### Foreign Keys

SQLite foreign-key enforcement is enabled using:

```sql
PRAGMA foreign_keys = ON;
```

---

# 🔑 Default Login

The default user is seeded automatically during the first startup.

```text
Username: admin
Password: admin123
```

**Important:** Change the default password after the first login.

Password can be changed from:

```text
Settings / Help → Change Password
```

---

# 🚀 Running from Command Line

The project includes shell scripts for compiling and running the application.

### Compile

```bash
./compile.sh
```

This compiles the Java source files into:

```text
out/
```

### Run Application

```bash
./run.sh
```

This starts the graphical user interface.

### Database Initialization

To run only database initialization / verification:

```bash
./run.sh --init
```

---

# 💻 Running from an IDE

## Eclipse

1. Create a Java project rooted at the repository directory.
2. Add the following JAR files to the Build Path:

```text
lib/sqlite-jdbc-3.45.3.0.jar
lib/slf4j-api-1.7.36.jar
lib/slf4j-nop-1.7.36.jar
```

3. Set the source directory appropriately.
4. Run:

```text
dairy.erp.Main
```

---

## IntelliJ IDEA

1. Open the repository as a plain Java project.
2. Mark:

```text
src
```

as the **Sources Root**.
3. Add the JAR files under:

```text
lib/
```

to the module classpath.
4. Run:

```text
dairy.erp.Main
```

---

# 🏗️ Application Architecture

The application follows a modular desktop application architecture.

```text
                   ┌─────────────────────┐
                   │       Login         │
                   └──────────┬──────────┘
                              │
                              ▼
                   ┌─────────────────────┐
                   │      Dashboard      │
                   └──────────┬──────────┘
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
          ▼                   ▼                   ▼
   ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
   │  Customers   │    │     Milk     │    │  Rate Chart  │
   │  Management  │    │  Collection  │    │  Management  │
   └──────┬───────┘    └──────┬───────┘    └──────┬───────┘
          │                   │                   │
          └───────────────────┼───────────────────┘
                              │
                              ▼
                   ┌─────────────────────┐
                   │     Payments &      │
                   │   Customer Ledger   │
                   └──────────┬──────────┘
                              │
                              ▼
                   ┌─────────────────────┐
                   │       Reports       │
                   └─────────────────────┘
```

### Application Layers

```text
UI Layer
   ↓
Service Layer
   ↓
DAO Layer
   ↓
JDBC
   ↓
SQLite Database
```

This separation keeps UI, business logic, and database operations organized and easier to maintain.

---

# 🌐 Offline Architecture

The application is intentionally designed to operate completely offline.

```text
┌──────────────────────────────────────────┐
│          SRS Dairy ERP System            │
│                                          │
│  Java Swing / AWT                        │
│           ↓                              │
│  Business Services                       │
│           ↓                              │
│  JDBC                                    │
│           ↓                              │
│  SQLite                                  │
│           ↓                              │
│  data/dairy.db                           │
└──────────────────────────────────────────┘

             NO INTERNET REQUIRED
```

No cloud server or external backend is required for normal operation.

---

# 🌿 Git Branch Strategy

The repository can follow the following branch structure:

| Branch      | Purpose                      |
| ----------- | ---------------------------- |
| `main`      | Stable production-ready code |
| `develop`   | Development and integration  |
| `feature/*` | New feature development      |
| `bugfix/*`  | Bug fixes                    |
| `release/*` | Release preparation          |

### Feature Branch Example

```bash
git checkout -b feature/customer-management
```

Commit changes:

```bash
git add .
git commit -m "Add customer management module"
```

Push the branch:

```bash
git push -u origin feature/customer-management
```

---

# 🔒 Security & Data Protection

Do not commit sensitive or production information to GitHub.

Avoid committing:

* Passwords
* Database credentials
* API keys
* Private configuration
* Production database files
* Customer personal information
* Financial information
* Local log files

The default login password should be changed after the first startup.

---

# 📦 Project Requirements

### Minimum

```text
Java 17+
```

### Recommended

```text
Java 21
```

The application has been verified with Java 21.

No installation of the following is required:

```text
Spring Boot
Maven
Gradle
Node.js
Web Server
Cloud Server
```

---

# 🛠️ Development Guidelines

When adding new features:

1. Keep Swing UI code separate from business logic.
2. Keep database operations inside DAO classes.
3. Keep business calculations inside service classes.
4. Use `PreparedStatement` for JDBC operations.
5. Validate user input before saving.
6. Handle database exceptions properly.
7. Avoid hard-coded configuration values.
8. Use meaningful class and method names.
9. Maintain backward compatibility with existing database data.
10. Test milk-rate calculations carefully before release.
11. Do not commit sensitive data.
12. Create a database backup before major database changes.

---

# 🔮 Future Enhancements

Potential future enhancements include:

* Advanced dashboard analytics
* SMS notifications
* WhatsApp notifications
* Automated payment reminders
* Advanced financial reports
* QR code support
* Barcode support
* Multi-user support
* Role-based access control
* Cloud backup
* Mobile application
* Online synchronization
* Advanced dairy accounting
* Enhanced inventory management
* Supplier management
* Expense management

---

# 📌 Project Status

**Status:** Active Development

SRS Dairy ERP System is being developed as a complete offline solution for dairy milk collection and day-to-day dairy booth operations.

---

# 📄 License

This project is intended for private and business use.

Copyright © SRS Dairy ERP System.

---

# 👨‍💻 Author

**SRS Dairy ERP System**

**Technology**

```text
Core Java
Java Swing
Java AWT
JDBC
SQLite
```

**Application Type**

```text
Offline Desktop Dairy ERP
```
