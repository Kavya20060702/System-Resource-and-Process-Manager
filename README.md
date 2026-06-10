# 🖥️ System Resource and Process Manager (SmartOS Monitor)

**SmartOS Monitor** is a full-stack real-time system monitoring dashboard 
that makes Operating Systems, Computer Networks and DBMS theory visible 
through a live web application — built entirely in Java Spring Boot 
with a MySQL backend.

---

## ✨ Features

* 📊 **Live System Dashboard**
  * Real-time CPU usage with per-core breakdown
  * RAM consumption with used, free and swap memory
  * Disk usage per partition
  * Network I/O with active TCP connections

* ⚙️ **Process Manager**
  * Lists all running processes with PID, CPU%, RAM, threads
  * Shows process state — Running, Sleeping, Stopped, Zombie
  * Color-coded rows for high CPU processes

* 🧠 **CPU Scheduling Simulator**
  * Visual implementation of FCFS, SJF and Round Robin
  * Generates Gantt chart with color-coded process blocks
  * Calculates Average Waiting Time, Turnaround Time and CPU Utilization
  * Demonstrates OS scheduling theory interactively

* 🌐 **Network Analyzer**
  * Displays hostname, IPv4 address and MAC per interface
  * Shows active TCP established connections
  * Open port scanner with process ID mapping
  * Demonstrates Transport and Data Link layer concepts

* 🗄️ **DBMS History Module**
  * Auto-saves system snapshots to MySQL every 30 seconds
  * Aggregation queries — AVG, MAX, COUNT with time filters
  * 7-day hourly trend using native GROUP BY SQL
  * Database indexes on timestamp for optimized queries

* 🚨 **Intelligent Alert System**
  * Detects CPU > 85% and RAM > 90% threshold breaches
  * Logs alerts with severity (HIGH / CRITICAL) to MySQL
  * Resolve button identifies top offending processes
  * Shows real-time recommendation on resolution

---

## 🛠️ Tech Stack

### Backend
* Java 17
* Spring Boot 3
* Spring Data JPA + Hibernate
* MySQL 8
* OSHI Library (OS & Hardware metrics)
* Maven

### Frontend
* HTML5 + CSS3
* Vanilla JavaScript
* Chart.js (live graphs)

### Database
* MySQL
* JPA Entities with indexing
* Native SQL + JPQL queries

---

## 🎯 Subjects Covered

| Subject | Concepts Demonstrated |
|---|---|
| **Operating Systems** | Process management, CPU scheduling, memory management, file system |
| **Computer Networks** | TCP/IP, transport layer, ports, sockets, HTTP, CORS |
| **DBMS** | Schema design, indexing, aggregation, scheduled transactions |

---

## 🏗️ Project Architecture

```text
SmartOS Monitor
│
├── Backend (Spring Boot)
│   ├── SystemMonitorService     → reads OS data using OSHI
│   ├── SchedulerService         → FCFS, SJF, Round Robin algorithms
│   ├── SystemController         → REST API endpoints
│   ├── SystemSnapshot (Entity)  → MySQL table with indexes
│   ├── Alert (Entity)           → threshold breach logs
│   └── CorsConfig               → cross-origin HTTP configuration
│
├── Frontend (HTML/CSS/JS)
│   ├── Dashboard Tab            → live metric cards + charts
│   ├── Processes Tab            → real process manager
│   ├── Network Tab              → interfaces + port scanner
│   ├── CPU Scheduler Tab        → Gantt chart simulator
│   └── History Tab              → MySQL aggregation results
│
└── Database (MySQL)
    ├── system_snapshots         → periodic OS metric records
    ├── system_alerts            → threshold breach events
    └── process_logs             → process audit trail
```

---

## 🚀 Getting Started

### Prerequisites
* Java 17+
* Maven 3.8+
* MySQL 8.0+
* VS Code with Live Server extension

### Database Setup
```sql
CREATE DATABASE smartos_db;
```

### Installation
```bash
git clone https://github.com/your-username/smartos-monitor.git
cd smartos-monitor
```

### Configure Database
Open `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smartos_db
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
server.port=8082
```

### Run Backend
```bash
mvn spring-boot:run
```
Backend starts at:
