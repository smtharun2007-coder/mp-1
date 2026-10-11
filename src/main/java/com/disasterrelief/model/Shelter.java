package com.disasterrelief.model;

/**
 * Represents an evacuation shelter with total capacity and reserved slots.
 * Occupancy modifications are strictly encapsulated.
 */
public class Shelter {
    private String id;
    private String name;
    private int node;
    private int capacity;
    private int reserved;
    private String locationCode;

    public Shelter() {}

    public Shelter(String id, String name, int node, int capacity) {
        this(id, name, "LOC" + (node < 10 ? "0" + node : String.valueOf(node)), node, capacity);
    }

    public Shelter(String id, String name, String locationCode, int node, int capacity) {
        this.id = id;
        this.name = name;
        this.locationCode = locationCode;
        this.node = node;
        this.capacity = Math.max(0, capacity);
        this.reserved = 0;
    }

    public String getLocationCode() {
        return locationCode != null ? locationCode : ("LOC" + (node < 10 ? "0" + node : String.valueOf(node)));
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
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

    public void hydrate(int capacity, int reserved) {
        this.capacity = Math.max(0, capacity);
        this.reserved = Math.max(0, reserved);
    }

    public int getCapacity() {
        return capacity;
    }

    public int getReserved() {
        return reserved;
    }

    public int getAvailableCapacity() {
        return Math.max(0, capacity - reserved);
    }

    /**
     * Reserves places up to available capacity.
     */
    public synchronized boolean reservePlaces(int qty) {
        if (qty > 0 && (capacity - reserved) >= qty) {
            this.reserved += qty;
            return true;
        }
        return false;
    }

    /**
     * Releases reserved places (e.g. if evacuated persons relocate or allocation cancelled).
     */
    public synchronized void releasePlaces(int qty) {
        this.reserved = Math.max(0, this.reserved - qty);
    }

    @Override
    public String toString() {
        return "Shelter{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", node=" + node +
                ", capacity=" + capacity +
                ", reserved=" + reserved +
                ", available=" + getAvailableCapacity() +
                '}';
    }
}
