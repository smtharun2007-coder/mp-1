package com.disasterrelief.service;

import com.disasterrelief.graph.DijkstraResult;
import com.disasterrelief.graph.Graph;
import com.disasterrelief.model.*;
import com.disasterrelief.queue.RequestPriorityQueue;

import java.util.*;

/**
 * Core disaster management response service demonstrating abstraction, composition,
 * encapsulated resources, and graph algorithms for District Aruvi.
 *
 * Concepts Demonstrated:
 * - Composition: orchestrates AllocationPolicy, Graph, RequestPriorityQueue, and HashMaps
 * - HashMaps: O(1) lookup tables for regions, depots, shelters, rescue teams, roads, requests
 * - PriorityQueue: binary min/max-heap triage ranking with explicit deterministic tie-breakers
 * - Graph Traversals: BFS / DFS with HashSet for reachability; Dijkstra min-heap for minimum travel times
 * - Encapsulation: reserveStock(), releaseTeam(), reservePlaces()
 * - Audit Trail: immutable chronological history of operational events
 */
public class ResponseService {

    private final AllocationPolicy policy;
    private final Graph graph;
    private final RequestPriorityQueue priorityQueue;

    // HashMaps for records and inventory
    private final Map<String, AffectedRegion> regions = new HashMap<>();
    private final Map<String, Depot> depots = new HashMap<>();
    private final Map<String, RescueTeam> rescueTeams = new HashMap<>();
    private final Map<String, Shelter> shelters = new HashMap<>();
    private final Map<String, ReliefRequest> requests = new HashMap<>();
    private final Map<String, Road> roads = new HashMap<>();
    private final List<String> auditHistory = new ArrayList<>();

    public ResponseService() {
        this(new AruviAllocationPolicy());
    }

    public ResponseService(AllocationPolicy policy) {
        this.policy = policy;
        this.graph = new Graph();
        this.priorityQueue = new RequestPriorityQueue();
    }

    /**
     * Seeds the standard District Aruvi dataset:
     * - 10 Locations LOC01-LOC10
     * - 2 Depots DEP01, DEP02 (300 food, 150 medicine total)
     * - 3 Rescue Teams TEAM01-TEAM03 (AVAILABLE)
     * - 2 Shelters SH01, SH02 (130 available places total)
     * - 20 Road connections RD01-RD20 (all OPEN)
     * - 20 Relief requests REQ01-REQ20
     */
    public void seedAruviDataset() {
        clear();

        // 1. Locations
        addRegion(new AffectedRegion("LOC01", 1, "Aruvi Central", 5000, true)); // Control room
        addRegion(new AffectedRegion("LOC02", 2, "Northbank", 3000, false));
        addRegion(new AffectedRegion("LOC03", 3, "Hillview", 2500, false));
        addRegion(new AffectedRegion("LOC04", 4, "Riverside", 3500, false));
        addRegion(new AffectedRegion("LOC05", 5, "Greenfield", 2000, false));
        addRegion(new AffectedRegion("LOC06", 6, "Eastwood", 2800, false));
        addRegion(new AffectedRegion("LOC07", 7, "Lakeview", 4000, false));
        addRegion(new AffectedRegion("LOC08", 8, "Pinebridge", 3200, false));
        addRegion(new AffectedRegion("LOC09", 9, "Meadowpark", 1800, false));
        addRegion(new AffectedRegion("LOC10", 10, "Stonepass", 1500, false));

        // 2. Depots (DEP01 at LOC01, DEP02 at LOC02)
        addDepot(new Depot("DEP01", "Central Logistics Depot", "LOC01", 1, 90, 180));
        addDepot(new Depot("DEP02", "Northbank Regional Depot", "LOC02", 2, 60, 120));

        // 3. Rescue Teams (all AVAILABLE)
        addRescueTeam(new RescueTeam("TEAM01", "Rapid Response Alpha", "LOC01", 1, RescueTeam.TeamStatus.AVAILABLE));
        addRescueTeam(new RescueTeam("TEAM02", "Rapid Response Bravo", "LOC02", 2, RescueTeam.TeamStatus.AVAILABLE));
        addRescueTeam(new RescueTeam("TEAM03", "Rescue Unit Charlie", "LOC08", 8, RescueTeam.TeamStatus.AVAILABLE));

        // 4. Shelters
        Shelter s1 = new Shelter("SH01", "Aruvi Relief Centre", "LOC01", 1, 100);
        s1.hydrate(100, 20); // initially occupied 20 -> available 80
        addShelter(s1);

        Shelter s2 = new Shelter("SH02", "Northbank Community Shelter", "LOC02", 2, 60);
        s2.hydrate(60, 10); // initially occupied 10 -> available 50
        addShelter(s2);

        // 5. Roads (20 undirected connections, all open)
        addRoad(new Road("RD01", 1, 2, 10, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD02", 1, 4, 12, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD03", 1, 7, 15, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD04", 2, 3, 8, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD05", 2, 5, 14, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD06", 2, 4, 7, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD07", 3, 6, 11, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD08", 3, 8, 16, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD09", 4, 5, 9, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD10", 4, 7, 8, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD11", 5, 6, 10, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD12", 5, 9, 13, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD13", 6, 8, 9, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD14", 6, 10, 18, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD15", 7, 9, 10, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD16", 7, 8, 14, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD17", 8, 10, 12, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD18", 9, 10, 15, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD19", 2, 7, 18, Road.RoadStatus.OPEN, ""));
        addRoad(new Road("RD20", 4, 8, 20, Road.RoadStatus.OPEN, ""));

        rebuildGraph();

        // 6. Relief Requests (REQ01 - REQ20)
        // High = 3, Critical = 4, Medium = 2, Low = 1
        addRequest(new RescueRequest("REQ01", "LOC02", 2, 3, 180, 1L, RequestStatus.WAITING, 40, 20, true, 25));
        addRequest(new RescueRequest("REQ02", "LOC04", 4, 4, 320, 2L, RequestStatus.WAITING, 60, 35, true, 40));
        addRequest(new RescueRequest("REQ03", "LOC03", 3, 3, 240, 3L, RequestStatus.WAITING, 35, 18, true, 20));
        addRequest(new SupplyRequest("REQ04", "LOC05", 5, 2, 150, 4L, RequestStatus.WAITING, 30, 10));
        addRequest(new RescueRequest("REQ05", "LOC07", 7, 4, 400, 5L, RequestStatus.WAITING, 70, 40, true, 45));
        addRequest(new SupplyRequest("REQ06", "LOC06", 6, 2, 110, 6L, RequestStatus.WAITING, 25, 12));
        requests.get("REQ06").setShelterPlacesRequested(10); // REQ06 asks for 10 shelter places without rescue team
        addRequest(new RescueRequest("REQ07", "LOC08", 8, 3, 260, 7L, RequestStatus.WAITING, 45, 20, true, 30));
        addRequest(new SupplyRequest("REQ08", "LOC09", 9, 1, 70, 8L, RequestStatus.WAITING, 15, 5));
        addRequest(new RescueRequest("REQ09", "LOC10", 10, 3, 290, 9L, RequestStatus.WAITING, 50, 25, true, 35));
        addRequest(new SupplyRequest("REQ10", "LOC02", 2, 2, 130, 10L, RequestStatus.WAITING, 20, 8));
        addRequest(new RescueRequest("REQ11", "LOC04", 4, 4, 350, 11L, RequestStatus.WAITING, 65, 30, true, 50));
        addRequest(new SupplyRequest("REQ12", "LOC05", 5, 1, 60, 12L, RequestStatus.WAITING, 12, 4));
        addRequest(new RescueRequest("REQ13", "LOC07", 7, 3, 210, 13L, RequestStatus.WAITING, 35, 15, true, 20));
        addRequest(new SupplyRequest("REQ14", "LOC03", 3, 2, 140, 14L, RequestStatus.WAITING, 28, 10));
        addRequest(new RescueRequest("REQ15", "LOC06", 6, 4, 300, 15L, RequestStatus.WAITING, 55, 28, true, 30));
        addRequest(new SupplyRequest("REQ16", "LOC08", 8, 2, 125, 16L, RequestStatus.WAITING, 22, 9));
        addRequest(new RescueRequest("REQ17", "LOC09", 9, 3, 230, 17L, RequestStatus.WAITING, 38, 18, true, 25));
        addRequest(new SupplyRequest("REQ18", "LOC10", 10, 1, 80, 18L, RequestStatus.WAITING, 18, 6));
        addRequest(new RescueRequest("REQ19", "LOC02", 2, 3, 240, 19L, RequestStatus.WAITING, 42, 22, true, 20));
        addRequest(new SupplyRequest("REQ20", "LOC04", 4, 2, 160, 20L, RequestStatus.WAITING, 30, 12));

        auditHistory.add("System initialized with 10 locations, 2 depots, 3 rescue teams, 2 shelters, 20 roads, and 20 relief requests.");
    }

    public void clear() {
        regions.clear();
        depots.clear();
        rescueTeams.clear();
        shelters.clear();
        roads.clear();
        requests.clear();
        priorityQueue.rebuild(Collections.emptyList());
        auditHistory.clear();
    }

    public void addRegion(AffectedRegion region) {
        regions.put(region.getLocationCode(), region);
        graph.addNode(region.getNode());
    }

    public void addDepot(Depot depot) {
        depots.put(depot.getId(), depot);
    }

    public void addRescueTeam(RescueTeam team) {
        rescueTeams.put(team.getId(), team);
    }

    public void addShelter(Shelter shelter) {
        shelters.put(shelter.getId(), shelter);
    }

    public void addRoad(Road road) {
        roads.put(road.getId(), road);
        rebuildGraph();
    }

    public void addRequest(ReliefRequest req) {
        requests.put(req.getId(), req);
        priorityQueue.add(req);
    }

    public void rebuildGraph() {
        graph.buildFromRoads(roads.values());
    }

    /**
     * Returns waiting requests sorted in strict priority order:
     * Severity DESC > Population DESC > Arrival sequence ASC
     */
    public List<Request> getRankedRequests() {
        return priorityQueue.getOrderedSnapshot();
    }

    /**
     * Evaluates allocation feasibility without committing resources.
     */
    public AllocationPreview previewAllocation(String requestId) {
        ReliefRequest req = requests.get(requestId);
        if (req == null) throw new IllegalArgumentException("Unknown request ID: " + requestId);
        return policy.evaluate(req, graph, depots.values(), rescueTeams.values(), shelters.values());
    }

    /**
     * Two-phase atomic allocation commit.
     * Enforces:
     * - Reachability verification via Dijkstra
     * - Encapsulated reserveStock() preventing negative stock and double-allocation
     * - Encapsulated reserveTeam() and releaseTeam()
     * - Encapsulated reservePlaces() preventing overcapacity
     * - Partial allocation tracking: stores allocated and outstanding quantities separately
     */
    public synchronized boolean allocateRequest(String requestId) {
        ReliefRequest req = requests.get(requestId);
        if (req == null) return false;

        AllocationPreview plan = previewAllocation(requestId);
        if (!plan.isReachable()) {
            req.setStatus(RequestStatus.BLOCKED);
            priorityQueue.update(req);
            auditHistory.add("REQUEST_BLOCKED: " + requestId + " marked BLOCKED because region is unreachable.");
            return false;
        }

        if (!plan.isFeasible()) {
            auditHistory.add("ALLOCATION_DEFERRED: " + requestId + " remains WAITING: " + plan.getStatusMessage());
            return false;
        }

        // 1. Commit Depot Stock Reservations
        for (AllocationPreview.DepotPlan dp : plan.getDepotPlans()) {
            Depot depot = depots.get(dp.getDepotId());
            if (depot != null) {
                boolean reserved = depot.reserveStock(dp.getMedicineDrawn(), dp.getFoodDrawn());
                if (!reserved) {
                    throw new IllegalStateException("Depot " + dp.getDepotId() + " stock exhausted during commit.");
                }
                auditHistory.add("STOCK_RESERVED: Depot " + dp.getDepotId() + " reserved " + dp.getFoodDrawn() +
                        " food kits, " + dp.getMedicineDrawn() + " medicine kits for " + requestId);
            }
        }
        req.setFoodAllocated(req.getFoodAllocated() + plan.getFoodAllocated());
        req.setMedicineAllocated(req.getMedicineAllocated() + plan.getMedicineAllocated());

        // 2. Commit Rescue Team Reservation (at most one active mission per team)
        if (plan.getTeamId() != null) {
            RescueTeam team = rescueTeams.get(plan.getTeamId());
            if (team != null) {
                boolean teamReserved = team.reserveTeam();
                if (!teamReserved) {
                    throw new IllegalStateException("Team " + plan.getTeamId() + " already busy.");
                }
                auditHistory.add("TEAM_ASSIGNED: Team " + plan.getTeamId() + " assigned to mission for " + requestId);
            }
        }

        // 3. Commit Shelter Reservation
        if (plan.getShelterId() != null && plan.getPeopleEvacuated() > 0) {
            Shelter shelter = shelters.get(plan.getShelterId());
            if (shelter != null) {
                boolean shelterReserved = shelter.reservePlaces(plan.getPeopleEvacuated());
                if (!shelterReserved) {
                    throw new IllegalStateException("Shelter " + plan.getShelterId() + " capacity exceeded.");
                }
                req.setShelterPlacesAllocated(req.getShelterPlacesAllocated() + plan.getPeopleEvacuated());
                auditHistory.add("SHELTER_RESERVED: Shelter " + plan.getShelterId() + " reserved " +
                        plan.getPeopleEvacuated() + " places for " + requestId);
            }
        }

        // 4. Update Request Fulfillment Status
        boolean foodComplete = (req.getOutstandingFood() == 0);
        boolean medComplete = (req.getOutstandingMedicine() == 0);
        boolean rescueComplete = (!req.isRescueRequired() || req.getOutstandingShelterPlaces() == 0);

        if (foodComplete && medComplete && rescueComplete) {
            req.setStatus(RequestStatus.ALLOCATED);
        } else {
            req.setStatus(RequestStatus.PARTIALLY_ALLOCATED);
        }

        priorityQueue.update(req);
        auditHistory.add("ALLOCATION_COMMITTED: " + requestId + " marked " + req.getStatus());
        return true;
    }

    /**
     * Allocates all waiting requests in priority order until available resources are exhausted.
     */
    public synchronized void allocateAllWaiting() {
        List<Request> ordered = getRankedRequests();
        for (Request r : ordered) {
            if (r.getStatus() == RequestStatus.WAITING ||
                r.getStatus() == RequestStatus.PARTIALLY_ALLOCATED) {
                allocateRequest(r.getId());
            }
        }
    }

    /**
     * Updates waiting request severity and forces binary heap re-heapification.
     */
    public synchronized void updateRequestSeverity(String requestId, ReliefRequest.Severity newSeverity) {
        ReliefRequest req = requests.get(requestId);
        if (req != null && newSeverity != null) {
            req.setSeverityEnum(newSeverity);
            priorityQueue.update(req);
            auditHistory.add("SEVERITY_UPDATED: Request " + requestId + " severity changed to " +
                    newSeverity.getLabel() + " (Level " + newSeverity.getLevel() + "). Queue reprioritized.");
        }
    }

    /**
     * Operator manual inventory update.
     */
    public synchronized void updateStock(String depotId, int foodDelta, int medicineDelta) {
        Depot depot = depots.get(depotId);
        if (depot != null) {
            depot.adjustStock(medicineDelta, foodDelta);
            auditHistory.add("MANUAL_STOCK_UPDATE: Depot " + depotId + " adjusted by " +
                    foodDelta + " food kits, " + medicineDelta + " medicine kits.");
        }
    }

    /**
     * Operator road closure and automatic graph rebuild.
     */
    public synchronized void closeRoad(String roadId, String reason) {
        Road road = roads.get(roadId);
        if (road != null) {
            road.setStatus(Road.RoadStatus.CLOSED);
            road.setReason(reason);
            rebuildGraph();
            auditHistory.add("ROAD_CLOSED: " + roadId + " closed. Reason: " + reason);
        }
    }

    /**
     * Operator road restoration and automatic graph rebuild.
     */
    public synchronized void reopenRoad(String roadId) {
        Road road = roads.get(roadId);
        if (road != null) {
            road.setStatus(Road.RoadStatus.OPEN);
            road.setReason("");
            rebuildGraph();
            auditHistory.add("ROAD_REOPENED: " + roadId + " restored to OPEN.");
        }
    }

    /**
     * Completes an active rescue mission, freeing the team back to AVAILABLE status.
     */
    public synchronized void releaseRescueTeam(String teamId) {
        RescueTeam team = rescueTeams.get(teamId);
        if (team != null) {
            team.releaseTeam();
            auditHistory.add("TEAM_RELEASED: Rescue team " + teamId + " returned to AVAILABLE status.");
        }
    }

    /**
     * Dijkstra's algorithm shortest path route lookup using min-heap.
     */
    public DijkstraResult findShortestRoute(int sourceNode, int targetNode) {
        return graph.findShortestPath(sourceNode, targetNode);
    }

    /**
     * BFS reachability check using HashSet.
     */
    public Set<Integer> getReachableNodes(int startNode) {
        return graph.bfsReachableFrom(startNode);
    }

    /**
     * DFS reachability check using HashSet.
     */
    public Set<Integer> getDfsReachableNodes(int startNode) {
        return graph.dfsReachableFrom(startNode);
    }

    // --- Getters for records & inventory HashMaps ---

    public Map<String, AffectedRegion> getRegions() { return Collections.unmodifiableMap(regions); }
    public Map<String, Depot> getDepots() { return Collections.unmodifiableMap(depots); }
    public Map<String, RescueTeam> getRescueTeams() { return Collections.unmodifiableMap(rescueTeams); }
    public Map<String, Shelter> getShelters() { return Collections.unmodifiableMap(shelters); }
    public Map<String, ReliefRequest> getRequests() { return Collections.unmodifiableMap(requests); }
    public Map<String, Road> getRoads() { return Collections.unmodifiableMap(roads); }
    public List<String> getAuditHistory() { return Collections.unmodifiableList(auditHistory); }
    public Graph getGraph() { return graph; }
    public RequestPriorityQueue getPriorityQueue() { return priorityQueue; }
}
