package model;

public class Depot {

    private String depotId;
    private String depotName;
    private Location location;

    public Depot(String depotId, String depotName, Location location) {
        this.depotId = depotId;
        this.depotName = depotName;
        this.location = location;
    }

    public String getDepotId()      { return depotId; }
    public String getDepotName()    { return depotName; }
    public Location getLocation()   { return location; }

    @Override
    public String toString() {
        return "[" + depotId + "] " + depotName + " | Location: " + location.getLocationName();
    }
}
