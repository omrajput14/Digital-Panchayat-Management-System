# Digital Panchayat Management System

A Java Swing desktop application for local Panchayat office staff to manage day-to-day governance tasks.

## Features

### 📊 Summary Dashboard
- Live statistics: open complaints, issues by severity, meetings this month, pending action items
- One-click **Print Full Report** → saves as PDF via system print dialog

### 🏠 Citizen Complaint Portal
- Register complaints with citizen name, ward, category, description, status
- Filter by status and ward number
- Update status (Pending → In Progress → Resolved)
- **Export CSV** and **Print Report** buttons
- Red-border validation for empty or incorrectly formatted fields

### 🔧 Infrastructure Issue Tracker
- Log issues with type, severity (Low / Medium / High), assigned officer, resolution date
- **Overdue rows highlighted in red** automatically
- Past-date warning confirmation dialog
- Status updates and full CRUD

### 📅 Meeting Records Manager
- Record Gram Sabha, Ward Committee, Emergency meetings
- Attendees and action items entered one-per-line, stored efficiently
- Keyword search across type, agenda, and attendees
- Click any row to see full meeting details in the detail panel

## Tech Stack
- **Language**: Java 17+
- **UI**: Java Swing (Nimbus L&F with custom dark theme)
- **Database**: SQLite via JDBC (no server required)
- **Build**: Shell scripts (no Maven/Gradle needed)

## Project Structure

```
PanchayatSystem/
├── src/panchayat/
│   ├── Main.java                   ← Entry point
│   ├── db/
│   │   └── DatabaseManager.java    ← SQLite singleton connection
│   ├── model/
│   │   ├── Complaint.java
│   │   ├── Issue.java
│   │   └── Meeting.java
│   ├── dao/
│   │   ├── ComplaintDAO.java
│   │   ├── IssueDAO.java
│   │   └── MeetingDAO.java
│   ├── view/
│   │   ├── MainFrame.java          ← Root JFrame with tabs + sidebar
│   │   ├── DashboardPanel.java
│   │   ├── ComplaintPanel.java
│   │   ├── IssuePanel.java
│   │   └── MeetingPanel.java
│   └── util/
│       └── ReportExporter.java     ← CSV export + PDF/Print utility
├── lib/
│   └── sqlite-jdbc-3.36.0.3.jar   ← SQLite JDBC driver
├── build.sh                        ← Compile script
├── run.sh                          ← Launch script
└── README.md
```

## How to Run

### Prerequisites
- Java 17 or later (`java -version`)

### Step 1 — Compile
```bash
cd PanchayatSystem
chmod +x build.sh run.sh
./build.sh
```

### Step 2 — Run
```bash
./run.sh
```

The SQLite database (`panchayat.db`) is created automatically on first launch.

### Manual compile & run
```bash
# Compile
find src -name "*.java" | xargs javac -cp "lib/sqlite-jdbc-3.36.0.3.jar" -d out

# Run
java --enable-native-access=ALL-UNNAMED \
     -cp "out:lib/sqlite-jdbc-3.36.0.3.jar" \
     panchayat.Main
```

> **Windows**: Replace `:` with `;` in the classpath.

## Export & Print

| Button | Where | What it does |
|--------|-------|--------------|
| 📥 Export CSV | Complaints / Issues / Meetings | Saves current table to a `.csv` file |
| 🖨 Print Report | All modules + Dashboard | Opens system print dialog (macOS: Save as PDF) |

## Design Decisions

- **Singleton DB connection** — avoids SQLite file-lock conflicts on a single-user desktop
- **DAO pattern** — all SQL lives in DAO classes; views stay clean
- **No WAL mode** — default DELETE journal avoids lock conflicts on fresh DB creation
- **Pipe-delimited storage** — attendees and action items stored as `"Ram|Shyam|Geeta"` to avoid extra join tables
- **No external PDF library** — uses Java's built-in `java.awt.print.PrinterJob`; macOS "Save as PDF" covers the use case

---

Made with ❤️ for Gram Panchayat office staff
