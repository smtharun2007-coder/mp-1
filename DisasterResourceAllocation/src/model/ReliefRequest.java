package model;

public class ReliefRequest {

    // Enum representing the lifecycle of a relief request
    public enum RequestStatus {
        NEW, WAITING, SELECTED, ALLOCATED, PARTIALLY_ALLOCATED
    }

    private String requestId;
    private Location location;
    private int severity;           // Valid range: 1 to 5
    private int affectedPopulation;
    private int foodRequired;
    private int medicineRequired;
    private boolean rescueTeamRequired;
    private boolean shelterRequired;
    private int arrivalOrder;       // Used for tie-breaking in priority queue
    private RequestStatus status;

    public ReliefRequest(String requestId, Location location, int severity, int affectedPopulation,
                         int foodRequired, int medicineRequired,
                         boolean rescueTeamRequired, boolean shelterRequired, int arrivalOrder) {

        this.requestId = requestId;
        this.location = location;
        this.arrivalOrder = arrivalOrder;
        this.status = RequestStatus.NEW;

        // Validate severity (1–5)
        if (severity < 1 || severity > 5) {
            System.out.println("  Warning: Invalid severity for " + requestId + ". Defaulting to 1.");
            this.severity = 1;
        } else {
            this.severity = severity;
        }

        // Validate non-negative values
        this.affectedPopulation = Math.max(0, affectedPopulation);
        this.foodRequired       = Math.max(0, foodRequired);
        this.medicineRequired   = Math.max(0, medicineRequired);

        this.rescueTeamRequired = rescueTeamRequired;
        this.shelterRequired    = shelterRequired;
    }

    // --- Getters ---
    public String getRequestId()            { return requestId; }
    public Location getLocation()           { return location; }
    public int getSeverity()                { return severity; }
    public int getAffectedPopulation()      { return affectedPopulation; }
    public int getFoodRequired()            { return foodRequired; }
    public int getMedicineRequired()        { return medicineRequired; }
    public boolean isRescueTeamRequired()   { return rescueTeamRequired; }
    public boolean isShelterRequired()      { return shelterRequired; }
    public int getArrivalOrder()            { return arrivalOrder; }
    public RequestStatus getStatus()        { return status; }

    // --- Setters ---
    public void setStatus(RequestStatus status)             { this.status = status; }
    public void setSeverity(int severity) {
        if (severity >= 1 && severity <= 5) this.severity = severity;
    }
    public void setAffectedPopulation(int pop)              { if (pop >= 0) this.affectedPopulation = pop; }
    public void setFoodRequired(int food)                   { if (food >= 0) this.foodRequired = food; }
    public void setMedicineRequired(int medicine)           { if (medicine >= 0) this.medicineRequired = medicine; }
    public void setRescueTeamRequired(boolean required)     { this.rescueTeamRequired = required; }
    public void setShelterRequired(boolean required)        { this.shelterRequired = required; }

    @Override
    public String toString() {
        return "[" + requestId + "] Location: " + location.getLocationName()
                + " | Severity: " + severity
                + " | Population: " + affectedPopulation
                + " | Food: " + foodRequired + " | Medicine: " + medicineRequired
                + " | Rescue: " + rescueTeamRequired + " | Shelter: " + shelterRequired
                + " | Status: " + status;
    }
}
