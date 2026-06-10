### 🖥️ SmartOS Monitor

Real-time system monitoring dashboard that brings Operating Systems, Computer Networks, and DBMS concepts to life using Java Spring Boot and MySQL.

### ✨ Features

1. Live Dashboard

   Real-time CPU, memory, disk, and network monitoring with interactive charts.

2. Process Manager

   View running processes with PID, CPU%, RAM, threads, and process state indicators.

3. CPU Scheduling Simulator

   Visualize FCFS, SJF, and Round Robin algorithms with Gantt charts and performance metrics.

4. Network Analyzer

   Inspect interfaces, MAC/IP details, active TCP connections, and open ports.

5. DBMS History Module

   Auto-store system snapshots in MySQL and generate aggregated trends and statistics.

6. Intelligent Alerts

   Detect high CPU/RAM usage, log alerts with severity levels, and suggest offending processes.

### 🛠️ Tech Stack

| Layer          | Technologies                                              |
| -------------- | --------------------------------------------------------- |
| Backend        | Java 17, Spring Boot 3, Spring Data JPA, Hibernate, Maven |
| Frontend       | HTML5, CSS3, Vanilla JavaScript, Chart.js                 |
| Database       | MySQL 8                                                   |
| System Metrics | OSHI Library                                              |

### 🎯 Subjects Covered

| Subject           | Concepts Demonstrated                                                |
| ----------------- | -------------------------------------------------------------------- |
| Operating Systems | Process management, CPU scheduling, memory and file systems          |
| Computer Networks | TCP/IP, ports, sockets, transport and data link layers               |
| DBMS              | Schema design, indexing, aggregation queries, scheduled transactions |

### 🏗️ Architecture

### 🚀 Getting Started

Prerequisites

* Java 17+

* Maven 3.8+

* MySQL 8+

* VS Code with Live Server extension

Database Setup

Installation

Configure Database

Edit src/main/resources/application.properties:

Run Backend

Backend API: [http://localhost:8082/api/health](http://localhost:8082/api/health)

Run Frontend

Open Frontend/index.html using VS Code Live Server.

Dashboard URL: [http://localhost:5500/Frontend/index.html](http://localhost:5500/Frontend/index.html)

### 📡 API Endpoints

| Method | Endpoint                 |
| ------ | ------------------------ |
| GET    | /api/health              |
| GET    | /api/cpu                 |
| GET    | /api/memory              |
| GET    | /api/disk                |
| GET    | /api/processes           |
| GET    | /api/network             |
| GET    | /api/ports               |
| GET    | /api/alerts              |
| PUT    | /api/alerts/{id}/resolve |
| GET    | /api/history/stats       |
| POST   | /api/scheduler/simulate  |

### 🌟 Future Enhancements

* Docker container monitoring

* Email/SMS notifications

* Spring Security authentication

* Dark/light theme toggle

* Responsive mobile UI

* WebSocket-based live updates

### 💡 Why This Project Stands Out

Most student projects focus on a single subject. SmartOS Monitor combines Operating Systems, Computer Networks, and DBMS into one practical application — because real-world systems rely on all three working together.
