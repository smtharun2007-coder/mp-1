package com.disasterrelief.logic;

import com.disasterrelief.graph.DijkstraResult;
import com.disasterrelief.graph.Graph;
import com.disasterrelief.model.Road;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure logic unit tests verifying hand-crafted graph algorithms:
 * BFS & DFS reachability, Dijkstra shortest path with positive weights,
 * and isolated region detection upon road closure.
 * Requires no database.
 */
public class GraphAlgorithmTest {

    private Graph graph;
    private List<Road> roads;

    @BeforeEach
    public void setUp() {
        graph = new Graph();
        roads = new ArrayList<>();

        // Topology:
        // Depot 11 connected to Region 1 (15 min)
        // Region 1 connected to Region 2 (20 min)
        // Region 2 connected to Region 3 (25 min)
        // Region 3 connected to Region 4 (10 min) - Single road bridge to Village 4!
        // Depot 12 connected to Region 2 (30 min)
        roads.add(new Road("RD01", 11, 1, 15, Road.RoadStatus.OPEN, ""));
        roads.add(new Road("RD02", 1, 2, 20, Road.RoadStatus.OPEN, ""));
        roads.add(new Road("RD03", 2, 3, 25, Road.RoadStatus.OPEN, ""));
        roads.add(new Road("RD04", 3, 4, 10, Road.RoadStatus.OPEN, "")); // Bridge to node 4
        roads.add(new Road("RD05", 12, 2, 30, Road.RoadStatus.OPEN, ""));

        graph.buildFromRoads(roads);
    }

    @Test
    @DisplayName("BFS and DFS should agree on reachability for connected graph")
    public void testBfsAndDfsReachability() {
        Set<Integer> bfsReachable = graph.bfsReachableFrom(11);
        Set<Integer> dfsReachable = graph.dfsReachableFrom(11);

        assertEquals(bfsReachable, dfsReachable, "BFS and DFS reachability sets must be identical");
        assertTrue(bfsReachable.containsAll(List.of(1, 2, 3, 4, 11, 12)));
    }

    @Test
    @DisplayName("Dijkstra should find exact shortest path and travel time on positive weights")
    public void testDijkstraShortestPath() {
        // From Depot 11 to Village 4:
        // Route: 11 -> 1 -> 2 -> 3 -> 4
        // Expected distance: 15 + 20 + 25 + 10 = 70 min
        DijkstraResult result = graph.findShortestPath(11, 4);

        assertTrue(result.isReachable());
        assertEquals(70, result.getDistance());
        assertEquals(List.of(11, 1, 2, 3, 4), result.getPath());
    }

    @Test
    @DisplayName("Dijkstra should choose faster alternative route if one exists")
    public void testDijkstraFasterAlternative() {
        // Add a direct bypass road from Depot 11 to Region 3 (travel time 25 min)
        roads.add(new Road("RD_FAST", 11, 3, 25, Road.RoadStatus.OPEN, ""));
        graph.buildFromRoads(roads);

        // New route from 11 to 4: 11 -> 3 -> 4 (25 + 10 = 35 min instead of 70 min)
        DijkstraResult result = graph.findShortestPath(11, 4);
        assertTrue(result.isReachable());
        assertEquals(35, result.getDistance());
        assertEquals(List.of(11, 3, 4), result.getPath());
    }

    @Test
    @DisplayName("Closing bridge road should isolate village and render it unreachable")
    public void testRoadClosureIsolatesVillage() {
        // Close bridge road RD04 (node 3 to 4) due to landslide
        roads.get(3).setStatus(Road.RoadStatus.CLOSED);
        roads.get(3).setClosureReason("Massive landslide after storm");

        // Rebuild graph
        graph.buildFromRoads(roads);

        // Check BFS and DFS reachability from Depot 11
        Set<Integer> reachableFromDepot = graph.reachableFrom(11);
        assertFalse(reachableFromDepot.contains(4), "Village 4 must be unreachable after bridge road closes");

        // Dijkstra to Village 4 must report unreachable
        DijkstraResult result = graph.findShortestPath(11, 4);
        assertFalse(result.isReachable());
        assertEquals(-1, result.getDistance());
        assertTrue(result.getPath().isEmpty());

        // Isolated regions detection
        List<Integer> isolated = graph.findIsolatedRegions(List.of(1, 2, 3, 4), List.of(11, 12));
        assertTrue(isolated.contains(4), "Village 4 should be marked as an isolated region");
        assertEquals(List.of(4), isolated);
    }
}
