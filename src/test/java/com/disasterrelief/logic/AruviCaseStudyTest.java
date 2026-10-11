package com.disasterrelief.logic;

import com.disasterrelief.graph.DijkstraResult;
import com.disasterrelief.model.*;
import com.disasterrelief.service.AruviAllocationPolicy;
import com.disasterrelief.service.ResponseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for District Aruvi Disaster Management Resource Allocation case study.
 * Covers all 11 required scenarios:
 * 1. Initial ranking of all 20 requests
 * 2. Insufficient stock causing partial kit allocation
 * 3. Shelter capacity shortage and unfulfilled evacuation places
 * 4. All rescue teams busy while other rescue requests remain waiting
 * 5. Road closure followed by shortest-route recalculation
 * 6. Closing RD14, RD17, and RD18 to make LOC10 unreachable
 * 7. Restoring a road and confirming reachability returns
 * 8. Updating a waiting request from Low to Critical and reprioritizing it
 * 9. Manual inventory increase and decrease
 * 10. Two requests competing for the same inventory without duplicate allocation
 * 11. A new high-priority request arriving while a rescue mission remains active
 */
public class AruviCaseStudyTest {

    private ResponseService service;

    @BeforeEach
    void setUp() {
        service = new ResponseService(new AruviAllocationPolicy());
        service.seedAruviDataset();
    }

    @Test
    @DisplayName("Scenario 1: Initial ranking of all 20 requests by Severity > Population > Arrival Sequence")
    void test01_initialRankingOfAll20Requests() {
        List<Request> ranked = service.getRankedRequests();
        assertEquals(20, ranked.size(), "All 20 requests must be queued");

        // Expected deterministic triage order:
        // Critical: REQ05 (400, arr 5), REQ11 (350, arr 11), REQ02 (320, arr 2), REQ15 (300, arr 15)
        // High: REQ09 (290, arr 9), REQ07 (260, arr 7), REQ03 (240, arr 3), REQ19 (240, arr 19 - tie broken by arr 3 < 19),
        //       REQ17 (230, arr 17), REQ13 (210, arr 13), REQ01 (180, arr 1)
        // Medium: REQ20 (160, arr 20), REQ04 (150, arr 4), REQ14 (140, arr 14), REQ10 (130, arr 10),
        //         REQ16 (125, arr 16), REQ06 (110, arr 6)
        // Low: REQ18 (80, arr 18), REQ08 (70, arr 8), REQ12 (60, arr 12)
        List<String> expectedOrder = List.of(
                "REQ05", "REQ11", "REQ02", "REQ15",
                "REQ09", "REQ07", "REQ03", "REQ19", "REQ17", "REQ13", "REQ01",
                "REQ20", "REQ04", "REQ14", "REQ10", "REQ16", "REQ06",
                "REQ18", "REQ08", "REQ12"
        );

        System.out.println("=== SCENARIO 1: INITIAL RANKING OF ALL 20 REQUESTS ===");
        System.out.printf("%-6s | %-6s | %-8s | %-10s | %-8s%n", "Rank", "ID", "Severity", "Population", "Arrival");
        System.out.println("-------------------------------------------------------");
        for (int i = 0; i < ranked.size(); i++) {
            Request r = ranked.get(i);
            System.out.printf("%-6d | %-6s | %-8s | %-10d | %-8d%n",
                    (i + 1), r.getId(), ((ReliefRequest) r).getSeverityEnum().getLabel(),
                    r.getAffectedPopulation(), r.getArrivalNo());
            assertEquals(expectedOrder.get(i), r.getId(), "Mismatch at rank " + (i + 1));
        }

        // Explicit check of deterministic tie-breaker: REQ03 vs REQ19 (same sev 3, same pop 240)
        int indexReq03 = -1, indexReq19 = -1;
        for (int i = 0; i < ranked.size(); i++) {
            if (ranked.get(i).getId().equals("REQ03")) indexReq03 = i;
            if (ranked.get(i).getId().equals("REQ19")) indexReq19 = i;
        }
        assertTrue(indexReq03 < indexReq19, "REQ03 (arrival 3) must rank before REQ19 (arrival 19)");
    }

    @Test
    @DisplayName("Scenario 2: Insufficient stock causing partial kit allocation")
    void test02_insufficientStockCausingPartialKitAllocation() {
        // Initial stock: DEP01 (180 food, 90 med), DEP02 (120 food, 60 med) -> Total: 300 food, 150 med
        // Allocate first 4 Critical requests:
        // REQ05: food 70, med 40 (remaining: 230 food, 110 med)
        // REQ11: food 65, med 30 (remaining: 165 food, 80 med)
        // REQ02: food 60, med 35 (remaining: 105 food, 45 med)
        // REQ15: food 55, med 28 (remaining: 50 food, 17 med)
        assertTrue(service.allocateRequest("REQ05"));
        assertTrue(service.allocateRequest("REQ11"));
        assertTrue(service.allocateRequest("REQ02"));
        assertTrue(service.allocateRequest("REQ15"));

        // 5th request: REQ09 needs 50 food and 25 medicine kits
        // Available: exactly 50 food, but only 17 medicine kits!
        ReliefRequest req09 = service.getRequests().get("REQ09");
        assertEquals(50, req09.getFoodNeeded());
        assertEquals(25, req09.getMedicineNeeded());

        boolean allocated = service.allocateRequest("REQ09");
        assertTrue(allocated, "Partial allocation should be committed");

        // Verify partial fulfillment state
        assertEquals(RequestStatus.PARTIALLY_ALLOCATED, req09.getStatus());
        assertEquals(50, req09.getFoodAllocated(), "All 50 food kits allocated");
        assertEquals(17, req09.getMedicineAllocated(), "17 medicine kits allocated (warehouse exhausted)");
        assertEquals(0, req09.getOutstandingFood(), "0 food kits outstanding");
        assertEquals(8, req09.getOutstandingMedicine(), "8 medicine kits remain outstanding");

        // Check depots total available medicine is now 0 and not below zero
        int totalAvailMed = service.getDepots().get("DEP01").getAvailableMedicine() +
                           service.getDepots().get("DEP02").getAvailableMedicine();
        assertEquals(0, totalAvailMed, "Available medicine should be 0");
    }

    @Test
    @DisplayName("Scenario 3: Shelter capacity shortage and unfulfilled evacuation places")
    void test03_shelterCapacityShortageAndUnfulfilledPlaces() {
        // SH01: 80 available, SH02: 50 available. Total available capacity = 130
        // REQ05 needs 45 places -> 45 allocated
        // REQ11 needs 50 places -> 50 allocated (85 reserved, 35 remain)
        assertTrue(service.allocateRequest("REQ05"));
        assertTrue(service.allocateRequest("REQ11"));

        // Next rescue request: REQ02 needs 40 shelter places
        // Only 35 places remain across all reachable shelters
        ReliefRequest req02 = service.getRequests().get("REQ02");
        assertEquals(40, req02.getShelterPlacesRequested());

        // Allocate REQ02
        assertTrue(service.allocateRequest("REQ02"));
        assertEquals(35, req02.getShelterPlacesAllocated(), "Only 35 places allocated up to capacity limit");
        assertEquals(5, req02.getOutstandingShelterPlaces(), "5 places remain unfulfilled/unsheltered");

        // Shelters are now completely full
        int totalAvailShelter = service.getShelters().get("SH01").getAvailableCapacity() +
                               service.getShelters().get("SH02").getAvailableCapacity();
        assertEquals(0, totalAvailShelter, "Total available shelter capacity must be 0");
    }

    @Test
    @DisplayName("Scenario 4: All rescue teams busy while other rescue requests remain waiting")
    void test04_allRescueTeamsBusyWhileOtherRescueRequestsWait() {
        // 3 teams total: TEAM01, TEAM02, TEAM03
        // Allocate REQ05, REQ11, REQ02
        assertTrue(service.allocateRequest("REQ05"));
        assertTrue(service.allocateRequest("REQ11"));
        assertTrue(service.allocateRequest("REQ02"));

        // Confirm all 3 teams are now BUSY
        for (RescueTeam t : service.getRescueTeams().values()) {
            assertFalse(t.isAvailable(), "Team " + t.getId() + " must be BUSY");
            assertEquals(RescueTeam.TeamStatus.BUSY, t.getStatus());
        }

        // 4th rescue request: needs a rescue team
        RescueRequest reqPending = new RescueRequest("REQ_RESCUE_ONLY", "LOC06", 6, 4, 100, 99L,
                RequestStatus.WAITING, 0, 0, true, 20);
        service.addRequest(reqPending);

        // Attempt allocation: all teams busy -> allocation deferred, stays WAITING
        boolean allocated = service.allocateRequest("REQ_RESCUE_ONLY");
        assertFalse(allocated, "Cannot allocate rescue request when all rescue teams are busy");
        assertEquals(RequestStatus.WAITING, reqPending.getStatus(), "Must remain in WAITING status");

        // Release TEAM01 and ensure shelter capacity exists
        service.releaseRescueTeam("TEAM01");
        service.getShelters().get("SH01").releasePlaces(20);
        assertTrue(service.getRescueTeams().get("TEAM01").isAvailable());

        // Now reqPending can be allocated
        boolean allocatedAfterRelease = service.allocateRequest("REQ_RESCUE_ONLY");
        assertTrue(allocatedAfterRelease, "Rescue request allocated successfully once a team was released");
        assertEquals(RequestStatus.ALLOCATED, reqPending.getStatus());
    }

    @Test
    @DisplayName("Scenario 5: Road closure followed by shortest-route recalculation")
    void test05_roadClosureFollowedByShortestRouteRecalculation() {
        // Initially, shortest path between LOC01 and LOC02 is direct road RD01: travel time = 10 min
        DijkstraResult initialRoute = service.findShortestRoute(1, 2);
        assertTrue(initialRoute.isReachable());
        assertEquals(10, initialRoute.getDistance());
        assertEquals(List.of(1, 2), initialRoute.getPath());

        // Close direct road RD01 (LOC01 - LOC02)
        service.closeRoad("RD01", "Severe flooding on RD01");

        // Recalculate shortest route from LOC01 to LOC02
        // Alternative paths:
        // LOC01 -> LOC04 (RD02, 12) -> LOC02 (RD06, 7) = 19 min
        DijkstraResult rerouted = service.findShortestRoute(1, 2);
        assertTrue(rerouted.isReachable(), "Alternative route must exist");
        assertEquals(19, rerouted.getDistance(), "Rerouted distance must be 19 min");
        assertEquals(List.of(1, 4, 2), rerouted.getPath());
    }

    @Test
    @DisplayName("Scenario 6: Closing RD14, RD17, and RD18 to make LOC10 unreachable")
    void test06_closingRD14_RD17_RD18_MakesLOC10Unreachable() {
        // Initially LOC10 (Stonepass, Node 10) is reachable from control room LOC01
        Set<Integer> reachableInitially = service.getReachableNodes(1);
        assertTrue(reachableInitially.contains(10), "LOC10 initially reachable");

        // Close all 3 roads connected to LOC10
        service.closeRoad("RD14", "Landslide at Eastwood-Stonepass");
        service.closeRoad("RD17", "Bridge collapse at Pinebridge-Stonepass");
        service.closeRoad("RD18", "Road washed out at Meadowpark-Stonepass");

        // BFS reachability check
        Set<Integer> reachableAfterClosures = service.getReachableNodes(1);
        assertFalse(reachableAfterClosures.contains(10), "LOC10 must be unreachable from LOC01");

        // Dijkstra shortest path confirms unreachable
        DijkstraResult route = service.findShortestRoute(1, 10);
        assertFalse(route.isReachable());

        // Attempting to allocate request at LOC10 (REQ09 or REQ18) marks it BLOCKED
        boolean allocated = service.allocateRequest("REQ09");
        assertFalse(allocated, "Cannot allocate delivery to cut-off region");
        assertEquals(RequestStatus.BLOCKED, service.getRequests().get("REQ09").getStatus(),
                "Request at unreachable region must be marked BLOCKED");
    }

    @Test
    @DisplayName("Scenario 7: Restoring a road and confirming reachability returns")
    void test07_restoringRoadRestoresReachability() {
        // Cut off LOC10
        service.closeRoad("RD14", "Cut off");
        service.closeRoad("RD17", "Cut off");
        service.closeRoad("RD18", "Cut off");
        assertFalse(service.getReachableNodes(1).contains(10));

        // Reopen RD17 (Pinebridge LOC08 to Stonepass LOC10, 12 min)
        service.reopenRoad("RD17");

        // Reachability should return
        Set<Integer> reachableNow = service.getReachableNodes(1);
        assertTrue(reachableNow.contains(10), "LOC10 reachability must return after reopening RD17");

        DijkstraResult route = service.findShortestRoute(1, 10);
        assertTrue(route.isReachable());
        assertTrue(route.getPath().contains(8), "Route must travel through LOC08 (Pinebridge)");
        assertEquals(10, route.getPath().get(route.getPath().size() - 1), "Destination must be LOC10");
    }

    @Test
    @DisplayName("Scenario 8: Updating waiting request from Low to Critical and reprioritizing it")
    void test08_updatingWaitingRequestFromLowToCriticalReprioritizesIt() {
        // REQ12 is initially Low (Severity 1, pop 60, arr 12) -> last place (index 19)
        List<Request> initialQueue = service.getRankedRequests();
        assertEquals("REQ12", initialQueue.get(19).getId(), "REQ12 should initially be at the bottom");

        // Upgrade REQ12 to Critical (Severity 4)
        service.updateRequestSeverity("REQ12", ReliefRequest.Severity.CRITICAL);

        // Verify immediate reprioritization in PriorityQueue
        List<Request> updatedQueue = service.getRankedRequests();
        ReliefRequest req12 = service.getRequests().get("REQ12");
        assertEquals(4, req12.getSeverity());

        // Critical tier requests: REQ05 (400), REQ11 (350), REQ02 (320), REQ15 (300), REQ12 (60)
        // REQ12 should now be ranked 5th overall (index 4)
        assertEquals("REQ12", updatedQueue.get(4).getId(), "REQ12 must now be in 5th place in Critical tier");

        // Requirements intact
        assertEquals(12, req12.getFoodNeeded());
        assertEquals(4, req12.getMedicineNeeded());
    }

    @Test
    @DisplayName("Scenario 9: Manual inventory increase and decrease")
    void test09_manualInventoryIncreaseAndDecrease() {
        Depot d1 = service.getDepots().get("DEP01");
        int initialFood = d1.getAvailableFood();
        int initialMed = d1.getAvailableMedicine();

        assertEquals(180, initialFood);
        assertEquals(90, initialMed);

        // Increase inventory by 50 food and 20 medicine
        service.updateStock("DEP01", 50, 20);
        assertEquals(230, d1.getAvailableFood());
        assertEquals(110, d1.getAvailableMedicine());

        // Decrease inventory by 30 food and 10 medicine
        service.updateStock("DEP01", -30, -10);
        assertEquals(200, d1.getAvailableFood());
        assertEquals(100, d1.getAvailableMedicine());

        // Verify audit log has entries
        boolean logged = service.getAuditHistory().stream().anyMatch(h -> h.contains("MANUAL_STOCK_UPDATE"));
        assertTrue(logged, "Audit history must record manual stock adjustments");
    }

    @Test
    @DisplayName("Scenario 10: Two requests competing for the same inventory without duplicate allocation")
    void test10_competingRequestsWithoutDuplicateAllocation() {
        // Adjust DEP01 to exactly 50 food kits and 0 med kits; DEP02 to 0
        service.getDepots().get("DEP01").hydrate(0, 50, 0, 0);
        service.getDepots().get("DEP02").hydrate(0, 0, 0, 0);

        // Create two competing requests
        SupplyRequest comp1 = new SupplyRequest("COMP01", "LOC04", 4, 3, 200, 101L, RequestStatus.WAITING, 40, 0);
        SupplyRequest comp2 = new SupplyRequest("COMP02", "LOC04", 4, 2, 200, 102L, RequestStatus.WAITING, 40, 0);
        service.addRequest(comp1);
        service.addRequest(comp2);

        // COMP01 is higher severity (High vs Medium) -> allocated first
        assertTrue(service.allocateRequest("COMP01"));
        assertEquals(40, comp1.getFoodAllocated());
        assertEquals(0, comp1.getOutstandingFood());
        assertEquals(10, service.getDepots().get("DEP01").getAvailableFood(), "10 kits remain");

        // COMP02 is allocated next -> only 10 kits available
        assertTrue(service.allocateRequest("COMP02"));
        assertEquals(10, comp2.getFoodAllocated(), "Only 10 food kits allocated to COMP02");
        assertEquals(30, comp2.getOutstandingFood(), "30 food kits outstanding");
        assertEquals(0, service.getDepots().get("DEP01").getAvailableFood(), "Warehouse exactly 0");

        // Verify total allocated kits is exactly 50, never allocated twice
        assertEquals(50, comp1.getFoodAllocated() + comp2.getFoodAllocated());
    }

    @Test
    @DisplayName("Scenario 11: New high-priority request arriving while a rescue mission remains active")
    void test11_newHighPriorityRequestWhileMissionActive() {
        // Determine which team is dispatched for REQ05
        String assignedTeam = service.previewAllocation("REQ05").getTeamId();
        assertNotNull(assignedTeam);
        assertTrue(service.allocateRequest("REQ05"));

        RescueTeam activeTeam = service.getRescueTeams().get(assignedTeam);
        assertEquals(RescueTeam.TeamStatus.BUSY, activeTeam.getStatus(), "Assigned team must be BUSY on mission");

        // A new urgent Critical request arrives: REQ_NEW (Sev 4, Pop 600, Arrival 21)
        RescueRequest reqNew = new RescueRequest("REQ_NEW", "LOC04", 4, 4, 600, 21L,
                RequestStatus.WAITING, 50, 20, true, 30);
        service.addRequest(reqNew);

        // REQ_NEW is now the highest priority request in the waiting queue (Pop 600 > REQ11's 350)
        Request topWaiting = service.getRankedRequests().get(0);
        assertEquals("REQ_NEW", topWaiting.getId(), "New urgent request must rank at top of waiting queue");

        // However, active team's mission is NOT automatically interrupted
        assertEquals(RescueTeam.TeamStatus.BUSY, activeTeam.getStatus(),
                "Active team's mission must not be interrupted");

        // Release team after mission completion
        service.releaseRescueTeam(assignedTeam);
        assertEquals(RescueTeam.TeamStatus.AVAILABLE, activeTeam.getStatus());
    }
}
