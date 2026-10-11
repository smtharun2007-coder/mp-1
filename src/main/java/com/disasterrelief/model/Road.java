package com.disasterrelief.model;

/**
 * Represents an undirected road connecting two graph nodes.
 * Contains travel time in minutes and status (OPEN / CLOSED).
 */
public class Road {
    public enum RoadStatus {
        OPEN,
        CLOSED
    }

    private String id;
    private int from;
    private int to;
    private int travelTime; // in minutes, positive
    private RoadStatus status;
    private String closureReason;

    public Road() {}

    public Road(String id, int from, int to, int travelTime, RoadStatus status, String closureReason) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.travelTime = Math.max(1, travelTime);
        this.status = status != null ? status : RoadStatus.OPEN;
        this.closureReason = closureReason != null ? closureReason : "";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public int getTravelTime() {
        return travelTime;
    }

    public void setTravelTime(int travelTime) {
        this.travelTime = Math.max(1, travelTime);
    }

    public String getFromCode() {
        return "LOC" + (from < 10 ? "0" + from : String.valueOf(from));
    }

    public String getToCode() {
        return "LOC" + (to < 10 ? "0" + to : String.valueOf(to));
    }

    public int getWeight() {
        return travelTime;
    }

    public RoadStatus getStatus() {
        return status;
    }

    public void setStatus(RoadStatus status) {
        this.status = status;
    }

    public boolean isOpen() {
        return status == RoadStatus.OPEN;
    }

    public String getClosureReason() {
        return closureReason;
    }

    public void setClosureReason(String closureReason) {
        this.closureReason = closureReason;
    }

    public String getReason() {
        return closureReason;
    }

    public void setReason(String reason) {
        this.closureReason = reason;
    }

    @Override
    public String toString() {
        return "Road{" +
                "id='" + id + '\'' +
                ", from=" + from +
                ", to=" + to +
                ", travelTime=" + travelTime +
                ", status=" + status +
                ", closureReason='" + closureReason + '\'' +
                '}';
    }
}
