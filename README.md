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
* Default admin accounts seeded on first startup

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
* **Keyboard-first form**: pressing Enter moves to the next field — Code → Name → Mobile → Address → Village → Milk Type → Status → Registration Date → Remarks → Save

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

The milk collection screen is designed for **keyboard-first operation** for faster daily data entry (Enter flows from date → shift → customer code → quantity → FAT → rate → remarks → **Save & Print**).

### Milk Types

* Cow Milk
* Buffalo Milk
* Mixed Milk

### Collection Features

* Automatic FAT/SNF rate lookup
* Automatic amount calculation
* Duplicate-entry prevention (same customer cannot be recorded twice for the same date & shift)
* Per-record rate snapshot
* Collection history (searchable by date range, customer, shift)
* **FAT entry for any milk type from 1 to 15** (decimals allowed, e.g. `7.25`) — no per-milk-type restriction
  * Typing a value **below 1 or above 15** turns the FAT field border **red** immediately
  * Saving with an out-of-range FAT shows: *"FAT must be between 1 and 15."*
* **Auto rate/amount**: typing FAT automatically fills the rate (from the rate chart) and the amount
* **Smart rate lookup**: exact FAT/SNF band match with a **nearest-FAT fallback** when there is no exact rule

### Collection Records Table

* Clean, modern grid with a **dark theme-coloured header**, zebra-striped rows and fixed column widths
* **Grand Totals bar** at the bottom: record count, unique customer count, **Avg FAT**, **Total Qty (L)**, **Avg Rate** and **Total Amount**
* **Generate Detailed Report** button below the table exports exactly what is on screen to a CSV — the app asks **where you would like to save it** before writing

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

The Rate Chart module allows the user to configure milk rates for different milk types and quality ranges (min/max FAT, min/max SNF, rate, effective from/to and active flag).

Example:

```text
Milk Type    FAT Range    SNF Range    Rate
------------------------------------------------
Cow          3.0 - 5.0    8.0 - 9.0    ₹XX.XX
Buffalo      5.0 - 8.0    8.5 - 10.0   ₹XX.XX
Mixed        4.0 - 6.0    8.0 - 9.5    ₹XX.XX
```

The actual rates are configurable from the application.

### Rate Chart CSV Import / Export

* **Template button** generates a ready-to-fill CSV template (with sample rows) so the columns are always right
* **Export** saves the current rate chart to CSV from the Export dropdown -> **Rate Chart**
* **Import** validates every row (milk type, FAT/SNF bounds, rate, dates, active flag) and is **transactional** — either every row is imported or nothing changes, with per-row errors reported
* On import you can choose to **replace the whole chart** or **add on top**
* Changes are pushed **live** to the Rate Chart screen and anywhere else that reads rates — **no restart needed**


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
* **Keyboard-first New Payment form** — Enter flows from date → mode → customer code → amount → reference → remarks → **Save & Print**

The ledger provides a consolidated view of milk collection amounts and payments for each customer (milk supplied with rates, payments made, and the running / outstanding balance).

---

# 📊 Dashboard

The dashboard provides a quick overview of daily dairy operations.

It can display:

* Today's milk collection
* **Today's Summary** cards (customers, total milk, total amount)
* **Collection Shifts** — morning and evening quantity cards
* **Milk Type Breakdown** — Cow / Buffalo / Mix quantities with animated milk-fill vessels
* 7-day earnings sparkline (mini area chart)
* Quick actions

The dashboard **re-colours itself live** to match the selected application theme.

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

Reports support **weighted-average calculations** where applicable (e.g. weighted average FAT).

---

# 📥 CSV Import & Export (Data Tools)

The **Data Tools** screen centralises all data movement — Import, Export, Templates, Backup and Restore. Export supports **Customers, Milk Collections, Payments, Rate Chart, Customer Ledger, Daily Report and Monthly Report**.

### CSV Import

* Transactional import (all-or-nothing)
* Row-level validation with per-row error messages
* Bread-ready CSV **templates** (Milk Collection, Rate Chart) so the columns are always correct
* Rate Chart import offers **replace** or **append**

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
* Milk collection slips (member receipts — **Save & Print** in each entry screen)
* Payment receipts
* Customer statements
* Other application reports

---

# 🎨 Appearance — Application Themes

Choose from **four colour themes** in Application Settings:

| Theme        | Vibe                        |
| ------------ | --------------------------- |
| Ocean Teal   | Calm, professional blue-green |
| Royal Purple | Rich, deep purple           |
| Forest Green | Fresh, natural green        |
| Sunset Orange| Warm, energetic orange      |

Selected theme colours flow through the **menu bar, dashboard figures, shift cards, table headers, zebra row stripes and buttons** — applied **live** the moment you save, no restart needed.

---

# ⌨️ Keyboard Shortcuts

The application provides function-key shortcuts for frequently used modules:

| Shortcut | Module                      |
| -------- | --------------------------- |
| `F1`     | Dashboard                   |
| `F2`     | Customer Management         |
| `F3`     | Rate Chart                  |
| `F4`     | New Collection              |
| `F5`     | New Payment                 |
| `F6`     | Customer Ledger             |
| `F7`     | Reports                     |
| `F8`     | Data Tools (Import/Export/Backup) |
| `F9`     | Application Settings        |
| `Esc`    | Back to Dashboard           |

### Enter-Key Form Navigation

The Customer Details, New Collection and New Payment forms are fully **keyboard-first**. Pressing Enter in any input moves focus to the next logical field; Enter on the Save / Save & Print button submits the form, and the confirmation popup's OK button is auto-focused so Enter closes it and readies the form for the next entry.


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
│           └── util/
│               ├── CSV
│               ├── Currency
│               ├── Date
│               ├── Validation
│               ├── Print
│               ├── Hash
│               ├── Theme
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

Admin accounts are seeded automatically during the first startup:

```text
 
Username: siyarampk   Password: Siyarampk
```

**Important:** Change the default passwords after the first login.

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

This separation keeps UI, business logic, and database operations organized and easier to maintain. A small in-process event bus (`AppBus`) lets shared data (dairy name, theme, settings, rate chart, customer changes) propagate **live** to every open screen.


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

# 🔒 Security & Data Protection

* Passwords are stored using **salted SHA-256 hashing** — never in plain text
* Bilingual English / Hindi UI, saved per user
* Application settings and theme changes propagate **live** through the event bus — no restart needed for most changes
* Regular **backup** is recommended (one click in Data Tools)

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

The default login passwords should be changed after the first startup.

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
13. Use `AppBus` to propagate shared-data changes so screens update live.
14. Reuse `UIUtil` shared styling (tables, fields, buttons, themes) for a consistent look.


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
