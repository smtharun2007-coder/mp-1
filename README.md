# Disaster Relief Resource Allocation & Prioritisation System

An enterprise-grade, demo-ready disaster logistics, emergency routing, and multi-criteria incident triage system developed for a college capstone project review. Built with **Java 17**, **Javalin 6**, **MongoDB Atlas Sync Driver**, and a vanilla reactive web dashboard.

---

## 🌟 Key Features

1. **START Mass-Casualty Triage & Unified Assessment**:
   - Field officers file **ONE comprehensive assessment report** per affected region capturing casualty counts (Red, Yellow, Green, Black, Trapped, Vulnerable), physical threats, and all relief needs (Rescue, Shelter, Medicine, Food).
   - Deterministic `SeverityCalculator` evaluates a prioritized decision table (**Worst-Finding-Wins**: Severity 5 &rarr; 1) with pure logic and natural language explanations.
   - **Need-First Ranking**: Officers rank needs (e.g. Food before Rescue); the request containing the #1 need gets lower `arrivalNo` to win the final tie-break within that region.
   - **Honest Labelling**:
     - START is an international mass-casualty method performed by trained on-scene responders, **NOT an official Government of India system**.
     - All thresholds, decision table rules, medicine-kit ratios (10 people/kit), and food-kit ratios (5 people/day) are **PROJECT-DEFINED ASSUMPTIONS** adapted from START principles and Sphere humanitarian standards, centralized in `SeverityConfig`.
     - Limitation: START is for adults; pediatric JumpSTART is not implemented in this version.
     - L1/L2 escalation flags are explicitly labelled **"adapted from national disaster-level ideas"**.

2. **Multi-Criteria Dynamic Priority Queue**:
   - Triage ranking hierarchy: **Severity Level (5 &rarr; 1 DESC)** &bull; **Affected Population DESC** &bull; **Arrival Sequence Number ASC (FIFO tie-breaker)**.
   - Dynamic update support: Reports updated from the field recalculate severity, clear prior overrides with audit alerts, and re-order the in-memory binary heap ($O(n)$ update cost) preserving `arrivalNo`.
   - "Why this rank" deterministic column explaining every triage decision.

3. **Graph Logistics & Pathfinding Engine**:
   - Handcrafted undirected weighted graph adjacency list (no third-party graph libraries).
   - Breadth-First Search (BFS) and Depth-First Search (DFS) reachability traversals ($O(V + E)$).
   - Dijkstra Single-Source Shortest Path ($O((V + E) \log V)$) computing optimal routes around blocked roads.
   - Dynamic detection of cut-vertices and isolated regions when flood or landslide closures occur.

4. **Two-Phase Allocation Engine (Preview &rarr; Commit)**:
   - **Phase 1 (Preview)**: Read-only simulation calculating reachability, Dijkstra paths, depot draws, team assignment, and shelter capacity. Zero database mutations.
   - **Phase 2 (Commit)**: Guaranteed transactional execution protected by a dual-layer concurrency lock (in-process monitor lock + MongoDB multi-document ACID transaction with conditional updates). Physical double-allocation is mathematically impossible.

5. **Mathematical Stock Ledger & Integrity Verification**:
   - Every kit movement creates an immutable entry in the `stock_ledger` collection.
   - Built-in live integrity endpoint proving: `Available Stock + Reserved Stock + Consumed Stock == Initial Ledger Balance`.

6. **Role-Based Access Control (RBAC)**:
   - Salted `PBKDF2WithHmacSHA256` password hashing (125,000 iterations, 16-byte cryptographically secure random salt).
   - Two operational roles: `CONTROL_ROOM` (full review, override, and allocation authority) and `FIELD_OFFICER` (restricted to assigned geographical regions and personal reports; HTTP 403 on privileged routes).

7. **UN Sustainable Development Goals (SDG) & Impact Analytics**:
   - Real-time impact metrics tracking fulfilled requests, sheltered evacuees, delivered medicine/food, and average travel time.
   - Direct alignment with SDG 3 (Good Health), SDG 9 (Resilient Infrastructure), SDG 11 (Safe Communities), and SDG 13 (Climate Disaster Action).
   - Zero-cost operation profile running on the free MongoDB Atlas M0 cluster tier.

---

## 📋 Environment Variables

All configuration is managed strictly via environment variables or a local `.env` file loaded at startup:

| Variable | Required | Default | Description |
| :--- | :---: | :---: | :--- |
| `MONGODB_URI` | **Yes** (at startup) | *None* | MongoDB Atlas connection string (`mongodb+srv://...`) |
| `MONGODB_DB` | No | `relief` | Production/runtime database name |
| `MONGODB_TEST_DB` | No | `relief_test` | Database used for Atlas integration tests (dropped before tests) |
| `PORT` | No | `7070` | HTTP port for the Javalin web server |
| `DEMO_CONTROL_PASSWORD` | No | Randomly generated | Password for `admin` account (default fallback: `Admin@12345`) |
| `DEMO_OFFICER_PASSWORD` | No | Randomly generated | Password for `officer1` & `officer2` accounts (default fallback: `Officer@12345`) |

> **Fail-Fast Protection**: If `MONGODB_URI` is unset when starting the server, the application terminates immediately with a detailed guide on how to configure it.

---

## 🚀 Quickstart: Running Locally

### 1. Prerequisites
- **Java 17+** (JDK)
- **Maven 3.8+**
- A free **MongoDB Atlas** cluster account

### 2. Configure Environment (`.env`)
Create a `.env` file in the project root (copied from `.env.example`):
```bash
cp .env.example .env
```
Populate your Atlas connection string:
```properties
MONGODB_URI=mongodb+srv://reliefAdmin:YourSecurePassword@cluster0.abcde.mongodb.net/?retryWrites=true&w=majority
MONGODB_DB=relief
MONGODB_TEST_DB=relief_test
PORT=7070
DEMO_CONTROL_PASSWORD=Admin@12345
DEMO_OFFICER_PASSWORD=Officer@12345
```

### 3. Build & Run Tests
```bash
# Run all unit and integration tests
mvn test

# Package the shaded executable Uber-JAR
mvn package
```

### 4. Start the Application
```bash
java -jar target/disaster-relief-system-1.0.0.jar
```
Open your browser to: **`http://localhost:7070`**

### Demo Login Accounts:
- **Control Room Console**: `admin` / `Admin@12345`
- **Field Officer 1 (Regions 1–5)**: `officer1` / `Officer@12345`
- **Field Officer 2 (Regions 6–10)**: `officer2` / `Officer@12345`

---

## 🌐 Step-by-Step MongoDB Atlas Setup Guide

To set up a free MongoDB Atlas replica set (which natively supports multi-document transactions):

1. **Sign Up**: Visit [https://www.mongodb.com/cloud/atlas](https://www.mongodb.com/cloud/atlas) and register a free account.
2. **Create Free M0 Cluster**:
   - Choose **Shared Cluster** &rarr; **M0 (Free)**.
   - Select your preferred cloud provider and closest region (e.g., AWS / Mumbai, Frankfurt, Virginia).
   - Click **Create Deployment**.
3. **Configure Database User**:
   - Go to **Security &rarr; Database Access**.
   - Click **Add New Database User**.
   - Select **Password Authentication**. Set username: `reliefAdmin`, and generate a password.
   - Set privileges to **Read and write to any database**.
   - Click **Add User**.
4. **Configure Network Access IP Whitelist**:
   - Go to **Security &rarr; Network Access**.
   - Click **Add IP Address**.
   - For local development: click **Add Current IP Address**.
   - For cloud platforms (Render, Heroku): add `0.0.0.0/0` (Allow Access from Anywhere) with a 24-hour expiry or permanent access.
   - Click **Confirm**.
5. **Get Connection String**:
   - Go to **Deployments &rarr; Database**.
   - Click **Connect** &rarr; **Drivers** (Java 4.3+).
   - Copy the string format: `mongodb+srv://reliefAdmin:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority`.
   - Replace `<password>` with your database user password.
   - Paste into `.env` as `MONGODB_URI`.

---

## 🐳 Docker & Render Cloud Deployment

### 1. Build & Run via Docker Locally
```bash
# Build Docker image
docker build -t disaster-relief-system .

# Run container
docker run -p 7070:7070 \
  -e MONGODB_URI="mongodb+srv://reliefAdmin:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority" \
  -e PORT=7070 \
  disaster-relief-system
```

### 2. Deploy to Render (Free Web Service)
1. Push your repository to **GitHub**.
2. Log in to [Render Dashboard](https://dashboard.render.com/) and click **New &rarr; Web Service**.
3. Connect your GitHub repository.
4. Select **Docker** as the runtime environment (Render automatically detects the multi-stage `Dockerfile`).
5. Under **Environment Variables**, add:
   - `MONGODB_URI`: Your MongoDB Atlas `mongodb+srv://` connection string.
   - `PORT`: `7070`
   - `MONGODB_DB`: `relief`
   - `DEMO_CONTROL_PASSWORD`: `Admin@12345`
   - `DEMO_OFFICER_PASSWORD`: `Officer@12345`
6. Click **Create Web Service**.
7. *Note*: On Render's free tier, containers automatically sleep after 15 minutes of inactivity and wake up upon receiving a new HTTP request (taking ~30 seconds for cold start).

---

## ⚡ Graph Algorithmic Benchmarks

Run the standalone graph benchmark suite:
```bash
java -cp "target/classes;target/test-classes" com.disasterrelief.benchmark.Benchmark
```

### Benchmark Results (Standard Laptop Hardware)
| Node Count | Edge Count | BFS Reachable | BFS Traversal (ms) | Dijkstra Shortest Path (ms) |
| :---: | :---: | :---: | :---: | :---: |
| **1,000** | 2,997 | 1,000 nodes | **1.4 ms** | **0.5 ms** |
| **10,000** | 29,996 | 10,000 nodes | **2.4 ms** | **8.6 ms** |
| **50,000** | 149,997 | 50,000 nodes | **22.2 ms** | **21.1 ms** |

---

## 📝 Sensible Defaults & Architectural Notes

Where specifications were open-ended, the following sensible defaults were adopted:
1. **Network Topology**: 14 vertices representing 10 administrative regions (IDs 1–10), 2 central depots (IDs 11–12), and 2 emergency shelters (IDs 13–14).
2. **Bridge Roads & Cut Vertices**: Road 4 (Node 3 &harr; 4) and Road 8 (Node 6 &harr; 9) act as single-link mountain/forest bridges. Closing either road instantly isolates Village 4 or Village 9, triggering dynamic re-routing or blocking.
3. **Arrival Order Sequencing**: Monotonically increasing atomic sequence persisted in the `counters` collection (`{_id: "arrival", seq: n}`). Even when incident parameters are updated, the original arrival order is retained to prevent starvation.
4. **Session Lifetime**: Tokens are stored in a thread-safe `ConcurrentHashMap` with instant invalidation upon logout.
