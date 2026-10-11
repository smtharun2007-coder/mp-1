package com.disasterrelief.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only allocation plan preview generated before any state changes are committed.
 */
public class AllocationPreview {
    private String requestId;
    private String requestType;
    private int targetRegionId;
    private String targetRegionName;
    private boolean reachable;
    private boolean feasible;
    private String statusMessage;

    // Supply allocation plan details
    private List<DepotPlan> depotPlans = new ArrayList<>();
    private int medicineNeeded;
    private int medicineAllocated;
    private int foodNeeded;
    private int foodAllocated;
    private int outstandingMedicine;
    private int outstandingFood;

    // Rescue allocation plan details
    private String teamId;
    private String teamName;
    private int teamTravelTime;
    private List<Integer> teamRoute = new ArrayList<>();
    private String shelterId;
    private String shelterName;
    private int shelterTravelTime;
    private List<Integer> shelterRoute = new ArrayList<>();
    private int peopleToEvacuate;
    private int peopleEvacuated;
    private int peopleUnsheltered;

    public AllocationPreview() {}

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public int getTargetRegionId() { return targetRegionId; }
    public void setTargetRegionId(int targetRegionId) { this.targetRegionId = targetRegionId; }

    public String getTargetRegionName() { return targetRegionName; }
    public void setTargetRegionName(String targetRegionName) { this.targetRegionName = targetRegionName; }

    public boolean isReachable() { return reachable; }
    public void setReachable(boolean reachable) { this.reachable = reachable; }

    public boolean isFeasible() { return feasible; }
    public void setFeasible(boolean feasible) { this.feasible = feasible; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    public List<DepotPlan> getDepotPlans() { return depotPlans; }
    public void setDepotPlans(List<DepotPlan> depotPlans) { this.depotPlans = depotPlans != null ? depotPlans : new ArrayList<>(); }

    public int getMedicineNeeded() { return medicineNeeded; }
    public void setMedicineNeeded(int medicineNeeded) { this.medicineNeeded = medicineNeeded; }

    public int getMedicineAllocated() { return medicineAllocated; }
    public void setMedicineAllocated(int medicineAllocated) { this.medicineAllocated = medicineAllocated; }

    public int getFoodNeeded() { return foodNeeded; }
    public void setFoodNeeded(int foodNeeded) { this.foodNeeded = foodNeeded; }

    public int getFoodAllocated() { return foodAllocated; }
    public void setFoodAllocated(int foodAllocated) { this.foodAllocated = foodAllocated; }

    public int getOutstandingMedicine() { return outstandingMedicine; }
    public void setOutstandingMedicine(int outstandingMedicine) { this.outstandingMedicine = outstandingMedicine; }

    public int getOutstandingFood() { return outstandingFood; }
    public void setOutstandingFood(int outstandingFood) { this.outstandingFood = outstandingFood; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public int getTeamTravelTime() { return teamTravelTime; }
    public void setTeamTravelTime(int teamTravelTime) { this.teamTravelTime = teamTravelTime; }

    public List<Integer> getTeamRoute() { return teamRoute; }
    public void setTeamRoute(List<Integer> teamRoute) { this.teamRoute = teamRoute != null ? teamRoute : new ArrayList<>(); }

    public String getShelterId() { return shelterId; }
    public void setShelterId(String shelterId) { this.shelterId = shelterId; }

    public String getShelterName() { return shelterName; }
    public void setShelterName(String shelterName) { this.shelterName = shelterName; }

    public int getShelterTravelTime() { return shelterTravelTime; }
    public void setShelterTravelTime(int shelterTravelTime) { this.shelterTravelTime = shelterTravelTime; }

    public List<Integer> getShelterRoute() { return shelterRoute; }
    public void setShelterRoute(List<Integer> shelterRoute) { this.shelterRoute = shelterRoute != null ? shelterRoute : new ArrayList<>(); }

    public int getPeopleToEvacuate() { return peopleToEvacuate; }
    public void setPeopleToEvacuate(int peopleToEvacuate) { this.peopleToEvacuate = peopleToEvacuate; }

    public int getPeopleEvacuated() { return peopleEvacuated; }
    public void setPeopleEvacuated(int peopleEvacuated) { this.peopleEvacuated = peopleEvacuated; }

    public int getPeopleUnsheltered() { return peopleUnsheltered; }
    public void setPeopleUnsheltered(int peopleUnsheltered) { this.peopleUnsheltered = peopleUnsheltered; }

    public static class DepotPlan {
        private String depotId;
        private String depotName;
        private int depotNode;
        private int medicineDrawn;
        private int foodDrawn;
        private int travelTime;
        private List<Integer> route;

        public DepotPlan() {}

        public DepotPlan(String depotId, String depotName, int depotNode,
                         int medicineDrawn, int foodDrawn, int travelTime, List<Integer> route) {
            this.depotId = depotId;
            this.depotName = depotName;
            this.depotNode = depotNode;
            this.medicineDrawn = medicineDrawn;
            this.foodDrawn = foodDrawn;
            this.travelTime = travelTime;
            this.route = route;
        }

        public String getDepotId() { return depotId; }
        public String getDepotName() { return depotName; }
        public int getDepotNode() { return depotNode; }
        public int getMedicineDrawn() { return medicineDrawn; }
        public int getFoodDrawn() { return foodDrawn; }
        public int getTravelTime() { return travelTime; }
        public List<Integer> getRoute() { return route; }
    }
}
