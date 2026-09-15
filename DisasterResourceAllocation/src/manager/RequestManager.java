package manager;

import model.ReliefRequest;
import model.ReliefRequest.RequestStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

public class RequestManager {

    // Stores all requests ever added (for full history)
    private ArrayList<ReliefRequest> allRequests;

    /*
     * Priority rules (highest priority first):
     *   1. Higher severity
     *   2. If equal severity → higher affected population
     *   3. If both equal → earlier arrivalOrder (smaller number = arrived first)
     */
    private PriorityQueue<ReliefRequest> priorityQueue;

    public RequestManager() {
        allRequests = new ArrayList<>();

        priorityQueue = new PriorityQueue<>(Comparator
                .comparingInt(ReliefRequest::getSeverity).reversed()
                .thenComparingInt(ReliefRequest::getAffectedPopulation).reversed()
                .thenComparingInt(ReliefRequest::getArrivalOrder));
    }

    // Adds a new request; sets its status to WAITING and enqueues it
    public void addRequest(ReliefRequest request) {
        request.setStatus(RequestStatus.WAITING);
        allRequests.add(request);
        priorityQueue.add(request);
        System.out.println("  Request " + request.getRequestId() + " added successfully.");
    }

    // Displays all requests in the system
    public void viewAllRequests() {
        if (allRequests.isEmpty()) {
            System.out.println("  No requests found.");
            return;
        }
        System.out.println("  --- All Relief Requests ---");
        for (ReliefRequest r : allRequests) {
            System.out.println("  " + r);
        }
    }

    // Displays only WAITING requests in priority order
    public void viewPendingRequests() {
        // Collect only WAITING requests from the queue into a temp list for display
        ArrayList<ReliefRequest> pending = new ArrayList<>();
        for (ReliefRequest r : priorityQueue) {
            if (r.getStatus() == RequestStatus.WAITING) {
                pending.add(r);
            }
        }

        if (pending.isEmpty()) {
            System.out.println("  No pending requests.");
            return;
        }

        // Sort the display list using the same priority rules
        pending.sort(Comparator
                .comparingInt(ReliefRequest::getSeverity).reversed()
                .thenComparingInt(ReliefRequest::getAffectedPopulation).reversed()
                .thenComparingInt(ReliefRequest::getArrivalOrder));

        System.out.println("  --- Pending Requests (by Priority) ---");
        int rank = 1;
        for (ReliefRequest r : pending) {
            System.out.println("  #" + rank++ + " " + r);
        }
    }

    // Returns (and removes) the highest-priority WAITING request
    public ReliefRequest getNextPriorityRequest() {
        // Skip any requests that are no longer WAITING
        while (!priorityQueue.isEmpty()) {
            ReliefRequest top = priorityQueue.peek();
            if (top.getStatus() == RequestStatus.WAITING) {
                return priorityQueue.poll();
            }
            priorityQueue.poll(); // discard non-waiting entries
        }
        return null;
    }

    public ArrayList<ReliefRequest> getAllRequests() {
        return allRequests;
    }
}
