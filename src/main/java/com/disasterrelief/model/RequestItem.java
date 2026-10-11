package com.disasterrelief.model;

/**
 * An itemized relief demand within a request, processed in the order established by needOrder.
 */
public class RequestItem {
    private String need; // "RESCUE", "SHELTER", "MEDICINE", "FOOD"
    private int quantityRequested;
    private int quantityAllocated;
    private int quantityOutstanding;

    public RequestItem() {}

    public RequestItem(String need, int quantityRequested) {
        this.need = need;
        this.quantityRequested = quantityRequested;
        this.quantityAllocated = 0;
        this.quantityOutstanding = quantityRequested;
    }

    public RequestItem(String need, int quantityRequested, int quantityAllocated, int quantityOutstanding) {
        this.need = need;
        this.quantityRequested = quantityRequested;
        this.quantityAllocated = quantityAllocated;
        this.quantityOutstanding = quantityOutstanding;
    }

    public String getNeed() {
        return need;
    }

    public void setNeed(String need) {
        this.need = need;
    }

    public int getQuantityRequested() {
        return quantityRequested;
    }

    public void setQuantityRequested(int quantityRequested) {
        this.quantityRequested = quantityRequested;
    }

    public int getQuantityAllocated() {
        return quantityAllocated;
    }

    public void setQuantityAllocated(int quantityAllocated) {
        this.quantityAllocated = quantityAllocated;
    }

    public int getQuantityOutstanding() {
        return quantityOutstanding;
    }

    public void setQuantityOutstanding(int quantityOutstanding) {
        this.quantityOutstanding = quantityOutstanding;
    }
}
