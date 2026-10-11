package com.disasterrelief.model;

/**
 * Represents a supply warehouse node storing medicine and food rations.
 * Quantities are strictly encapsulated and modified only via domain methods.
 */
public class Depot {
    private String id;
    private String name;
    private int node; // Graph node (11 or 12)
    private int medicineKits;
    private int foodPacks;
    private int reservedMedicine;
    private int reservedFood;

    private String locationCode; // e.g. "LOC01" or "LOC02"

    public Depot() {}

    public Depot(String id, String name, int node, int medicineKits, int foodPacks) {
        this(id, name, "LOC" + (node < 10 ? "0" + node : String.valueOf(node)), node, medicineKits, foodPacks);
    }

    public Depot(String id, String name, String locationCode, int node, int medicineKits, int foodPacks) {
        this.id = id;
        this.name = name;
        this.locationCode = locationCode;
        this.node = node;
        this.medicineKits = Math.max(0, medicineKits);
        this.foodPacks = Math.max(0, foodPacks);
        this.reservedMedicine = 0;
        this.reservedFood = 0;
    }

    public String getLocationCode() {
        return locationCode != null ? locationCode : ("LOC" + (node < 10 ? "0" + node : String.valueOf(node)));
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public synchronized ResourceStock getStock() {
        ResourceStock rs = new ResourceStock(this.foodPacks, this.medicineKits);
        rs.hydrate(this.foodPacks, this.medicineKits, this.reservedFood, this.reservedMedicine);
        return rs;
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

    public int getNode() {
        return node;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public void hydrate(int medicineKits, int foodPacks, int reservedMedicine, int reservedFood) {
        this.medicineKits = Math.max(0, medicineKits);
        this.foodPacks = Math.max(0, foodPacks);
        this.reservedMedicine = Math.max(0, reservedMedicine);
        this.reservedFood = Math.max(0, reservedFood);
    }

    public int getMedicineKits() {
        return medicineKits;
    }

    public int getFoodPacks() {
        return foodPacks;
    }

    public int getReservedMedicine() {
        return reservedMedicine;
    }

    public int getReservedFood() {
        return reservedFood;
    }

    public int getAvailableMedicine() {
        return medicineKits;
    }

    public int getAvailableFood() {
        return foodPacks;
    }

    /**
     * Reserves stock atomically for an allocation preview/commit.
     */
    public synchronized boolean reserveStock(int medicine, int food) {
        if (medicine <= medicineKits && food <= foodPacks) {
            this.medicineKits -= medicine;
            this.reservedMedicine += medicine;
            this.foodPacks -= food;
            this.reservedFood += food;
            return true;
        }
        return false;
    }

    /**
     * Releases previously reserved stock back to available stock.
     */
    public synchronized void releaseStock(int medicine, int food) {
        int medToRelease = Math.min(medicine, this.reservedMedicine);
        int foodToRelease = Math.min(food, this.reservedFood);
        this.reservedMedicine -= medToRelease;
        this.medicineKits += medToRelease;
        this.reservedFood -= foodToRelease;
        this.foodPacks += foodToRelease;
    }

    /**
     * Consumes reserved stock upon completed delivery.
     */
    public synchronized void consumeReservedStock(int medicine, int food) {
        this.reservedMedicine = Math.max(0, this.reservedMedicine - medicine);
        this.reservedFood = Math.max(0, this.reservedFood - food);
    }

    /**
     * Directly adjusts stock (e.g. for warehouse restocking or audits).
     */
    public synchronized void adjustStock(int medicineDelta, int foodDelta) {
        this.medicineKits = Math.max(0, this.medicineKits + medicineDelta);
        this.foodPacks = Math.max(0, this.foodPacks + foodDelta);
    }

    @Override
    public String toString() {
        return "Depot{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", node=" + node +
                ", medicineKits=" + medicineKits +
                ", foodPacks=" + foodPacks +
                ", reservedMedicine=" + reservedMedicine +
                ", reservedFood=" + reservedFood +
                '}';
    }
}
