package com.disasterrelief.model;

/**
 * Represents a geographical administrative region affected by disaster.
 * Extends AffectedRegion for District Aruvi compatibility.
 */
public class Region extends AffectedRegion {

    public Region() {
        super();
    }

    public Region(int id, String name, int population) {
        super(id, "LOC" + (id < 10 ? "0" + id : String.valueOf(id)), name, population, id == 1);
    }

    public Region(String code, int id, String name, int population, boolean controlRoom) {
        super(id, code, name, population, controlRoom);
    }

    @Override
    public String toString() {
        return "Region{" +
                "id=" + getId() +
                ", code='" + getLocationCode() + '\'' +
                ", name='" + getName() + '\'' +
                ", population=" + getPopulation() +
                ", controlRoom=" + isControlRoom() +
                '}';
    }
}
