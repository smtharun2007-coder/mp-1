package com.disasterrelief.model;

/**
 * Rescue operation request requiring a search and rescue team and emergency shelter places.
 * Extends ReliefRequest to fulfill domain inheritance and polymorphism.
 */
public class RescueRequest extends ReliefRequest {
    private int peopleToEvacuate;
    private int peopleEvacuated;
    private int rescueCount;
    private int shelterCount;
    private String assignedTeamId;
    private String assignedShelterId;

    // Transient contextual reachability indicators evaluated dynamically during preview
    private boolean reachable = true;
    private boolean availableTeamCanReach = true;
    private boolean shelterPlacesAvailable = true;

    public RescueRequest() {
        super();
        setType("RESCUE");
    }

    public RescueRequest(String id, String reportId, int regionId, int severity, int affectedPopulation,
                         long arrivalNo, RequestStatus status, String createdBy,
                         UrgentNeed mostUrgentNeed, int peopleToEvacuate) {
        super(id, "LOC" + (regionId < 10 ? "0" + regionId : String.valueOf(regionId)), reportId, regionId, severity, affectedPopulation, arrivalNo, status, createdBy, "RESCUE", mostUrgentNeed);
        this.peopleToEvacuate = Math.max(0, peopleToEvacuate);
        this.rescueCount = this.peopleToEvacuate;
        this.peopleEvacuated = 0;
        setRescueRequired(true);
        setShelterPlacesRequested(this.peopleToEvacuate);
    }

    public RescueRequest(String id, String locationCode, int regionId, int severity, int affectedPopulation,
                         long arrivalNo, RequestStatus status, int foodNeeded, int medicineNeeded,
                         boolean rescueRequired, int shelterPlacesRequested) {
        super(id, locationCode, regionId, severity, affectedPopulation, arrivalNo, status, "operator", "RESCUE", UrgentNeed.RESCUE);
        setFoodNeeded(foodNeeded);
        setMedicineNeeded(medicineNeeded);
        setRescueRequired(rescueRequired);
        setShelterPlacesRequested(shelterPlacesRequested);
        this.peopleToEvacuate = shelterPlacesRequested > 0 ? shelterPlacesRequested : affectedPopulation;
        this.rescueCount = this.peopleToEvacuate;
        this.shelterCount = shelterPlacesRequested;
        this.peopleEvacuated = 0;
    }

    public int getRescueCount() {
        return rescueCount > 0 ? rescueCount : peopleToEvacuate;
    }

    public void setRescueCount(int rescueCount) {
        this.rescueCount = rescueCount;
    }

    public int getShelterCount() {
        return shelterCount;
    }

    public void setShelterCount(int shelterCount) {
        this.shelterCount = shelterCount;
    }

    @Override
    public boolean isFulfillable() {
        return reachable && availableTeamCanReach && shelterPlacesAvailable;
    }

    public int getPeopleToEvacuate() {
        return peopleToEvacuate;
    }

    public void setPeopleToEvacuate(int peopleToEvacuate) {
        this.peopleToEvacuate = Math.max(0, peopleToEvacuate);
    }

    public int getPeopleEvacuated() {
        return peopleEvacuated;
    }

    public void setPeopleEvacuated(int peopleEvacuated) {
        this.peopleEvacuated = Math.max(0, peopleEvacuated);
        setShelterPlacesAllocated(peopleEvacuated);
    }

    public String getAssignedTeamId() {
        return assignedTeamId;
    }

    public void setAssignedTeamId(String assignedTeamId) {
        this.assignedTeamId = assignedTeamId;
    }

    public String getAssignedShelterId() {
        return assignedShelterId;
    }

    public void setAssignedShelterId(String assignedShelterId) {
        this.assignedShelterId = assignedShelterId;
    }

    public boolean isReachable() {
        return reachable;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public boolean isAvailableTeamCanReach() {
        return availableTeamCanReach;
    }

    public void setAvailableTeamCanReach(boolean availableTeamCanReach) {
        this.availableTeamCanReach = availableTeamCanReach;
    }

    public boolean isShelterPlacesAvailable() {
        return shelterPlacesAvailable;
    }

    public void setShelterPlacesAvailable(boolean shelterPlacesAvailable) {
        this.shelterPlacesAvailable = shelterPlacesAvailable;
    }

    @Override
    public String toString() {
        return "RescueRequest{" +
                "id='" + getId() + '\'' +
                ", location='" + getLocationCode() + '\'' +
                ", region=" + getRegionId() +
                ", severity=" + getSeverity() +
                ", population=" + getAffectedPopulation() +
                ", arrivalNo=" + getArrivalNo() +
                ", status=" + getStatus() +
                ", peopleToEvacuate=" + peopleToEvacuate +
                ", peopleEvacuated=" + peopleEvacuated +
                ", foodNeeded=" + getFoodNeeded() +
                ", medicineNeeded=" + getMedicineNeeded() +
                '}';
    }
}
