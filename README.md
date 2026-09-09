# APU-ASC: Automotive Service Centre Management System

A full-featured **Java Swing desktop application** for managing an automotive service centre.  
Built as an Object-Oriented Programming (OOP) course project at Asia Pacific University (APU).

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [User Roles](#user-roles)
- [Architecture](#architecture)
- [OOP Design](#oop-design)
- [Data Persistence](#data-persistence)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [How to Run](#how-to-run)
- [Default Login](#default-login)
- [Dependencies](#dependencies)
- [License](#license)

---

## Overview

APU-ASC is a multi-role desktop management system for an automotive service centre. It handles the complete service lifecycle (from appointment booking and technician assignment through service execution to payment and receipt generation), along with a VIP loyalty program, customer feedback system, vehicle health records, inventory management, and employee scheduling.

- **55 Java source files**, ~19,700 lines of code
- **No external database**, since all data is persisted in flat text files (pipe-separated)
- **Pure Java Swing GUI** with custom rendering, rounded panels, avatar system, and themeable backgrounds
- **PDFBox** integration for PDF report/receipt generation

---

## Key Features

### Appointment Management
- Full lifecycle: `SCHEDULED → AWAITING_PAYMENT → COMPLETED` (or `MISSED` if past date)
- Normal Service (1 hour) and Major Service (3 hours) types
- Technician assignment by counter staff
- Auto-detection of missed appointments on startup

### VIP & Rewards Program
- 4-tier loyalty system: **NONE → BRONZE → GOLD → BLACKGOLD**
- Points earned at 1 pt per RM spent
- Tier-based service discounts (GOLD: 2%, BLACKGOLD: 5%)
- Automatic monthly coupon issuance (BRONZE: RM 20, BLACKGOLD: RM 50)
- Upgrade-aware coupon logic: tier-up triggers immediate new-tier coupon

### Payment & Receipts
- Counter staff collect payments for completed services
- Wallet top-up system for customers
- Formatted text receipts with full breakdown
- PDF export via Apache PDFBox

### Vehicle Health Records
- Technician records vehicle condition after each service
- Historical health tracking per vehicle
- Linked to appointment and technician feedback

### Customer Feedback & Comments
- Star ratings for counter staff and technicians
- Text comments per completed appointment
- Notification system, so technicians get notified when rated

### Inventory (Car Parts)
- Manager manages part stock levels
- Technician logs parts consumed per appointment
- Consumption history retained even after appointment deletion (Association)

### Employee Scheduling
- Manager creates draft work schedules
- Draft → Published workflow (staff only see published schedules)
- Backward-compatible file format migration

### Customer Support Chatbot
- In-app support dialog accessible from customer dashboard
- 8 topic categories with sub-questions
- Smart follow-up: asked questions are hidden to prevent repeats
- Free-text keyword matching for natural queries
- Bug report submission flow

### UI / UX
- Custom rounded borders and bubble chat rendering
- Avatar system with gender-based defaults and custom image upload
- Customizable dashboard background images per user
- Look & Feel: FlatLaf (if available) → Nimbus → Metal (graceful degradation)
- Role-specific dashboards with tabbed navigation

---

## User Roles

| Role | Dashboard | Key Capabilities |
|------|-----------|-----------------|
| **Manager** | `ManagerDashboard` | User management, employee scheduling, parts inventory, service pricing, feedback review, reports |
| **Counter Staff** | `CounterStaffDashboard` | Appointment creation/assignment, payment collection, scheduling, customer management, top-ups |
| **Technician** | `TechnicianDashboard` | Appointment execution, vehicle health records, parts consumption, service feedback, analytics |
| **Customer** | `CustomerDashboard` | Book appointments, view history, VIP/wallet, vehicle health, feedback/comments, support chatbot |

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Main.java                         │
│         (L&F init, data bootstrap, login)            │
├─────────────────────────────────────────────────────┤
│              GUI Layer (javax.swing)                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐ │
│  │ Customer │ │ Counter  │ │Technician│ │ Manager │ │
│  │  Pages   │ │  Pages   │ │  Pages   │ │  Pages  │ │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬────┘ │
│       └──────────┬──┴──────────────┘        │       │
│              UIUtils (shared)                │       │
│     (avatars, backgrounds, rounded borders)  │       │
├─────────────────────────────────────────────┤       │
│              Model Layer                      │       │
│  User (abstract)                             │       │
│    ├── Manager                               │       │
│    ├── CounterStaff                          │       │
│    ├── Technician                            │       │
│    └── Customer                              │       │
│  Appointment, Payment, VipAccount, Coupon,   │       │
│  CarPart, VehicleHealthRecord, Feedback,     │       │
│  Comment, Notification, ServicePrice, ...    │       │
├──────────────────────────────────────────────┤       │
│           Persistence Layer                   │       │
│            FileManager                        │       │
│   (flat-file I/O, reference resolution,      │       │
│    auto-missed detection, coupon refresh)     │       │
├──────────────────────────────────────────────┤       │
│              data/ (18 .txt files)            │       │
│   data/Picture/ (images, icons, avatars)     │       │
└──────────────────────────────────────────────┘
```

---

## OOP Design

The project demonstrates all four OOP pillars plus three relationship types:

### Four Pillars

| Pillar | Implementation |
|--------|---------------|
| **Abstraction** | `User` is an abstract class with abstract methods `getDisplayInfo()` and `openDashboard()`, and each subclass implements its own dashboard |
| **Inheritance** | `Manager`, `CounterStaff`, `Technician`, `Customer` all extend `User` |
| **Encapsulation** | All fields are `private`/`protected` with public getters/setters; `transient` marker used to exclude runtime object references from serialization |
| **Polymorphism** | `openDashboard()` is overridden, so calling `user.openDashboard()` opens the correct dashboard based on runtime type |

### Relationships

| Type | Example | Meaning |
|------|---------|---------|
| **Composition** | `Customer ◆── VipAccount` | VIP account cannot exist without the customer; cascade-deleted |
| **Composition** | `Customer ◆── Coupon` | Coupons are issued to a specific customer |
| **Composition** | `Appointment ◆── Feedback` | Feedback is part-of the appointment |
| **Composition** | `User ◆── Notification` | Notifications belong to and die with the user |
| **Aggregation** | `Customer ◇── Appointment` | Appointments can exist independently (historical records) |
| **Association** | `Appointment ── Payment` | Payment references an appointment via object reference at runtime |

### Runtime Reference Resolution

File persistence uses ID strings (e.g., `customerId`), but at runtime `FileManager.resolveReferences()` populates actual object references (e.g., `appointment.getCustomer()` returns the `Customer` object, not just the ID). This is a key design decision that keeps the data layer simple while enabling rich OOP interactions in the GUI layer.

---

## Data Persistence

All data is stored in **pipe-separated flat text files** under `data/`. No database required.

| File | Contents |
|------|----------|
| `managers.txt` | Manager accounts |
| `counter_staff.txt` | Counter staff accounts |
| `technicians.txt` | Technician accounts |
| `customers.txt` | Customer accounts (with vehicle info) |
| `appointments.txt` | Service appointments |
| `payments.txt` | Payment transactions |
| `feedbacks.txt` | Technician service feedback |
| `comments.txt` | Customer reviews & ratings |
| `prices.txt` | Service pricing (Normal / Major) |
| `vip_accounts.txt` | VIP loyalty accounts |
| `coupons.txt` | Issued coupons |
| `car_parts.txt` | Inventory parts |
| `vehicle_health.txt` | Vehicle health records |
| `appointment_parts.txt` | Parts consumed per appointment |
| `staff_employee_scheduling.txt` | Draft employee schedules |
| `published_employee_scheduling.txt` | Published employee schedules |
| `notifications.txt` | User notifications |
| `bug_reports.txt` | Customer support bug reports |

**File format example** (`appointments.txt`):
```
APT001|CUST001|TECH001|CS001|NORMAL|COMPLETED|2025-06-15|10:00|toyota vios WXY1234|Oil change and inspection
```

On startup, `FileManager` performs three automatic operations:
1. **`initializeFiles()`**: creates missing files and seeds a default manager (`admin` / `admin123`) and default pricing
2. **`resolveReferences()`**: populates runtime object references from ID strings
3. **`autoMarkMissedAppointments()`**: marks past-due SCHEDULED appointments as MISSED
4. **`autoRefreshAllMonthlyCoupons()`**: issues pending monthly coupons for eligible VIP members

---

## Project Structure

```
APUASC/
├── src/asc/
│   ├── Main.java                          # Entry point, L&F setup, bootstrap
│   ├── model/                             # 14 entity classes
│   │   ├── User.java                      #   Abstract base (Abstraction)
│   │   ├── Manager.java                   #   extends User
│   │   ├── CounterStaff.java              #   extends User
│   │   ├── Technician.java                #   extends User
│   │   ├── Customer.java                  #   extends User (multi-vehicle, stats)
│   │   ├── Appointment.java               #   Lifecycle: SCHEDULED→COMPLETED
│   │   ├── Payment.java                   #   Payment transactions
│   │   ├── VipAccount.java                #   4-tier loyalty system
│   │   ├── Coupon.java                    #   Monthly discount coupons
│   │   ├── CarPart.java                   #   Inventory items
│   │   ├── AppointmentPart.java           #   Parts consumption records
│   │   ├── VehicleHealthRecord.java       #   Post-service health records
│   │   ├── Feedback.java                  #   Technician service notes
│   │   ├── Comment.java                   #   Customer reviews & ratings
│   │   ├── Notification.java              #   In-app notifications
│   │   └── ServicePrice.java              #   Pricing config
│   ├── util/
│   │   ├── FileManager.java               # Flat-file CRUD, reference resolution
│   │   ├── UIUtils.java                   # Shared UI: avatars, borders, backgrounds
│   │   └── ValidationUtils.java           # Input validation helpers
│   └── gui/
│       ├── shared/
│       │   ├── LoginFrame.java            # Login window
│       │   └── RegisterDialog.java        # Registration dialog
│       ├── customer/                      # 11 customer-facing pages
│       │   ├── CustomerDashboard.java     #   Main frame + navigation
│       │   ├── CustomerHomePage.java      #   Landing page with banner
│       │   ├── CustomerChartPage.java     #   Spending charts
│       │   ├── CustomerHistoryPage.java   #   Appointment history
│       │   ├── CustomerProductsPage.java  #   Service catalog
│       │   ├── CustomerReceiptPage.java   #   Receipt viewer
│       │   ├── CustomerCommentPage.java   #   Reviews & ratings
│       │   ├── CustomerTopupPage.java     #   Wallet top-up
│       │   ├── CustomerVipPage.java       #   VIP dashboard
│       │   ├── CustomerVehicleHealthPage.java
│       │   └── CustomerSupportDialog.java #   AI chatbot support
│       ├── counter/                       # 7 counter-staff pages
│       ├── technician/                    # 7 technician pages
│       └── manager/                       # 7 manager pages
├── data/                                  # All persistent data
│   ├── *.txt                              # 18 flat data files
│   ├── Picture/                           # UI images & icons
│   ├── avatars/                           # User avatar images
│   └── counter/                           # Counter staff resources
├── lib/
│   └── pdfbox-app-2.0.31.jar              # Apache PDFBox for PDF generation
├── bin/                                   # Compiled .class files
├── .classpath                             # Eclipse classpath config
└── .project                               # Eclipse project config
```

---

## Prerequisites

- **Java JDK 21** or later (developed and tested on Java 21)
- **Eclipse IDE** (recommended) or any Java IDE
- **Apache PDFBox 2.0.31** (included in `lib/`)
- *(Optional)* **FlatLaf** jar for a modern IntelliJ-style dark UI; otherwise it falls back to Nimbus

---

## How to Run

### Option A: Eclipse (Recommended)

1. **Import** → `Existing Projects into Workspace` → select the `APUASC` folder
2. Ensure `lib/pdfbox-app-2.0.31.jar` is on the build path (should be auto-configured via `.classpath`)
3. Run `src/asc/Main.java` as a Java Application

### Option B: Command Line

```bash
# Compile (use --release 21 for Java 21 compatibility)
javac --release 21 -cp "lib/pdfbox-app-2.0.31.jar" -d bin src/asc/Main.java src/asc/**/*.java src/asc/**/**/*.java

# Run
java -cp "bin;lib/pdfbox-app-2.0.31.jar" asc.Main
```

> **Note:** On Linux/macOS, use `:` instead of `;` as the classpath separator.

---

## Default Login

A default manager account is seeded on first run:

| Field | Value |
|-------|-------|
| Username | `admin` |
| Password | `admin123` |
| Role | Manager |

Other accounts (counter staff, technicians, customers) can be created through the Manager's User Management page or via the registration dialog.

---

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| [Apache PDFBox](https://pdfbox.apache.org/) | 2.0.31 | PDF report and receipt generation |
| [FlatLaf](https://www.formdev.com/flatlaf/) *(optional)* | 3.4+ | Modern dark Look & Feel (graceful fallback to Nimbus if absent) |

---

## License

This project is an academic submission for the Object-Oriented Programming course at Asia Pacific University (APU). All rights reserved.
