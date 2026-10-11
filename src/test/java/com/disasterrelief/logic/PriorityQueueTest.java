package com.disasterrelief.logic;

import com.disasterrelief.model.*;
import com.disasterrelief.queue.RequestPriorityQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure logic unit tests verifying multi-criteria priority ordering,
 * tie-breaking determinism, and heap re-ordering upon request updates.
 * Requires no database.
 */
public class PriorityQueueTest {

    private RequestPriorityQueue pq;

    @BeforeEach
    public void setUp() {
        pq = new RequestPriorityQueue();
    }

    @Test
    @DisplayName("Higher severity must be prioritised over lower severity")
    public void testSeverityOrdering() {
        // req1: Sev 3, pop 1000, arrival 1
        Request req1 = new SupplyRequest("REQ1", "REP1", 1, 3, 1000, 1L, RequestStatus.WAITING, "officer", UrgentNeed.MEDICINE, 50, 50);
        // req2: Sev 5, pop 200, arrival 2 (higher severity, lower pop, arrived later)
        Request req2 = new SupplyRequest("REQ2", "REP2", 2, 5, 200, 2L, RequestStatus.WAITING, "officer", UrgentNeed.MEDICINE, 20, 20);

        pq.add(req1);
        pq.add(req2);

        // Sev 5 must poll first
        Request first = pq.poll();
        assertEquals("REQ2", first.getId(), "Severity 5 should be ranked before Severity 3");
        assertEquals(5, first.getSeverity());

        Request second = pq.poll();
        assertEquals("REQ1", second.getId());
    }

    @Test
    @DisplayName("Equal severity ties must be broken by larger affected population")
    public void testPopulationTieBreaking() {
        // Both Sev 4, but REQ_B has 3000 people vs 1000 people
        Request reqA = new SupplyRequest("REQ_A", "REP_A", 1, 4, 1000, 1L, RequestStatus.WAITING, "officer", UrgentNeed.MEDICINE, 10, 10);
        Request reqB = new SupplyRequest("REQ_B", "REP_B", 2, 4, 3000, 2L, RequestStatus.WAITING, "officer", UrgentNeed.MEDICINE, 30, 30);

        pq.add(reqA);
        pq.add(reqB);

        Request first = pq.poll();
        assertEquals("REQ_B", first.getId(), "Larger affected population should poll first for equal severity");
        assertEquals(3000, first.getAffectedPopulation());
    }

    @Test
    @DisplayName("Equal severity and population ties must be broken by earlier arrival sequence (FIFO)")
    public void testArrivalOrderTieBreaking() {
        // Same severity 5, same pop 500, but REQ_EARLY arrived at seq 10, REQ_LATE at seq 25
        Request reqLate = new RescueRequest("REQ_LATE", "REP_LATE", 3, 5, 500, 25L, RequestStatus.WAITING, "officer", UrgentNeed.RESCUE, 100);
        Request reqEarly = new RescueRequest("REQ_EARLY", "REP_EARLY", 4, 5, 500, 10L, RequestStatus.WAITING, "officer", UrgentNeed.RESCUE, 100);

        pq.add(reqLate);
        pq.add(reqEarly);

        Request first = pq.poll();
        assertEquals("REQ_EARLY", first.getId(), "Earlier arrival order should break tie deterministically");
        assertEquals(10L, first.getArrivalNo());
    }

    @Test
    @DisplayName("Dynamic severity update should remove, reinsert, and re-rank the request")
    public void testUpdateAndReRank() {
        Request reqLow = new SupplyRequest("REQ_LOW", "REP1", 1, 2, 500, 1L, RequestStatus.WAITING, "officer", UrgentNeed.FOOD, 10, 10);
        Request reqHigh = new SupplyRequest("REQ_HIGH", "REP2", 2, 4, 500, 2L, RequestStatus.WAITING, "officer", UrgentNeed.FOOD, 10, 10);

        pq.add(reqLow);
        pq.add(reqHigh);

        assertEquals("REQ_HIGH", pq.peek().getId());

        // Escalate REQ_LOW to severity 5
        reqLow.setSeverity(5);
        pq.update(reqLow);

        // Now REQ_LOW must become the new top rank
        assertEquals("REQ_LOW", pq.peek().getId(), "Escalated request should become top priority after update");
        Request top = pq.poll();
        assertEquals("REQ_LOW", top.getId());
        assertEquals(5, top.getSeverity());
    }

    @Test
    @DisplayName("Ordered snapshot should produce list matching exact priority hierarchy")
    public void testOrderedSnapshot() {
        Request r1 = new SupplyRequest("R1", "REP1", 1, 5, 500, 1L, RequestStatus.WAITING, "officer", UrgentNeed.MEDICINE, 10, 10);
        Request r2 = new SupplyRequest("R2", "REP2", 2, 5, 1000, 2L, RequestStatus.WAITING, "officer", UrgentNeed.MEDICINE, 10, 10); // Higher pop
        Request r3 = new SupplyRequest("R3", "REP3", 3, 3, 2000, 3L, RequestStatus.WAITING, "officer", UrgentNeed.MEDICINE, 10, 10); // Lower sev

        pq.add(r1);
        pq.add(r2);
        pq.add(r3);

        List<Request> snapshot = pq.getOrderedSnapshot();
        assertEquals(3, snapshot.size());
        assertEquals("R2", snapshot.get(0).getId()); // Sev 5, Pop 1000
        assertEquals("R1", snapshot.get(1).getId()); // Sev 5, Pop 500
        assertEquals("R3", snapshot.get(2).getId()); // Sev 3, Pop 2000
    }
}
