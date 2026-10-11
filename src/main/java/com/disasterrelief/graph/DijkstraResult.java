package com.disasterrelief.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of Dijkstra's single-pair shortest path search.
 */
public class DijkstraResult {
    private final int distance;
    private final List<Integer> path;
    private final boolean reachable;

    public DijkstraResult(int distance, List<Integer> path, boolean reachable) {
        this.distance = distance;
        this.path = path != null ? Collections.unmodifiableList(new ArrayList<>(path)) : Collections.emptyList();
        this.reachable = reachable;
    }

    public static DijkstraResult unreachable() {
        return new DijkstraResult(-1, Collections.emptyList(), false);
    }

    public int getDistance() {
        return distance;
    }

    public List<Integer> getPath() {
        return path;
    }

    public boolean isReachable() {
        return reachable;
    }

    @Override
    public String toString() {
        return "DijkstraResult{distance=" + distance + ", path=" + path + ", reachable=" + reachable + '}';
    }
}
