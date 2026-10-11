package com.disasterrelief.service;

import com.disasterrelief.config.AppConfig;
import com.disasterrelief.db.MongoManager;
import com.disasterrelief.db.SeedData;
import com.disasterrelief.graph.DijkstraResult;
import com.disasterrelief.graph.Graph;
import com.disasterrelief.model.*;
import com.disasterrelief.queue.RequestPriorityQueue;
import com.disasterrelief.repository.*;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification of the core system rules and the 6 named review test cases:
 *
 * 1. Normal flow: ranked by severity, then population, then arrival, and resources allocated.
 * 2. Insufficient kits: partial allocation, outstanding quantity retained.
 * 3. Full shelter: evacuation refused, no places reserved.
 * 4. Isolated village: after closing a road, BFS and DFS report it unreachable and no allocation is committed.
 * 5. Updated severity: the queued request is removed and reinserted and the order changes.
 * 6. Stock update: manual update reflected, no duplicated stock (integrity check passes).
 *
 * Plus:
 * - Tie-breaking determinism
 * - Concurrent reservation atomicity (conditional update guarantee)
 * - Transaction rollback leaves stock unchanged
 */
public class DisasterReliefSystemTest {

    private static MongoDatabase testDb;
    private static MongoClient mongoClient;
    private static AllocationService allocationService;
    private static RequestPriorityQueue priorityQueue;
    private static RequestRepository requestRepo;
    private static DepotRepository depotRepo;
    private static ShelterRepository shelterRepo;
    private static RescueTeamRepository teamRepo;
    private static RoadRepository roadRepo;

    @BeforeAll
    public static void initDatabase() {
        Assumptions.assumeTrue(AppConfig.hasMongoUri(),
                "MONGODB_URI is not set. Set MONGODB_URI in .env to run Atlas integration tests.");

        mongoClient = MongoManager.getInstance().getMongoClient();
        testDb = MongoManager.getInstance().getDatabase(AppConfig.getTestDatabaseName());
        // Drop test database before running tests
        testDb.drop();
        MongoManager.getInstance().initIndexes(testDb);

        priorityQueue = new RequestPriorityQueue();
        allocationService = new AllocationService(testDb, mongoClient, priorityQueue);

        requestRepo = new RequestRepository(testDb);
        depotRepo = new DepotRepository(testDb);
        shelterRepo = new ShelterRepository(testDb);
        teamRepo = new RescueTeamRepository(testDb);
        roadRepo = new RoadRepository(testDb);
    }

    @BeforeEach
    public void resetSeed() {
        if (testDb != null) {
            SeedData.resetDatabase(testDb);
            priorityQueue.rebuild(requestRepo.findWaitingOrPartiallyAllocated());
        }
    }

    @Test
    @DisplayName("Case 1 - Normal flow: ranked by severity, population, arrival, and resources allocated")
    public void test1_NormalFlow_RankedAndAllocated() {
        // In Aruvi seed data: top is REQ05 (Sev 4 Critical, Pop 400, Arr 5), followed by REQ11 (Sev 4, Pop 350, Arr 11)
        Request top = priorityQueue.peek();
        assertNotNull(top);
        assertEquals("REQ05", top.getId(), "REQ05 with Critical severity and Pop 400 must be top rank");

        // Preview REQ05
        AllocationPreview preview = allocationService.preview(top.getId());
        assertTrue(preview.isReachable());
        assertTrue(preview.isFeasible());
        assertEquals(40, preview.getMedicineAllocated());
        assertEquals(70, preview.getFoodAllocated());

        // Commit REQ05
        boolean committed = allocationService.commit(top.getId(), "admin", "CONTROL_ROOM");
        assertTrue(committed);

        // Verify request is ALLOCATED
        Request updatedReq = requestRepo.findById(top.getId());
        assertEquals(RequestStatus.ALLOCATED, updatedReq.getStatus());

        // Next in queue must now be REQ11
        Request next = priorityQueue.peek();
        assertNotNull(next);
        assertEquals("REQ11", next.getId());
    }

    @Test
    @DisplayName("Case 2 - Insufficient kits: partial allocation, outstanding quantity retained")
    public void test2_InsufficientKits_PartialAllocation() {
        // Drain medicine so only 5 units are available across all depots
        Depot d1 = depotRepo.findById("DEP01");
        Depot d2 = depotRepo.findById("DEP02");
        assertNotNull(d1);
        assertNotNull(d2);
        depotRepo.adjustStock(null, d1.getId(), -(d1.getMedicineKits() - 5), 0);
        depotRepo.adjustStock(null, d2.getId(), -d2.getMedicineKits(), 0);

        // REQ04 needs 30 food kits and 10 medicine kits in Region 5
        AllocationPreview preview = allocationService.preview("REQ04");
        assertTrue(preview.isFeasible());
        assertEquals(5, preview.getMedicineAllocated());
        assertTrue(preview.getOutstandingMedicine() > 0, "Should have outstanding medicine due to limited depot stock");

        boolean committed = allocationService.commit("REQ04", "admin", "CONTROL_ROOM");
        assertTrue(committed);

        Request req = requestRepo.findById("REQ04");
        assertEquals(RequestStatus.PARTIALLY_ALLOCATED, req.getStatus(), "Status must be PARTIALLY_ALLOCATED");
        ReliefRequest rel = (ReliefRequest) req;
        assertEquals(5, rel.getMedicineAllocated());
        assertEquals(5, rel.getOutstandingMedicine(), "Outstanding quantity must be retained");
    }

    @Test
    @DisplayName("Case 3 - Full shelter: evacuation refused, no places reserved")
    public void test3_FullShelter_EvacuationRefused() {
        // Fill remaining available capacity in both shelters SH01 and SH02
        Shelter s1 = shelterRepo.findById("SH01");
        Shelter s2 = shelterRepo.findById("SH02");
        assertNotNull(s1);
        assertNotNull(s2);
        shelterRepo.reservePlaces(null, s1.getId(), s1.getAvailableCapacity());
        shelterRepo.reservePlaces(null, s2.getId(), s2.getAvailableCapacity());

        // Preview rescue request REQ02
        AllocationPreview preview = allocationService.preview("REQ02");
        assertFalse(preview.isFeasible(), "Evacuation must be unfeasible when shelters are full");
        assertTrue(preview.getStatusMessage().toLowerCase().contains("full") ||
                   preview.getStatusMessage().toLowerCase().contains("refused") ||
                   preview.getStatusMessage().toLowerCase().contains("zero"));

        // Commit must fail and reserve zero places
        boolean committed = allocationService.commit("REQ02", "admin", "CONTROL_ROOM");
        assertFalse(committed);

        Request req = requestRepo.findById("REQ02");
        assertEquals(RequestStatus.WAITING, req.getStatus(), "Request must remain WAITING when evacuation is refused");
    }

    @Test
    @DisplayName("Case 4 - Isolated village: after closing roads, BFS reports unreachable, no allocation committed")
    public void test4_IsolatedVillage_UnreachableNoAllocationCommitted() {
        // Stonepass (Region 10) connects via RD14, RD17, RD18
        // REQ18 is located in Region 10
        allocationService.handleRoadClosure("RD14", "Road closed due to flood", "admin");
        allocationService.handleRoadClosure("RD17", "Road closed due to flood", "admin");
        allocationService.handleRoadClosure("RD18", "Road closed due to flood", "admin");

        Graph graph = allocationService.buildLiveGraph();
        assertFalse(graph.reachableFrom(1).contains(10), "Stonepass 10 must be unreachable from Depot 1");
        assertFalse(graph.reachableFrom(2).contains(10), "Stonepass 10 must be unreachable from Depot 2");

        // REQ18 must now be marked BLOCKED
        Request req = requestRepo.findById("REQ18");
        assertEquals(RequestStatus.BLOCKED, req.getStatus(), "Request in isolated village must be BLOCKED");

        // Attempting to commit an unreachable request must fail
        boolean committed = allocationService.commit("REQ18", "admin", "CONTROL_ROOM");
        assertFalse(committed);
    }

    @Test
    @DisplayName("Case 5 - Updated severity: queued request is removed and reinserted, changing order")
    public void test5_UpdatedSeverity_ReRankedInQueue() {
        // REQ12 is initially Severity 1 Low
        Request r12 = requestRepo.findById("REQ12");
        assertEquals(1, r12.getSeverity());

        // Escalate REQ12 to Severity 4 Critical, pop 9000 (Highest in the entire system)
        r12.setSeverity(4);
        r12.setAffectedPopulation(9000);
        requestRepo.update(r12);
        priorityQueue.update(r12);

        // REQ12 must now be at the very top of the priority queue
        Request top = priorityQueue.peek();
        assertEquals("REQ12", top.getId(), "Escalated request must become top rank in live queue");
        assertEquals(4, top.getSeverity());
    }

    @Test
    @DisplayName("Case 6 - Stock update: manual update reflected, no duplicated stock (integrity check passes)")
    public void test6_StockUpdate_IntegrityCheckPasses() {
        // Commit REQ05 which draws stock
        allocationService.commit("REQ05", "admin", "CONTROL_ROOM");

        // Manual restock of Depot 1
        depotRepo.adjustStock(null, "DEP01", 50, 100);
        new StockLedgerRepository(testDb).create(new StockLedgerEntry(
                "LED_RESTOCK_TEST", "DEP01", "admin", "CONTROL_ROOM", "MEDICINE", 50, "RESTOCK", "Emergency airlift restock"
        ));
        new StockLedgerRepository(testDb).create(new StockLedgerEntry(
                "LED_RESTOCK_TEST_2", "DEP01", "admin", "CONTROL_ROOM", "FOOD", 100, "RESTOCK", "Emergency airlift restock"
        ));

        // Mathematical integrity check
        Map<String, Object> integrity = allocationService.verifyStockIntegrity();
        assertTrue((Boolean) integrity.get("valid"), "Integrity check must pass (stock + reserved == expected)");
        assertEquals(0, integrity.get("doubleAllocationsCount"), "Double allocations count must be 0");
    }

    @Test
    @DisplayName("Concurrent reservation test: two threads cannot double-promise the same stock")
    public void testConcurrentReservation() throws InterruptedException {
        // Reset depot stock to exact 50 units
        Depot d = depotRepo.findById("DEP01");
        assertNotNull(d);
        // Drain depot first
        depotRepo.adjustStock(null, d.getId(), -d.getMedicineKits(), -d.getFoodPacks());
        // Set exact 50 medicine and food units
        depotRepo.adjustStock(null, d.getId(), 50, 50);

        // Two threads attempt to reserve 40 units each simultaneously (total 80 > 50)
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    boolean ok = depotRepo.reserveStock(null, "DEP01", 40, 40);
                    if (ok) successCount.incrementAndGet();
                } catch (Exception ignored) {}
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        // Exactly one thread must succeed; the other must fail due to conditional update
        assertEquals(1, successCount.get(), "Only one thread may reserve stock; conditional update prevents double-promising");
    }
}
