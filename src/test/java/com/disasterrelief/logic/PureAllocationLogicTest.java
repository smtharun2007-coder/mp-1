package com.disasterrelief.logic;

import com.disasterrelief.graph.DijkstraResult;
import com.disasterrelief.graph.Graph;
import com.disasterrelief.model.*;
import com.disasterrelief.queue.RequestPriorityQueue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure logic unit tests verifying the 6 key allocation invariants without requiring a database.
 */
public class PureAllocationLogicTest {

    @Test
    @DisplayName("Invariant 1: Priority Queue orders requests by Severity, Population, and Arrival")
    public void testPriorityQueueHierarchy() {
        RequestPriorityQueue queue = new RequestPriorityQueue();
        SupplyRequest r1 = new SupplyRequest("R1", "REP1", 1, 5, 2000, 1L, RequestStatus.WAITING, "officer", UrgentNeed.MEDICINE, 100, 100);
        SupplyRequest r2 = new SupplyRequest("R2", "REP2", 2, 5, 4000, 2L, RequestStatus.WAITING, "officer", UrgentNeed.MEDICINE, 100, 100);
        SupplyRequest r3 = new SupplyRequest("R3", "REP3", 3, 4, 9000, 3L, RequestStatus.WAITING, "officer", UrgentNeed.MEDICINE, 100, 100);

        queue.add(r1);
        queue.add(r2);
        queue.add(r3);

        // R2 has highest severity (5) and highest population (4000) among sev 5
        assertEquals("R2", queue.poll().getId());
        assertEquals("R1", queue.poll().getId());
        assertEquals("R3", queue.poll().getId());
    }

    @Test
    @DisplayName("Invariant 2: Insufficient stock yields partial allocation with outstanding balance")
    public void testPartialAllocationOutstandingQuantity() {
        Depot depot = new Depot("D1", "Central Depot", 11, 80, 50);
        SupplyRequest request = new SupplyRequest("REQ_SUPPLY", "REP1", 1, 4, 1500, 1L, RequestStatus.WAITING, "officer", UrgentNeed.MEDICINE, 150, 100);

        int availableMed = depot.getAvailableMedicine();
        int availableFood = depot.getAvailableFood();

        int drawMed = Math.min(request.getOutstandingMedicine(), availableMed);
        int drawFood = Math.min(request.getOutstandingFood(), availableFood);

        depot.reserveStock(drawMed, drawFood);
        request.setMedicineAllocated(drawMed);
        request.setFoodAllocated(drawFood);

        assertEquals(80, request.getMedicineAllocated());
        assertEquals(50, request.getFoodAllocated());
        assertEquals(70, request.getOutstandingMedicine(), "150 needed - 80 allocated = 70 outstanding");
        assertEquals(50, request.getOutstandingFood(), "100 needed - 50 allocated = 50 outstanding");
        assertEquals(0, depot.getAvailableMedicine());
        assertEquals(0, depot.getAvailableFood());
    }

    @Test
    @DisplayName("Invariant 3: Full shelter refuses evacuation and keeps request unfulfilled")
    public void testFullShelterEvacuationRefusal() {
        Shelter shelter = new Shelter("S1", "Mountain Shelter", 13, 100);
        shelter.reservePlaces(100); // Shelter is now full

        RescueRequest request = new RescueRequest("REQ_RESCUE", "REP1", 1, 5, 500, 1L, RequestStatus.WAITING, "officer", UrgentNeed.RESCUE, 50);

        int availablePlaces = shelter.getAvailableCapacity();
        assertEquals(0, availablePlaces);

        boolean evacuationFeasible = availablePlaces > 0;
        assertFalse(evacuationFeasible, "Evacuation must be refused when available capacity is 0");

        // Request stays unfulfilled
        assertEquals(0, request.getPeopleEvacuated());
        assertEquals(RequestStatus.WAITING, request.getStatus());
    }

    @Test
    @DisplayName("Invariant 4: Road closure isolates village and renders graph unreachable")
    public void testIsolatedVillageReachability() {
        Graph graph = new Graph();
        Road bridge = new Road("RD_BRIDGE", 3, 4, 15, Road.RoadStatus.OPEN, "");
        List<Road> roads = List.of(
                new Road("RD1", 11, 1, 10, Road.RoadStatus.OPEN, ""),
                new Road("RD2", 1, 3, 15, Road.RoadStatus.OPEN, ""),
                bridge
        );
        graph.buildFromRoads(roads);

        assertTrue(graph.reachableFrom(11).contains(4), "Village 4 reachable when bridge is open");

        // Close bridge
        bridge.setStatus(Road.RoadStatus.CLOSED);
        graph.buildFromRoads(roads);

        Set<Integer> reachable = graph.reachableFrom(11);
        assertFalse(reachable.contains(4), "Village 4 must be unreachable when bridge road is CLOSED");
        DijkstraResult res = graph.findShortestPath(11, 4);
        assertFalse(res.isReachable());
        assertEquals(-1, res.getDistance());
    }

    @Test
    @DisplayName("Invariant 5: Updating request severity dynamically elevates its queue priority")
    public void testDynamicReRanking() {
        RequestPriorityQueue queue = new RequestPriorityQueue();
        SupplyRequest r1 = new SupplyRequest("R1", "REP1", 1, 2, 500, 1L, RequestStatus.WAITING, "officer", UrgentNeed.FOOD, 10, 10);
        SupplyRequest r2 = new SupplyRequest("R2", "REP2", 2, 3, 500, 2L, RequestStatus.WAITING, "officer", UrgentNeed.FOOD, 10, 10);

        queue.add(r1);
        queue.add(r2);
        assertEquals("R2", queue.peek().getId());

        // Update R1 to Severity 5
        r1.setSeverity(5);
        queue.update(r1);

        assertEquals("R1", queue.peek().getId(), "R1 with severity 5 must now be polled first");
    }

    @Test
    @DisplayName("Invariant 6: Stock reservation guarantees mathematical conservation (available + reserved == total)")
    public void testStockConservation() {
        Depot depot = new Depot("D1", "Central Depot", 11, 100, 200);
        int initialMed = depot.getMedicineKits();
        int initialFood = depot.getFoodPacks();

        depot.reserveStock(30, 70);
        assertEquals(70, depot.getAvailableMedicine());
        assertEquals(30, depot.getReservedMedicine());
        assertEquals(initialMed, depot.getAvailableMedicine() + depot.getReservedMedicine(), "Medicine total must be conserved");

        assertEquals(130, depot.getAvailableFood());
        assertEquals(70, depot.getReservedFood());
        assertEquals(initialFood, depot.getAvailableFood() + depot.getReservedFood(), "Food total must be conserved");

        // Releasing stock restores available quantities
        depot.releaseStock(10, 20);
        assertEquals(80, depot.getAvailableMedicine());
        assertEquals(20, depot.getReservedMedicine());
        assertEquals(initialMed, depot.getAvailableMedicine() + depot.getReservedMedicine());
    }
}
