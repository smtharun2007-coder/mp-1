import manager.RequestManager;
import model.*;
import model.ReliefRequest.RequestStatus;
import model.Resource.ResourceType;
import model.RescueTeam.TeamStatus;

import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        // -------------------------------------------------------
        // SAMPLE DATA SETUP
        // -------------------------------------------------------

        // --- Locations ---
        Location l1 = new Location("L01", "Salem");
        Location l2 = new Location("L02", "Attur");
        Location l3 = new Location("L03", "Mettur");
        Location l4 = new Location("L04", "Omalur");
        Location l5 = new Location("L05", "Sankari");

        ArrayList<Location> locations = new ArrayList<>();
        locations.add(l1); locations.add(l2); locations.add(l3);
        locations.add(l4); locations.add(l5);

        // --- Roads ---
        ArrayList<Road> roads = new ArrayList<>();
        roads.add(new Road("R01", l1, l2, 45, true));   // Salem -> Attur
        roads.add(new Road("R02", l1, l3, 60, true));   // Salem -> Mettur
        roads.add(new Road("R03", l1, l4, 30, true));   // Salem -> Omalur
        roads.add(new Road("R04", l2, l5, 50, false));  // Attur -> Sankari (CLOSED)
        roads.add(new Road("R05", l3, l4, 40, true));   // Mettur -> Omalur
        roads.add(new Road("R06", l4, l5, 35, true));   // Omalur -> Sankari
        roads.add(new Road("R07", l2, l3, 55, false));  // Attur -> Mettur (CLOSED)

        // --- Depots ---
        Depot d1 = new Depot("D01", "Salem Central Depot", l1);
        Depot d2 = new Depot("D02", "Mettur Relief Depot", l3);

        ArrayList<Depot> depots = new ArrayList<>();
        depots.add(d1); depots.add(d2);

        // --- Resources ---
        Resource r1 = new Resource("RS01", ResourceType.FOOD,     500, d1);
        Resource r2 = new Resource("RS02", ResourceType.MEDICINE,  300, d1);
        Resource r3 = new Resource("RS03", ResourceType.FOOD,     400, d2);
        Resource r4 = new Resource("RS04", ResourceType.MEDICINE,  250, d2);

        ArrayList<Resource> resources = new ArrayList<>();
        resources.add(r1); resources.add(r2); resources.add(r3); resources.add(r4);

        // --- Rescue Teams ---
        RescueTeam t1 = new RescueTeam("T01", "Rescue Team 1", TeamStatus.AVAILABLE);
        RescueTeam t2 = new RescueTeam("T02", "Rescue Team 2", TeamStatus.AVAILABLE);
        RescueTeam t3 = new RescueTeam("T03", "Rescue Team 3", TeamStatus.BUSY);

        ArrayList<RescueTeam> rescueTeams = new ArrayList<>();
        rescueTeams.add(t1); rescueTeams.add(t2); rescueTeams.add(t3);

        // --- Shelters ---
        Shelter s1 = new Shelter("S01", "Salem Relief Shelter",  300);
        Shelter s2 = new Shelter("S02", "Mettur Relief Shelter", 200);
        s1.setReservedCapacity(80);
        s2.setReservedCapacity(50);

        ArrayList<Shelter> shelters = new ArrayList<>();
        shelters.add(s1); shelters.add(s2);

        // --- Request Manager ---
        RequestManager requestManager = new RequestManager();

        // Pre-load 5 sample relief requests
        requestManager.addRequest(new ReliefRequest("RQ01", l2, 3, 200, 100, 60, true,  true,  1));
        requestManager.addRequest(new ReliefRequest("RQ02", l5, 5, 500, 250, 150, true, true,  2));
        requestManager.addRequest(new ReliefRequest("RQ03", l4, 2, 80,  40,  20,  false, false, 3));
        requestManager.addRequest(new ReliefRequest("RQ04", l3, 5, 300, 180, 100, true,  false, 4));
        requestManager.addRequest(new ReliefRequest("RQ05", l2, 4, 150, 90,  50,  false, true,  5));

        // --- Sample Allocation (manually created for demo) ---
        ArrayList<Allocation> allocations = new ArrayList<>();
        allocations.add(new Allocation(
                "AL01", "RQ03", d1, 40, 20, null, null,
                "Salem -> Omalur", 30, "ALLOCATED"
        ));

        // -------------------------------------------------------
        // MAIN MENU LOOP
        // -------------------------------------------------------

        int arrivalCounter = 6; // continues from the 5 pre-loaded requests

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("  Enter your choice: ");

            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input. Please enter a number.\n");
                continue;
            }

            switch (choice) {

                case 1: // View Locations
                    System.out.println("\n  --- Locations ---");
                    for (Location loc : locations) {
                        System.out.println("  " + loc);
                    }
                    break;

                case 2: // View Road Network
                    System.out.println("\n  --- Road Network ---");
                    for (Road road : roads) {
                        System.out.println("  " + road);
                    }
                    break;

                case 3: // View Resources / Stock
                    System.out.println("\n  --- Resources / Stock ---");
                    for (Resource res : resources) {
                        System.out.println("  " + res);
                    }
                    break;

                case 4: // Add Relief Request
                    arrivalCounter = addReliefRequest(scanner, locations, requestManager, arrivalCounter);
                    break;

                case 5: // View Pending Requests
                    System.out.println();
                    requestManager.viewPendingRequests();
                    break;

                case 6:  // Process Relief Requests (not yet implemented)
                case 7:  // Update Road Status
                case 8:  // Update Resource Stock
                case 9:  // Update Relief Request
                case 13: // View System Status
                    System.out.println("\n  ⚠ This feature is planned but not implemented in the current prototype.");
                    break;

                case 10: // View Allocations
                    System.out.println("\n  --- Allocations ---");
                    if (allocations.isEmpty()) {
                        System.out.println("  No allocations recorded yet.");
                    } else {
                        for (Allocation alloc : allocations) {
                            System.out.println("  " + alloc);
                        }
                    }
                    break;

                case 11: // View Rescue Teams
                    System.out.println("\n  --- Rescue Teams ---");
                    for (RescueTeam team : rescueTeams) {
                        System.out.println("  " + team);
                    }
                    break;

                case 12: // View Shelters
                    System.out.println("\n  --- Shelters ---");
                    for (Shelter shelter : shelters) {
                        System.out.println("  " + shelter);
                    }
                    break;

                case 14: // Exit
                    System.out.println("\n  Exiting system. Stay safe!\n");
                    running = false;
                    break;

                default:
                    System.out.println("\n  Invalid choice. Please select a valid option.");
            }

            if (running) System.out.println();
        }

        scanner.close();
    }

    // -------------------------------------------------------
    // Prints the main menu
    // -------------------------------------------------------
    private static void printMenu() {
        System.out.println("============================================================");
        System.out.println("   DISASTER MANAGEMENT RESOURCE ALLOCATION SYSTEM");
        System.out.println("============================================================");
        System.out.println("  1.  [✓] View Locations");
        System.out.println("  2.  [✓] View Road Network");
        System.out.println("  3.  [✓] View Resources / Stock");
        System.out.println("  4.  [✓] Add Relief Request");
        System.out.println("  5.  [✓] View Pending Requests");
        System.out.println("  6.  [ ] Process Relief Requests");
        System.out.println("  7.  [ ] Update Road Status");
        System.out.println("  8.  [ ] Update Resource Stock");
        System.out.println("  9.  [ ] Update Relief Request");
        System.out.println("  10. [✓] View Allocations");
        System.out.println("  11. [✓] View Rescue Teams");
        System.out.println("  12. [✓] View Shelters");
        System.out.println("  13. [ ] View System Status");
        System.out.println("  14.     Exit");
        System.out.println("============================================================");
    }

    // -------------------------------------------------------
    // Handles adding a new relief request via console input
    // -------------------------------------------------------
    private static int addReliefRequest(Scanner scanner, ArrayList<Location> locations,
                                        RequestManager requestManager, int arrivalCounter) {
        System.out.println("\n  --- Add New Relief Request ---");

        // Display available locations
        System.out.println("  Available Locations:");
        for (int i = 0; i < locations.size(); i++) {
            System.out.println("    " + (i + 1) + ". " + locations.get(i));
        }

        // Select location
        System.out.print("  Select location number: ");
        int locChoice;
        try {
            locChoice = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("  Invalid input. Returning to menu.");
            return arrivalCounter;
        }

        if (locChoice < 1 || locChoice > locations.size()) {
            System.out.println("  Invalid location choice. Returning to menu.");
            return arrivalCounter;
        }
        Location selectedLocation = locations.get(locChoice - 1);

        // Severity
        System.out.print("  Enter severity ranking (high=1 to low=5): ");
        int severity;
        try {
            severity = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("  Invalid input. Returning to menu.");
            return arrivalCounter;
        }

        // Affected population
        System.out.print("  Enter affected population: ");
        int population;
        try {
            population = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("  Invalid input. Returning to menu.");
            return arrivalCounter;
        }

        // Food required
        System.out.print("  Enter food required (units): ");
        int food;
        try {
            food = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("  Invalid input. Returning to menu.");
            return arrivalCounter;
        }

        // Medicine required
        System.out.print("  Enter medicine required (units): ");
        int medicine;
        try {
            medicine = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("  Invalid input. Returning to menu.");
            return arrivalCounter;
        }

        // Rescue team required
        System.out.print("  Rescue team required? (yes/no): ");
        boolean rescueRequired = scanner.nextLine().trim().equalsIgnoreCase("yes");

        // Shelter required
        System.out.print("  Shelter required? (yes/no): ");
        boolean shelterRequired = scanner.nextLine().trim().equalsIgnoreCase("yes");

        // Generate request ID
        String requestId = "RQ" + String.format("%02d", arrivalCounter);

        ReliefRequest newRequest = new ReliefRequest(
                requestId, selectedLocation, severity, population,
                food, medicine, rescueRequired, shelterRequired, arrivalCounter
        );

        requestManager.addRequest(newRequest);
        return arrivalCounter + 1;
    }
}
