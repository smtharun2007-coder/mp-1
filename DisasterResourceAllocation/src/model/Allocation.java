package model;

public class Allocation {

    private String allocationId;
    private String requestId;
    private Depot depot;
    private int foodAllocated;
    private int medicineAllocated;
    private RescueTeam rescueTeam;  // Can be null if not assigned
    private Shelter shelter;        // Can be null if not assigned
    private String route;
    private int travelTime;
    private String status;

    public Allocation(String allocationId, String requestId, Depot depot,
                      int foodAllocated, int medicineAllocated,
                      RescueTeam rescueTeam, Shelter shelter,
                      String route, int travelTime, String status) {
        this.allocationId = allocationId;
        this.requestId = requestId;
        this.depot = depot;
        this.foodAllocated = foodAllocated;
        this.medicineAllocated = medicineAllocated;
        this.rescueTeam = rescueTeam;
        this.shelter = shelter;
        this.route = route;
        this.travelTime = travelTime;
        this.status = status;
    }

    public String getAllocationId()     { return allocationId; }
    public String getRequestId()        { return requestId; }
    public Depot getDepot()             { return depot; }
    public int getFoodAllocated()       { return foodAllocated; }
    public int getMedicineAllocated()   { return medicineAllocated; }
    public RescueTeam getRescueTeam()   { return rescueTeam; }
    public Shelter getShelter()         { return shelter; }
    public String getRoute()            { return route; }
    public int getTravelTime()          { return travelTime; }
    public String getStatus()           { return status; }

    @Override
    public String toString() {
        String teamInfo    = (rescueTeam != null) ? rescueTeam.getTeamName() : "None";
        String shelterInfo = (shelter != null)    ? shelter.getShelterName() : "None";
        return "[" + allocationId + "] Request: " + requestId
                + " | Depot: " + depot.getDepotName()
                + " | Food: " + foodAllocated + " | Medicine: " + medicineAllocated
                + " | Team: " + teamInfo + " | Shelter: " + shelterInfo
                + " | Route: " + route + " | Travel: " + travelTime + " min"
                + " | Status: " + status;
    }
}
