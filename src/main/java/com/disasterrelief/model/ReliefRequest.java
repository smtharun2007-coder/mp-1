package com.disasterrelief.model;

/**
 * Base abstract domain entity for disaster relief requests in District Aruvi.
 * Extended by SupplyRequest and RescueRequest to achieve polymorphic fulfillability checks.
 *
 * Prioritized according to deterministic triage hierarchy:
 * 1. Severity DESC: Critical (4) > High (3) > Medium (2) > Low (1)
 * 2. Affected Population DESC
 * 3. Arrival Sequence ASC (deterministic FIFO tie-breaker)
 */
public abstract class ReliefRequest extends Request {

    public enum Severity {
        CRITICAL(4, "Critical"),
        HIGH(3, "High"),
        MEDIUM(2, "Medium"),
        LOW(1, "Low");

        private final int level;
        private final String label;

        Severity(int level, String label) {
            this.level = level;
            this.label = label;
        }

        public int getLevel() {
            return level;
        }

        public String getLabel() {
            return label;
        }

        public static Severity fromString(String str) {
            if (str == null) return LOW;
            for (Severity s : values()) {
                if (s.name().equalsIgnoreCase(str) || s.label.equalsIgnoreCase(str)) {
                    return s;
                }
            }
            return LOW;
        }

        public static Severity fromLevel(int level) {
            for (Severity s : values()) {
                if (s.level == level) return s;
            }
            return level >= 4 ? CRITICAL : (level == 3 ? HIGH : (level == 2 ? MEDIUM : LOW));
        }
    }

    private String locationCode;
    private int foodNeeded;
    private int foodAllocated;
    private int medicineNeeded;
    private int medicineAllocated;
    private boolean rescueRequired;
    private int shelterPlacesRequested;
    private int shelterPlacesAllocated;

    public ReliefRequest() {
        super();
    }

    public ReliefRequest(String id, String locationCode, String reportId, int regionId, int severity, int affectedPopulation,
                         long arrivalNo, RequestStatus status, String createdBy, String type, UrgentNeed mostUrgentNeed) {
        super(id, reportId, regionId, severity, affectedPopulation, arrivalNo, status, createdBy, type, mostUrgentNeed);
        this.locationCode = locationCode != null ? locationCode : ("LOC" + (regionId < 10 ? "0" + regionId : String.valueOf(regionId)));
    }

    public ReliefRequest(String id, String locationCode, int regionId, int severity, int affectedPopulation,
                         long arrivalNo, RequestStatus status, String createdBy, String type, UrgentNeed mostUrgentNeed) {
        this(id, locationCode, null, regionId, severity, affectedPopulation, arrivalNo, status, createdBy, type, mostUrgentNeed);
    }

    /**
     * Polymorphic verification of request fulfillability against resources and road reachability.
     */
    @Override
    public abstract boolean isFulfillable();

    public String getLocationCode() {
        if (locationCode == null && getRegionId() > 0) {
            locationCode = "LOC" + (getRegionId() < 10 ? "0" + getRegionId() : String.valueOf(getRegionId()));
        }
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public Severity getSeverityEnum() {
        return Severity.fromLevel(getSeverity());
    }

    public void setSeverityEnum(Severity severity) {
        if (severity != null) {
            setSeverity(severity.getLevel());
            setSeverityLevel(severity.getLevel());
        }
    }

    public int getFoodNeeded() {
        return foodNeeded;
    }

    public void setFoodNeeded(int foodNeeded) {
        this.foodNeeded = Math.max(0, foodNeeded);
    }

    public int getFoodAllocated() {
        return foodAllocated;
    }

    public void setFoodAllocated(int foodAllocated) {
        this.foodAllocated = Math.max(0, foodAllocated);
    }

    public int getMedicineNeeded() {
        return medicineNeeded;
    }

    public void setMedicineNeeded(int medicineNeeded) {
        this.medicineNeeded = Math.max(0, medicineNeeded);
    }

    public int getMedicineAllocated() {
        return medicineAllocated;
    }

    public void setMedicineAllocated(int medicineAllocated) {
        this.medicineAllocated = Math.max(0, medicineAllocated);
    }

    public boolean isRescueRequired() {
        return rescueRequired;
    }

    public void setRescueRequired(boolean rescueRequired) {
        this.rescueRequired = rescueRequired;
    }

    public int getShelterPlacesRequested() {
        return shelterPlacesRequested;
    }

    public void setShelterPlacesRequested(int shelterPlacesRequested) {
        this.shelterPlacesRequested = Math.max(0, shelterPlacesRequested);
    }

    public int getShelterPlacesAllocated() {
        return shelterPlacesAllocated;
    }

    public void setShelterPlacesAllocated(int shelterPlacesAllocated) {
        this.shelterPlacesAllocated = Math.max(0, shelterPlacesAllocated);
    }

    public int getOutstandingFood() {
        return Math.max(0, foodNeeded - foodAllocated);
    }

    public int getOutstandingMedicine() {
        return Math.max(0, medicineNeeded - medicineAllocated);
    }

    public int getOutstandingShelterPlaces() {
        return Math.max(0, shelterPlacesRequested - shelterPlacesAllocated);
    }
}
