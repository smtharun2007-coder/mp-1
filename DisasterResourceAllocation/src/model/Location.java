package model;

public class Location {

    private String locationId;
    private String locationName;

    public Location(String locationId, String locationName) {
        this.locationId = locationId;
        this.locationName = locationName;
    }

    public String getLocationId() { return locationId; }
    public String getLocationName() { return locationName; }

    @Override
    public String toString() {
        return "[" + locationId + "] " + locationName;
    }
}
