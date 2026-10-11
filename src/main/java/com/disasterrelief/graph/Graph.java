package com.disasterrelief.graph;

import com.disasterrelief.model.Road;

import java.util.*;

/**
 * In-memory undirected weighted graph representation using an adjacency list.
 * Roads marked CLOSED are excluded during graph construction and pathfinding.
 *
 * Algorithmic Complexity:
 * - HashMap lookup & vertex addition: O(1) average
 * - BFS / DFS traversal: O(V + E)
 * - Dijkstra shortest path (PriorityQueue min-heap): O((V + E) log V)
 */
public class Graph {
    // Adjacency list: node ID -> list of outgoing edges
    private final Map<Integer, List<Edge>> adjList = new HashMap<>();

    public Graph() {}

    /**
     * Builds or rebuilds the graph from a collection of roads.
     * Skips roads where status is not OPEN.
     */
    public synchronized void buildFromRoads(Collection<Road> roads) {
        adjList.clear();
        if (roads == null) return;

        for (Road road : roads) {
            // Guarantee vertices exist in adjacency map
            adjList.computeIfAbsent(road.getFrom(), k -> new ArrayList<>());
            adjList.computeIfAbsent(road.getTo(), k -> new ArrayList<>());

            if (road.isOpen()) {
                // Undirected edge: traverse in both directions
                adjList.get(road.getFrom()).add(new Edge(road.getTo(), road.getTravelTime(), road.getId(), true));
                adjList.get(road.getTo()).add(new Edge(road.getFrom(), road.getTravelTime(), road.getId(), true));
            }
        }
    }

    /**
     * Ensures an isolated node exists in the graph structure even if it has no roads yet.
     */
    public synchronized void addNode(int nodeId) {
        adjList.computeIfAbsent(nodeId, k -> new ArrayList<>());
    }

    /**
     * Breadth-First Search (BFS) reachability traversal using an ArrayDeque queue.
     * Complexity: O(V + E) time, O(V) space.
     *
     * @param startNode starting node ID
     * @return Set of all node IDs reachable from the startNode
     */
    public Set<Integer> bfsReachableFrom(int startNode) {
        Set<Integer> visited = new HashSet<>();
        if (!adjList.containsKey(startNode)) {
            return visited;
        }

        Queue<Integer> queue = new ArrayDeque<>();
        visited.add(startNode);
        queue.add(startNode);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            List<Edge> neighbors = adjList.getOrDefault(current, Collections.emptyList());
            for (Edge edge : neighbors) {
                if (edge.isOpen() && !visited.contains(edge.getTo())) {
                    visited.add(edge.getTo());
                    queue.add(edge.getTo());
                }
            }
        }
        return visited;
    }

    /**
     * Depth-First Search (DFS) reachability traversal using an explicit Stack.
     * Complexity: O(V + E) time, O(V) space.
     *
     * @param startNode starting node ID
     * @return Set of all node IDs reachable from the startNode
     */
    public Set<Integer> dfsReachableFrom(int startNode) {
        Set<Integer> visited = new HashSet<>();
        if (!adjList.containsKey(startNode)) {
            return visited;
        }

        Deque<Integer> stack = new ArrayDeque<>();
        visited.add(startNode);
        stack.push(startNode);

        while (!stack.isEmpty()) {
            int current = stack.pop();
            List<Edge> neighbors = adjList.getOrDefault(current, Collections.emptyList());
            for (Edge edge : neighbors) {
                if (edge.isOpen() && !visited.contains(edge.getTo())) {
                    visited.add(edge.getTo());
                    stack.push(edge.getTo());
                }
            }
        }
        return visited;
    }

    /**
     * Canonical reachability check (uses BFS).
     */
    public Set<Integer> reachableFrom(int node) {
        return bfsReachableFrom(node);
    }

    /**
     * Dijkstra's Algorithm for Single-Source Shortest Path using a PriorityQueue min-heap.
     * Edge weights are strictly positive travel times in minutes.
     * Complexity: O((V + E) log V).
     *
     * @param source starting node ID
     * @param target destination node ID
     * @return DijkstraResult containing travel time, path, and reachability flag
     */
    public DijkstraResult findShortestPath(int source, int target) {
        if (!adjList.containsKey(source) || !adjList.containsKey(target)) {
            return DijkstraResult.unreachable();
        }

        if (source == target) {
            return new DijkstraResult(0, List.of(source), true);
        }

        Map<Integer, Integer> dist = new HashMap<>();
        Map<Integer, Integer> prev = new HashMap<>();

        // PriorityQueue min-heap ordered by accumulated distance: PriorityQueue insert/poll is O(log V)
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingInt(nd -> nd.distance));

        dist.put(source, 0);
        pq.add(new NodeDistance(source, 0));

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            int u = current.node;

            if (current.distance > dist.getOrDefault(u, Integer.MAX_VALUE)) {
                continue; // Stale distance entry
            }

            if (u == target) {
                // Target reached with optimal distance
                break;
            }

            List<Edge> edges = adjList.getOrDefault(u, Collections.emptyList());
            for (Edge edge : edges) {
                if (!edge.isOpen()) continue;

                int v = edge.getTo();
                int newDist = current.distance + edge.getTravelTime();

                if (newDist < dist.getOrDefault(v, Integer.MAX_VALUE)) {
                    dist.put(v, newDist);
                    prev.put(v, u);
                    pq.add(new NodeDistance(v, newDist));
                }
            }
        }

        if (!dist.containsKey(target)) {
            return DijkstraResult.unreachable();
        }

        // Reconstruct path backwards from target to source
        List<Integer> path = new ArrayList<>();
        Integer curr = target;
        while (curr != null) {
            path.add(curr);
            curr = prev.get(curr);
        }
        Collections.reverse(path);

        return new DijkstraResult(dist.get(target), path, true);
    }

    /**
     * Finds isolated regions: regions that cannot be reached from any of the given depot nodes.
     *
     * @param regionNodeIds Set of all region node IDs
     * @param depotNodeIds  Collection of all depot node IDs
     * @return List of isolated region node IDs
     */
    public List<Integer> findIsolatedRegions(Collection<Integer> regionNodeIds, Collection<Integer> depotNodeIds) {
        Set<Integer> reachableFromDepots = new HashSet<>();
        for (int depotNode : depotNodeIds) {
            reachableFromDepots.addAll(reachableFrom(depotNode));
        }

        List<Integer> isolated = new ArrayList<>();
        for (int regionId : regionNodeIds) {
            if (!reachableFromDepots.contains(regionId)) {
                isolated.add(regionId);
            }
        }
        Collections.sort(isolated);
        return isolated;
    }

    public Map<Integer, List<Edge>> getAdjList() {
        return Collections.unmodifiableMap(adjList);
    }

    /**
     * Helper node-distance wrapper for Dijkstra PriorityQueue.
     */
    private static class NodeDistance {
        final int node;
        final int distance;

        NodeDistance(int node, int distance) {
            this.node = node;
            this.distance = distance;
        }
    }
}
