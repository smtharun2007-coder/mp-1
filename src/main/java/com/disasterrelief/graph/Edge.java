package com.disasterrelief.graph;

/**
 * Represents a directed traversal edge in the adjacency list.
 */
public class Edge {
    private final int to;
    private final int travelTime;
    private final String roadId;
    private final boolean open;

    public Edge(int to, int travelTime, String roadId, boolean open) {
        this.to = to;
        this.travelTime = travelTime;
        this.roadId = roadId;
        this.open = open;
    }

    public int getTo() {
        return to;
    }

    public int getTravelTime() {
        return travelTime;
    }

    public String getRoadId() {
        return roadId;
    }

    public boolean isOpen() {
        return open;
    }

    @Override
    public String toString() {
        return "Edge{to=" + to + ", time=" + travelTime + ", roadId='" + roadId + "'}";
    }
}
