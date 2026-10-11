package com.disasterrelief.model;

/**
 * Represents a rapid disaster search and rescue team.
 * Status is encapsulated and managed via reserveTeam() and releaseTeam().
 */
public class RescueTeam {
    public enum TeamStatus {
        AVAILABLE,
        BUSY
    }

    private String id;
    private String name;
    private int baseNode;
    private TeamStatus status;
    private String locationCode;

    public RescueTeam() {}

    public RescueTeam(String id, String name, int baseNode, TeamStatus status) {
        this(id, name, "LOC" + (baseNode < 10 ? "0" + baseNode : String.valueOf(baseNode)), baseNode, status);
    }

    public RescueTeam(String id, String name, String locationCode, int baseNode, TeamStatus status) {
        this.id = id;
        this.name = name;
        this.locationCode = locationCode;
        this.baseNode = baseNode;
        this.status = status != null ? status : TeamStatus.AVAILABLE;
    }

    public String getLocationCode() {
        return locationCode != null ? locationCode : ("LOC" + (baseNode < 10 ? "0" + baseNode : String.valueOf(baseNode)));
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBaseNode() {
        return baseNode;
    }

    public void setBaseNode(int baseNode) {
        this.baseNode = baseNode;
    }

    public TeamStatus getStatus() {
        return status;
    }

    public void setStatus(TeamStatus status) {
        this.status = status;
    }

    public boolean isAvailable() {
        return status == TeamStatus.AVAILABLE;
    }

    /**
     * Reserves team if currently available.
     */
    public synchronized boolean reserveTeam() {
        if (this.status == TeamStatus.AVAILABLE) {
            this.status = TeamStatus.BUSY;
            return true;
        }
        return false;
    }

    /**
     * Releases team back to available status upon rescue completion.
     */
    public synchronized void releaseTeam() {
        this.status = TeamStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        return "RescueTeam{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", baseNode=" + baseNode +
                ", status=" + status +
                '}';
    }
}
