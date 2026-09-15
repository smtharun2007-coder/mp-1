package model;

public class Road {

    private String roadId;
    private Location source;
    private Location destination;
    private int travelTime;   // in minutes
    private boolean open;

    public Road(String roadId, Location source, Location destination, int travelTime, boolean open) {
        this.roadId = roadId;
        this.source = source;
        this.destination = destination;
        this.travelTime = travelTime;
        this.open = open;
    }

    public String getRoadId()           { return roadId; }
    public Location getSource()         { return source; }
    public Location getDestination()    { return destination; }
    public int getTravelTime()          { return travelTime; }
    public boolean isOpen()             { return open; }

    public void setOpen(boolean open)   { this.open = open; }

    @Override
    public String toString() {
        String status = open ? "OPEN" : "CLOSED";
        return "[" + roadId + "] " + source.getLocationName() + " -> " + destination.getLocationName()
                + " | Travel Time: " + travelTime + " min | Status: " + status;
    }
}
