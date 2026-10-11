package com.disasterrelief.benchmark;

import com.disasterrelief.graph.DijkstraResult;
import com.disasterrelief.graph.Graph;
import com.disasterrelief.model.Road;

import java.util.*;

/**
 * Performance benchmarking utility measuring algorithmic execution times
 * across synthetically generated graphs of varying scales (1,000, 10,000, and 50,000 nodes).
 *
 * Algorithmic Complexity Theoretical Bounds:
 * -------------------------------------------------------------------------------------------------
 * 1. PriorityQueue insert (add) & poll: O(log n)
 *    - Binary min/max-heap structure guarantees logarithmic tree depth.
 * 2. HashMap vertex & edge lookup: O(1) average, O(n) worst-case on heavy hash collisions.
 * 3. Breadth-First Search (BFS) & Depth-First Search (DFS): O(V + E)
 *    - Visits every reachable vertex and traverses every connected incident edge once.
 * 4. Dijkstra Single-Source Shortest Path: O((V + E) log V)
 *    - Uses a PriorityQueue min-heap. Each vertex polled once O(V log V) and edges relaxed O(E log V).
 * 5. PriorityQueue remove-and-reinsert (update): O(n)
 *    - PriorityQueue.remove(Object) performs a linear scan through the backing array O(n),
 *      followed by sift-up / sift-down O(log n) -> total O(n).
 * -------------------------------------------------------------------------------------------------
 */
public class Benchmark {

    public static void main(String[] args) {
        System.out.println("==========================================================================================");
        System.out.println("   DISASTER RELIEF LOGISTICS - GRAPH ALGORITHM PERFORMANCE BENCHMARK");
        System.out.println("==========================================================================================");
        System.out.println(" Hardware: " + System.getProperty("os.name") + " | Java: " + System.getProperty("java.version"));
        System.out.println(" Complexity: BFS/DFS O(V+E) | Dijkstra O((V+E) log V) | PriorityQueue O(log n)");
        System.out.println("==========================================================================================");

        int[] nodeCounts = { 1_000, 10_000, 50_000 };

        System.out.printf("%-12s %-14s %-16s %-18s %-16s%n",
                "Node Count", "Edge Count", "BFS Reachable", "BFS Time (ms)", "Dijkstra (ms)");
        System.out.println("------------------------------------------------------------------------------------------");

        for (int n : nodeCounts) {
            runBenchmarkForSize(n);
        }

        System.out.println("==========================================================================================");
        System.out.println(" Benchmark completed successfully.");
    }

    private static void runBenchmarkForSize(int numNodes) {
        Random rand = new Random(42); // Deterministic seed
        List<Road> roads = new ArrayList<>(numNodes * 3);

        // Generate connected sparse graph: backbone chain plus random cross-links
        for (int i = 1; i < numNodes; i++) {
            int weight = 5 + rand.nextInt(45);
            roads.add(new Road("R_CHAIN_" + i, i, i + 1, weight, Road.RoadStatus.OPEN, ""));
        }

        // Add 2 random cross edges per node to simulate regional road network
        for (int i = 1; i <= numNodes; i++) {
            int target1 = 1 + rand.nextInt(numNodes);
            if (target1 != i) {
                int w = 10 + rand.nextInt(50);
                roads.add(new Road("R_CROSS1_" + i, i, target1, w, Road.RoadStatus.OPEN, ""));
            }
            int target2 = 1 + rand.nextInt(numNodes);
            if (target2 != i) {
                int w = 10 + rand.nextInt(50);
                roads.add(new Road("R_CROSS2_" + i, i, target2, w, Road.RoadStatus.OPEN, ""));
            }
        }

        Graph graph = new Graph();
        graph.buildFromRoads(roads);

        int source = 1;
        int target = numNodes;

        // Warm up JIT
        for (int w = 0; w < 3; w++) {
            graph.bfsReachableFrom(source);
            graph.findShortestPath(source, target);
        }

        // Time BFS
        long startBfs = System.nanoTime();
        Set<Integer> bfsReachable = graph.bfsReachableFrom(source);
        long endBfs = System.nanoTime();
        double bfsTimeMs = (endBfs - startBfs) / 1_000_000.0;

        // Time Dijkstra
        long startDijkstra = System.nanoTime();
        DijkstraResult dijkstraResult = graph.findShortestPath(source, target);
        long endDijkstra = System.nanoTime();
        double dijkstraTimeMs = (endDijkstra - startDijkstra) / 1_000_000.0;

        System.out.printf("%-12s %-14s %-16s %-18.3f %-16.3f%n",
                String.format("%,d", numNodes),
                String.format("%,d", roads.size()),
                String.format("%,d nodes", bfsReachable.size()),
                bfsTimeMs,
                dijkstraTimeMs);
    }
}
