# 5-Minute Project Review Demonstration Script

This script walks through the **six core review test cases** directly within the web dashboard interface for college evaluation.

---

## 🎬 Pre-Flight Setup (30 Seconds)

1. **Start the application**:
   ```bash
   java -jar target/disaster-relief-system-1.0.0.jar
   ```
2. Open browser to **`http://localhost:7070`**.
3. Click **"Reset Demo Data"** or click **"Fill Admin"** (`admin` / `Admin@12345`) and sign in.

---

## 🔬 Test Case Walkthrough (4 Minutes 30 Seconds)

### Test Case 1: Normal Triage Flow & Resource Allocation
- **Goal**: Demonstrate multi-criteria ranking (**Severity 5 &rarr; Population 5,000 &rarr; Arrival #1**) and two-phase allocation.
- **Steps**:
  1. Navigate to **Dashboard**. Point out the **Live Triage Queue** table.
  2. Notice that **`REQ01`** (Coastal Harbor, Severity 5, Population 5,000) is ranked **#1**.
  3. Navigate to **Process & Allocate Desk**. `REQ01` is selected.
  4. Click **"Preview Plan (No Writes)"**:
     - Point out the Dijkstra shortest route: `Node 11 &rarr; Node 1` (15 mins travel time).
     - Point out the calculated depot draw: 80 medicine kits, 120 food packs.
     - Note that zero database mutations occurred during preview.
  5. Click **"Commit Transaction"**:
     - System executes atomic reservation inside the MongoDB transaction.
     - Green toast: *"Allocation successfully committed!"*
     - Point out that `REQ01` is marked **`ALLOCATED`** and the next request (**`REQ02`**) automatically becomes #1 in the live queue.

---

### Test Case 2: Insufficient Stock & Partial Allocation
- **Goal**: Demonstrate how stock shortages result in partial fulfillment while preserving outstanding balances.
- **Steps**:
  1. On the **Process Desk**, select **`REQ05`** (East Plains, demands 220 medicine kits and 250 food packs).
  2. Click **"Preview Plan"**:
     - Notice the amber warning: *"Partial allocation: depots have limited stock; outstanding quantity will remain."*
     - Shows that only 100 medicine kits can be drawn from Depot 2, leaving **120 medicine kits outstanding**.
  3. Click **"Commit Transaction"**:
     - Request status updates to **`PARTIALLY_ALLOCATED`**.
     - Navigate to **Allocations Desk**: show the committed mission with 100 kits. The outstanding shortfall remains recorded for future fulfillment.

---

### Test Case 3: Shelter Capacity Exhaustion (Evacuation Refusal)
- **Goal**: Prove that when shelters are full, evacuating citizens is safely refused and no phantom places are double-promised.
- **Steps**:
  1. Navigate to **Shelters**. Show **Hope Mountain Outpost Shelter** (Capacity 60) and **Haven Shelter** (Capacity 350).
  2. Select an active rescue request (e.g. `REQ02` or `REQ06`) requiring evacuation.
  3. In a scenario where all reachable shelter slots are filled, click **"Preview Plan"**:
     - The preview pill displays: **`UNFEASIBLE (DEFERRED)`**.
     - Status message states: *"All reachable shelters are completely full. Evacuation refused; request remains waiting."*
     - The "Commit Transaction" button is disabled, safeguarding human life from overcrowding.

---

### Test Case 4: Road Closure & Isolated Village Detection
- **Goal**: Demonstrate dynamic graph cut-vertex isolation, automatic request blocking, and re-routing.
- **Steps**:
  1. Navigate to **Road Network**.
  2. Locate **`RD04`** (Bridge connecting Node 3 to **Forest Settlement - Node 4**).
  3. Click **"Close Road"**, and enter reason: *"Bridge collapse due to flash flood"*.
  4. Road status changes to **`CLOSED`**.
  5. Return to **Dashboard**:
     - Red warning pill appears: **`⚠️ ISOLATED REGIONS DETECTED: [4]`**.
     - Show that request **`REQ08`** located in Forest Settlement has automatically shifted from `WAITING` to **`BLOCKED`**.
  6. Return to **Road Network**, click **"Reopen Road"** on `RD04`:
     - System re-evaluates graph reachability; Village 4 reconnects and `REQ08` returns to the active `WAITING` queue.

---

### Test Case 5: Dynamic Severity Escalation & Heap Re-Ranking
- **Goal**: Show that modifying incident parameters in the field dynamically updates the in-memory binary heap order.
- **Steps**:
  1. Navigate to **Priority Queue**.
  2. Notice request **`REQ11`** (Severity 2, Low urgency) sits at the bottom of the queue.
  3. Navigate to **Review Inbox** or execute a severity override on `REQ11`:
     - Escalate Severity from **2** to **5**, and Population to **9,000**.
     - Mandatory reason entered: *"Chemical plant leak detected near residential center"*.
  4. Return to **Priority Queue**:
     - **`REQ11` has immediately leaped to Rank #1** at the top of the binary heap!
     - Point out the **"Why This Rank"** column: *"Severity Level 5 | Affected Pop: 9,000 | Arrival Sequence #11"*.

---

### Test Case 6: Stock Ledger Mathematical Audit & Zero Double-Allocations
- **Goal**: Demonstrate mathematical conservation of stock (`Available + Reserved == Ledger Balance`) and proof of zero double-allocations.
- **Steps**:
  1. Navigate to **Stock & Ledgers**.
  2. Point out the **Depots Summary Cards**: note the current available medicine and food.
  3. Click **"+ Restock / Adjust Depot"**:
     - Add `+50` medicine kits and `+100` food packs to Depot 1. Reason: *"Emergency airlift restock"*.
     - Click **"Commit Stock Ledger"**.
  4. Show the new row instantly appear in the **Immutable Stock Ledger** table with user, role, and delta.
  5. Navigate to **Impact & SDGs**:
     - Point out the metric **Double Allocations: `0`** (mathematically verified).
     - Point out the **Stock Integrity Badge**: **`100% BALANCED`**.

---

### Bonus: Role-Based Access Control (RBAC) Demonstration
- **Steps**:
  1. Click **"Sign Out"**.
  2. Click **"Fill Officer 1"** (`officer1` / `Officer@12345`) and sign in.
  3. Point out the changed UI:
     - Control Room menus (Process desk, Depots, Stock ledger, User administration) are **completely hidden**.
     - Field Officer sees only **My Area (Regions 1–5)**, **New Incident Report**, and **Report Road Hazard**.
     - Attempting direct unauthorized API calls triggers an HTTP `403 Forbidden` response.
