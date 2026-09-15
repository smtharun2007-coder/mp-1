package model;

public class Shelter {

    private String shelterId;
    private String shelterName;
    private int capacity;
    private int reservedCapacity;

    public Shelter(String shelterId, String shelterName, int capacity) {
        this.shelterId = shelterId;
        this.shelterName = shelterName;
        this.capacity = capacity;
        this.reservedCapacity = 0;
    }

    public String getShelterId()        { return shelterId; }
    public String getShelterName()      { return shelterName; }
    public int getCapacity()            { return capacity; }
    public int getReservedCapacity()    { return reservedCapacity; }

    // Available capacity = total capacity minus what is already reserved
    public int getAvailableCapacity() {
        return capacity - reservedCapacity;
    }

    // Prevents over-reservation beyond total capacity
    public void setReservedCapacity(int reservedCapacity) {
        if (reservedCapacity > capacity) {
            System.out.println("  Warning: Reserved capacity cannot exceed total capacity. Setting to max.");
            this.reservedCapacity = capacity;
        } else if (reservedCapacity < 0) {
            this.reservedCapacity = 0;
        } else {
            this.reservedCapacity = reservedCapacity;
        }
    }

    @Override
    public String toString() {
        return "[" + shelterId + "] " + shelterName
                + " | Total: " + capacity
                + " | Reserved: " + reservedCapacity
                + " | Available: " + getAvailableCapacity();
    }
}
