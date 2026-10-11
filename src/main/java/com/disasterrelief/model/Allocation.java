package com.disasterrelief.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Record of committed or dispatched disaster relief allocation.
 */
public class Allocation {
    public enum AllocationStatus {
        PREVIEW,
        COMMITTED,
        DISPATCHED,
        DELIVERED,
        CANCELLED
    }

    private String id;
    private String requestId;
    private String reportId;
    private int targetRegionId;
    private String type; // RESCUE or SUPPLY
    private String depotId;
    private int depotNode;
    private int medicineAllocated;
    private int foodAllocated;
    private String teamId;
    private String shelterId;
    private int shelterNode;
    private int peopleEvacuated;
    private List<Integer> route = new ArrayList<>();
    private int travelTime; // in minutes
    private AllocationStatus status;
    private Instant createdAt;
    private Instant dispatchedAt;
    private Instant deliveredAt;
    private String notes;

    public Allocation() {
        this.createdAt = Instant.now();
        this.status = AllocationStatus.COMMITTED;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public int getTargetRegionId() {
        return targetRegionId;
    }

    public void setTargetRegionId(int targetRegionId) {
        this.targetRegionId = targetRegionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDepotId() {
        return depotId;
    }

    public void setDepotId(String depotId) {
        this.depotId = depotId;
    }

    public int getDepotNode() {
        return depotNode;
    }

    public void setDepotNode(int depotNode) {
        this.depotNode = depotNode;
    }

    public int getMedicineAllocated() {
        return medicineAllocated;
    }

    public void setMedicineAllocated(int medicineAllocated) {
        this.medicineAllocated = medicineAllocated;
    }

    public int getFoodAllocated() {
        return foodAllocated;
    }

    public void setFoodAllocated(int foodAllocated) {
        this.foodAllocated = foodAllocated;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getShelterId() {
        return shelterId;
    }

    public void setShelterId(String shelterId) {
        this.shelterId = shelterId;
    }

    public int getShelterNode() {
        return shelterNode;
    }

    public void setShelterNode(int shelterNode) {
        this.shelterNode = shelterNode;
    }

    public int getPeopleEvacuated() {
        return peopleEvacuated;
    }

    public void setPeopleEvacuated(int peopleEvacuated) {
        this.peopleEvacuated = peopleEvacuated;
    }

    public List<Integer> getRoute() {
        return route;
    }

    public void setRoute(List<Integer> route) {
        this.route = route != null ? route : new ArrayList<>();
    }

    public int getTravelTime() {
        return travelTime;
    }

    public void setTravelTime(int travelTime) {
        this.travelTime = travelTime;
    }

    public AllocationStatus getStatus() {
        return status;
    }

    public void setStatus(AllocationStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(Instant dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "Allocation{" +
                "id='" + id + '\'' +
                ", requestId='" + requestId + '\'' +
                ", type='" + type + '\'' +
                ", travelTime=" + travelTime +
                ", status=" + status +
                '}';
    }
}
