# Dairy ERP - Milk Collection Management System

Offline desktop Dairy / Milk Collection Management System built with **Core Java**,
**Java Swing / AWT**, **JDBC** and **SQLite**. Single user, fully offline,
no Maven / Gradle / Spring required.

## Tech Stack
- Java 17+ (verified with Java 21)
- Java Swing / AWT for the desktop UI
- JDBC with SQLite via the xerial `sqlite-jdbc` driver (`lib/sqlite-jdbc-3.45.3.0.jar`)
- `java.util.logging` for application logs

## Folder Structure
```
src/dairy/erp/
├── Main.java                 entry point (login → main frame)
├── config/                   AppConfig (paths, names)
├── database/                 DatabaseManager + DatabaseInitializer
├── model/                    POJOs (Customer, MilkCollection, Payment, …)
├── dao/                      JDBC data access (PreparedStatement)
├── service/                  business logic (rate calc, reports, backup, …)
├── ui/                       Swing screens (Login, Main, panels, dialogs)
├── report/                   (report computation lives in ReportService)
└── util/                     CSV, currency, date, validation, print, hash, log
data/     runtime SQLite database (data/dairy.db)
backup/   database backups
export/   CSV exports
logs/     application log (logs/dairy-erp.log)
lib/      sqlite-jdbc + slf4j jars
```

## Running from the command line
```bash
./compile.sh          # compile sources into out/
./run.sh              # run the GUI
./run.sh --init       # run only database initialisation (verification mode)
```

## Running from an IDE
- **Eclipse**: create a Java project rooted at this directory, add
  `lib/sqlite-jdbc-3.45.3.0.jar`, `lib/slf4j-api-1.7.36.jar` and
  `lib/slf4j-nop-1.7.36.jar` to the Build Path, and run `dairy.erp.Main`.
- **IntelliJ IDEA**: open as a plain Java project, mark `src` as Sources Root,
  add the jars under `lib/` to the module classpath, run `dairy.erp.Main`.

## Default login (seeded on first startup)
- Username: `admin`
- Password: `admin123`  (changeable from Settings / Help → Change Password)

## Features
- Login (salted SHA-256 hashed passwords), logout, password change
- Dashboard with today's collection cards and quick actions
- Customer / Member master with automatic `CUST001`-style code generation,
  search, safe delete (inactive archival), print
- Milk collection entry — keyboard-first, automatic FAT/SNF rate lookup and
  amount calculation, duplicate-entry prevention, per-record rate snapshot
- Rate chart management with configurable rate service (exact band + nearest-FAT fallback)
- Payments, customer ledger & statement
- Daily / Weekly / Monthly / Customer / Payment reports with weighted averages
- CSV import (transactional, row validation) & CSV export
- SQLite backup & restore
- `java.awt.print` printing for lists and reports
- F1 = Customers, F2 = Rate Chart, F3 = Reports (Swing keyboard bindings)

## Database
Created automatically on first startup (no manual SQL). Tables: `users`,
`customers`, `rate_chart`, `milk_collection`, `payments`, `settings`, `app_log`.
Foreign keys enabled via `PRAGMA foreign_keys = ON`.