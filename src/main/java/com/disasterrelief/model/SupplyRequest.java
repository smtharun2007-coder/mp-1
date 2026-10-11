package com.disasterrelief.model;

/**
 * Supply delivery request requiring medicine kits and food packs.
 * Extends ReliefRequest to fulfill domain inheritance and polymorphism.
 * Shortfalls are tracked as outstanding quantities.
 */
public class SupplyRequest extends ReliefRequest {

    // Transient reachability & stock indicator evaluated dynamically
    private boolean reachable = true;
    private int totalAvailableStock = 1;

    public SupplyRequest() {
        super();
        setType("SUPPLY");
    }

    public SupplyRequest(String id, String reportId, int regionId, int severity, int affectedPopulation,
                         long arrivalNo, RequestStatus status, String createdBy,
                         UrgentNeed mostUrgentNeed, int medicineNeeded, int foodNeeded) {
        super(id, "LOC" + (regionId < 10 ? "0" + regionId : String.valueOf(regionId)), reportId, regionId, severity, affectedPopulation, arrivalNo, status, createdBy, "SUPPLY", mostUrgentNeed);
        setMedicineNeeded(medicineNeeded);
        setFoodNeeded(foodNeeded);
        setMedicineAllocated(0);
        setFoodAllocated(0);
        setRescueRequired(false);
        setShelterPlacesRequested(0);
    }

    public SupplyRequest(String id, String locationCode, int regionId, int severity, int affectedPopulation,
                         long arrivalNo, RequestStatus status, int foodNeeded, int medicineNeeded) {
        super(id, locationCode, regionId, severity, affectedPopulation, arrivalNo, status, "operator", "SUPPLY", UrgentNeed.FOOD);
        setFoodNeeded(foodNeeded);
        setMedicineNeeded(medicineNeeded);
        setMedicineAllocated(0);
        setFoodAllocated(0);
        setRescueRequired(false);
        setShelterPlacesRequested(0);
    }

    @Override
    public boolean isFulfillable() {
        return reachable && (totalAvailableStock > 0);
    }

    public boolean isReachable() {
        return reachable;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public int getTotalAvailableStock() {
        return totalAvailableStock;
    }

    public void setTotalAvailableStock(int totalAvailableStock) {
        this.totalAvailableStock = totalAvailableStock;
    }

    @Override
    public String toString() {
        return "SupplyRequest{" +
                "id='" + getId() + '\'' +
                ", location='" + getLocationCode() + '\'' +
                ", region=" + getRegionId() +
                ", severity=" + getSeverity() +
                ", population=" + getAffectedPopulation() +
                ", arrivalNo=" + getArrivalNo() +
                ", foodNeeded=" + getFoodNeeded() +
                ", foodAllocated=" + getFoodAllocated() +
                ", medicineNeeded=" + getMedicineNeeded() +
                ", medicineAllocated=" + getMedicineAllocated() +
                ", status=" + getStatus() +
                '}';
    }
}
