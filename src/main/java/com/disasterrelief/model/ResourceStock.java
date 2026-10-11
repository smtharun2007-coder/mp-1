package com.disasterrelief.model;

/**
 * Domain entity modeling encapsulated resource inventory stock.
 * Tracks food kits and medicine kits independently with strict non-negative invariants.
 */
public class ResourceStock {
    private int foodKits;
    private int medicineKits;
    private int reservedFood;
    private int reservedMedicine;

    public ResourceStock() {
        this(0, 0);
    }

    public ResourceStock(int foodKits, int medicineKits) {
        this.foodKits = Math.max(0, foodKits);
        this.medicineKits = Math.max(0, medicineKits);
        this.reservedFood = 0;
        this.reservedMedicine = 0;
    }

    public synchronized int getFoodKits() {
        return foodKits;
    }

    public synchronized int getMedicineKits() {
        return medicineKits;
    }

    public synchronized int getReservedFood() {
        return reservedFood;
    }

    public synchronized int getReservedMedicine() {
        return reservedMedicine;
    }

    public synchronized int getAvailableFood() {
        return foodKits;
    }

    public synchronized int getAvailableMedicine() {
        return medicineKits;
    }

    /**
     * Atomically reserves food and medicine stock.
     * Prevents reducing stock below zero and prevents double-allocation.
     *
     * @param foodQty requested food kits
     * @param medQty  requested medicine kits
     * @return true if stock was successfully reserved, false if insufficient
     */
    public synchronized boolean reserveStock(int foodQty, int medQty) {
        if (foodQty < 0 || medQty < 0) return false;
        if (foodQty <= this.foodKits && medQty <= this.medicineKits) {
            this.foodKits -= foodQty;
            this.reservedFood += foodQty;
            this.medicineKits -= medQty;
            this.reservedMedicine += medQty;
            return true;
        }
        return false;
    }

    /**
     * Releases previously reserved stock back to available warehouse balance.
     */
    public synchronized void releaseStock(int foodQty, int medQty) {
        int foodToRelease = Math.min(Math.max(0, foodQty), this.reservedFood);
        int medToRelease = Math.min(Math.max(0, medQty), this.reservedMedicine);
        this.reservedFood -= foodToRelease;
        this.foodKits += foodToRelease;
        this.reservedMedicine -= medToRelease;
        this.medicineKits += medToRelease;
    }

    /**
     * Consumes reserved stock upon completed delivery.
     */
    public synchronized void consumeReservedStock(int foodQty, int medQty) {
        this.reservedFood = Math.max(0, this.reservedFood - Math.max(0, foodQty));
        this.reservedMedicine = Math.max(0, this.reservedMedicine - Math.max(0, medQty));
    }

    /**
     * Directly adjusts stock (e.g. for restock or inventory audit adjustments).
     * Prevents negative inventory levels.
     */
    public synchronized void adjustStock(int foodDelta, int medDelta) {
        this.foodKits = Math.max(0, this.foodKits + foodDelta);
        this.medicineKits = Math.max(0, this.medicineKits + medDelta);
    }

    public synchronized void hydrate(int food, int med, int resFood, int resMed) {
        this.foodKits = Math.max(0, food);
        this.medicineKits = Math.max(0, med);
        this.reservedFood = Math.max(0, resFood);
        this.reservedMedicine = Math.max(0, resMed);
    }

    @Override
    public synchronized String toString() {
        return "ResourceStock{" +
                "foodAvailable=" + foodKits +
                ", medicineAvailable=" + medicineKits +
                ", foodReserved=" + reservedFood +
                ", medicineReserved=" + reservedMedicine +
                '}';
    }
}
