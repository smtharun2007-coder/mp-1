package com.disasterrelief.model;

/**
 * Domain entity representing an administrative geographical region affected by disaster.
 * Used for District Aruvi locations LOC01 to LOC10.
 */
public class AffectedRegion {
    private int id;                 // Node ID (1 to 10)
    private String locationCode;    // e.g. "LOC01" to "LOC10"
    private String name;            // Location name, e.g. "Aruvi Central"
    private int population;         // Total census population
    private boolean controlRoom;    // Flag indicating district control-room location

    public AffectedRegion() {}

    public AffectedRegion(int id, String locationCode, String name, int population, boolean controlRoom) {
        this.id = id;
        this.locationCode = locationCode != null ? locationCode : ("LOC" + (id < 10 ? "0" + id : String.valueOf(id)));
        this.name = name;
        this.population = Math.max(0, population);
        this.controlRoom = controlRoom;
    }

    public AffectedRegion(String locationCode, int id, String name, int population, boolean controlRoom) {
        this(id, locationCode, name, population, controlRoom);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNode() {
        return id;
    }

    public void setNode(int node) {
        this.id = node;
    }

    public String getLocationCode() {
        if (locationCode == null && id > 0) {
            locationCode = "LOC" + (id < 10 ? "0" + id : String.valueOf(id));
        }
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public String getCode() {
        return getLocationCode();
    }

    public void setCode(String code) {
        setLocationCode(code);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPopulation() {
        return population;
    }

    public void setPopulation(int population) {
        this.population = Math.max(0, population);
    }

    public boolean isControlRoom() {
        return controlRoom;
    }

    public void setControlRoom(boolean controlRoom) {
        this.controlRoom = controlRoom;
    }

    @Override
    public String toString() {
        return "AffectedRegion{" +
                "id=" + id +
                ", code='" + getLocationCode() + '\'' +
                ", name='" + name + '\'' +
                ", population=" + population +
                ", controlRoom=" + controlRoom +
                '}';
    }
}
