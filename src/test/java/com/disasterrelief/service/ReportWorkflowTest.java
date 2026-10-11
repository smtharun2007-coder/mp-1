package com.disasterrelief.service;

import com.disasterrelief.config.AppConfig;
import com.disasterrelief.db.MongoManager;
import com.disasterrelief.db.SeedData;
import com.disasterrelief.model.*;
import com.disasterrelief.queue.RequestPriorityQueue;
import com.disasterrelief.repository.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for the Single Report Assessment and Severity Workflow:
 *
 * 1. Need-first ranking: With needOrder starting with FOOD, the SupplyRequest gets the lower arrivalNo
 *    and is prioritized ahead of the RescueRequest for the same region.
 * 2. Dynamic Update: Editing casualty counts recalculates severity, updates priority queue preserving arrivalNo,
 *    and clears prior overrides while generating an alert.
 * 3. Authority & RBAC: Control Room severity override requires a mandatory justification reason;
 *    unauthorized field officer attempts are rejected with HTTP 403 (SecurityException).
 * 4. Duplicate Prevention: Rapid identical submissions within 10 minutes are blocked unless explicitly overridden.
 */
public class ReportWorkflowTest {

    private static MongoDatabase testDb;
    private static MongoClient mongoClient;
    private static ReportService reportService;
    private static RequestPriorityQueue priorityQueue;
    private static ReportRepository reportRepo;
    private static RequestRepository requestRepo;
    private static AlertRepository alertRepo;

    private static User controlUser;
    private static User officerUser;

    @BeforeAll
    public static void initDatabase() {
        Assumptions.assumeTrue(AppConfig.hasMongoUri(),
                "MONGODB_URI is not set. Set MONGODB_URI in .env to run Atlas integration tests.");

        mongoClient = MongoManager.getInstance().getMongoClient();
        testDb = MongoManager.getInstance().getDatabase(AppConfig.getTestDatabaseName());
        testDb.drop();

        controlUser = new User("usr_ctrl", "admin", "hash", "salt", Role.CONTROL_ROOM, List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        officerUser = new User("usr_off1", "officer1", "hash", "salt", Role.FIELD_OFFICER, List.of(1, 2, 3, 4, 5));
    }

    @BeforeEach
    void resetState() {
        SeedData.resetDatabase(testDb);
        priorityQueue = new RequestPriorityQueue();
        requestRepo = new RequestRepository(testDb);
        reportRepo = new ReportRepository(testDb);
        alertRepo = new AlertRepository(testDb);

        // Load waiting requests into priority queue
        for (Request r : requestRepo.findWaitingAndPartiallyAllocated()) {
            priorityQueue.insert(r);
        }

        reportService = new ReportService(testDb, mongoClient, priorityQueue);
    }

    @Test
    @DisplayName("Need-First Rule: When FOOD is ranked #1, SupplyRequest gets lower arrivalNo than RescueRequest")
    void testNeedFirstRuleAssignsLowerArrivalNoToSupply() {
        // Create a report that has BOTH Rescue and Food needs, with FOOD ranked first!
        Report report = new Report("REP_NEEDFIRST", "officer1", 1);
        report.setCounts(new ReportCounts(1000, 4, 10, 50, 0, 1, 100)); // immediateRisk = 5 -> Sev 5
        report.setHoursWithoutSupplies(10);
        report.setOngoingThreat(false);
        report.setRoadAccessObserved("OPEN");

        ReportNeeds needs = new ReportNeeds();
        needs.setRescue(new ReportNeeds.RescueNeed(20));
        needs.setFood(new ReportNeeds.FoodNeed(200, 3, 120, 120, "Priority calorie supply"));
        report.setNeeds(needs);

        // Crucial: Officer specifies FOOD first, RESCUE second
        report.setNeedOrder(List.of("FOOD", "RESCUE"));

        // Submit via officer
        reportService.submit(report, officerUser, false);
        assertEquals("PENDING_REVIEW", report.getStatus());

        // Control Room approves the report
        Report approved = reportService.approve(report.getId(), controlUser);
        assertEquals("APPROVED", approved.getStatus());
        assertEquals(2, approved.getLinkedRequestIds().size());

        String req1Id = approved.getLinkedRequestIds().get(0);
        String req2Id = approved.getLinkedRequestIds().get(1);

        Request reqA = requestRepo.findById(req1Id);
        Request reqB = requestRepo.findById(req2Id);

        // One is RescueRequest, one is SupplyRequest
        RescueRequest rescueReq = (reqA instanceof RescueRequest) ? (RescueRequest) reqA : (RescueRequest) reqB;
        SupplyRequest supplyReq = (reqA instanceof SupplyRequest) ? (SupplyRequest) reqA : (SupplyRequest) reqB;

        assertNotNull(rescueReq);
        assertNotNull(supplyReq);

        // The request containing the #1 need (FOOD -> SupplyRequest) MUST have a lower arrivalNo
        assertTrue(supplyReq.getArrivalNo() < rescueReq.getArrivalNo(),
                "SupplyRequest arrival (" + supplyReq.getArrivalNo() +
                ") must be lower than RescueRequest arrival (" + rescueReq.getArrivalNo() + ") because FOOD was ranked #1");

        // Verify items inside SupplyRequest are ordered per needOrder
        assertFalse(supplyReq.getItems().isEmpty());
        assertEquals("FOOD", supplyReq.getItems().get(0).getNeed());
    }

    @Test
    @DisplayName("Update Workflow: Recalculates severity, preserves arrivalNo, clears override, and creates alert")
    void testUpdateRecalculatesSeverityAndClearsOverride() {
        // 1. Submit and approve report REP03 (starts at Severity 3 High, pop 240)
        Report rep = reportRepo.findById("REP03");
        assertNotNull(rep);
        assertEquals(3, rep.getSeverity().getEffectiveLevel());

        // 2. Control Room overrides severity to 1 with a justification
        reportService.overrideSeverity("REP03", 1, "Initial aerial survey suggests lower casualty severity", controlUser);
        rep = reportRepo.findById("REP03");
        assertEquals(1, rep.getSeverity().getEffectiveLevel());
        assertEquals(1, rep.getSeverity().getOverrideLevel());

        // Verify priority queue updated
        Request queuedReq = priorityQueue.find(r -> "REQ03".equals(r.getId()));
        assertNotNull(queuedReq);
        assertEquals(1, queuedReq.getSeverity());
        long originalArrivalNo = queuedReq.getArrivalNo();

        // 3. Field officer submits updated recount: immediateRisk jumps back up (red=7 -> Sev 5)
        Report updatedData = new Report("REP03", "officer1", 2);
        updatedData.setCounts(new ReportCounts(1500, 7, 5, 80, 0, 0, 150)); // red=7 >= 5 -> Sev 5
        updatedData.setHoursWithoutSupplies(14);
        updatedData.setOngoingThreat(false);
        updatedData.setRoadAccessObserved("OPEN");
        updatedData.setNeeds(rep.getNeeds());
        updatedData.setNeedOrder(rep.getNeedOrder());

        int initialAlerts = alertRepo.findAll().size();
        Report updated = reportService.updateReport("REP03", updatedData, officerUser);

        // Override must be cleared!
        assertNull(updated.getSeverity().getOverrideLevel(), "Override must be removed upon recount update");
        assertEquals(5, updated.getSeverity().getCalculated());
        assertEquals(5, updated.getSeverity().getEffectiveLevel());

        // Request in priority queue must have its severity restored to 5, preserving arrivalNo
        Request updatedReq = priorityQueue.find(r -> "REQ03".equals(r.getId()));
        assertNotNull(updatedReq);
        assertEquals(5, updatedReq.getSeverity());
        assertEquals(originalArrivalNo, updatedReq.getArrivalNo(), "Arrival number must be preserved across recalculation updates");

        // An alert must have been logged for the override removal
        List<AlertEntry> alerts = alertRepo.findAll();
        assertTrue(alerts.size() > initialAlerts);
        boolean foundAlert = alerts.stream().anyMatch(a -> a.getMessage().contains("previous override removed"));
        assertTrue(foundAlert, "Alert must be generated when previous override is cleared");
    }

    @Test
    @DisplayName("Override Authority: Control Room requires reason; Field Officer gets 403 SecurityException")
    void testOverridePermissionsAndValidation() {
        // Control room missing reason throws IllegalArgumentException
        IllegalArgumentException reasonEx = assertThrows(IllegalArgumentException.class, () ->
                reportService.overrideSeverity("REP01", 3, "   ", controlUser));
        assertTrue(reasonEx.getMessage().contains("mandatory justification"));

        // Field officer attempting override throws SecurityException (HTTP 403)
        SecurityException secEx = assertThrows(SecurityException.class, () ->
                reportService.overrideSeverity("REP01", 3, "Field officer override", officerUser));
        assertTrue(secEx.getMessage().contains("Only Control Room operators"));
    }

    @Test
    @DisplayName("Duplicate Prevention: Reject identical open report from same officer in same region within 10 min")
    void testDuplicateReportPrevention() {
        // First report
        Report r1 = new Report("REP_DUP_1", "officer1", 3);
        r1.setCounts(new ReportCounts(500, 2, 5, 40, 0, 0, 50));
        r1.setHoursWithoutSupplies(5);
        r1.setOngoingThreat(false);
        r1.setRoadAccessObserved("OPEN");
        r1.setNeeds(new ReportNeeds(new ReportNeeds.RescueNeed(10)));
        r1.setNeedOrder(List.of("RESCUE"));

        reportService.submit(r1, officerUser, false);

        // Immediate identical second report for same region
        Report r2 = new Report("REP_DUP_2", "officer1", 3);
        r2.setCounts(new ReportCounts(500, 2, 5, 40, 0, 0, 50));
        r2.setHoursWithoutSupplies(5);
        r2.setOngoingThreat(false);
        r2.setRoadAccessObserved("OPEN");
        r2.setNeeds(new ReportNeeds(new ReportNeeds.RescueNeed(10)));
        r2.setNeedOrder(List.of("RESCUE"));

        // Must reject without allowDuplicate override
        IllegalArgumentException dupEx = assertThrows(IllegalArgumentException.class, () ->
                reportService.submit(r2, officerUser, false));
        assertTrue(dupEx.getMessage().contains("Duplicate report detected"));

        // Should succeed when allowDuplicate = true
        assertDoesNotThrow(() -> reportService.submit(r2, officerUser, true));
    }
}
