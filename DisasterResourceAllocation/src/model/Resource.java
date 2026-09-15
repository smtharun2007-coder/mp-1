package model;

public class Resource {

    // Enum to categorize the type of resource
    public enum ResourceType {
        FOOD, MEDICINE
    }

    private String resourceId;
    private ResourceType resourceType;
    private int quantity;
    private Depot depot;

    public Resource(String resourceId, ResourceType resourceType, int quantity, Depot depot) {
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.quantity = Math.max(0, quantity); // quantity cannot be negative at creation
        this.depot = depot;
    }

    public String getResourceId()           { return resourceId; }
    public ResourceType getResourceType()   { return resourceType; }
    public int getQuantity()                { return quantity; }
    public Depot getDepot()                 { return depot; }

    // Prevents quantity from going negative
    public void setQuantity(int quantity) {
        if (quantity < 0) {
            System.out.println("  Warning: Quantity cannot be negative. Setting to 0.");
            this.quantity = 0;
        } else {
            this.quantity = quantity;
        }
    }

    @Override
    public String toString() {
        return "[" + resourceId + "] " + resourceType + " | Quantity: " + quantity
                + " | Depot: " + depot.getDepotName();
    }
}
