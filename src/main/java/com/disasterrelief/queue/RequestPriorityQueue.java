package com.disasterrelief.queue;

import com.disasterrelief.model.Request;
import com.disasterrelief.model.RequestStatus;

import java.util.*;

/**
 * Thread-safe live in-memory priority queue maintaining the active triage dispatch order.
 *
 * Multi-criteria ranking rules (Fixing previous comparator ambiguity):
 * 1. Severity DESC (Severity 5 = most critical emergency, 1 = lowest)
 * 2. Affected Population DESC (Larger affected casualty/population group first)
 * 3. Arrival Order ASC (FIFO tie-breaker: smaller arrivalNo = arrived earlier, avoiding starvation)
 *
 * Complexity Notes:
 * - PriorityQueue insert (add) / poll: O(log n)
 * - PriorityQueue remove(Object): O(n) linear search through heap array
 * - Updating a request: remove(req) then add(req) -> O(n)
 */
public class RequestPriorityQueue {

    /**
     * Deterministic Comparator enforcing the disaster prioritization hierarchy.
     */
    public static final Comparator<Request> PRIORITY_COMPARATOR = Comparator
            .comparingInt(Request::getSeverity).reversed()
            .thenComparing(Comparator.comparingInt(Request::getAffectedPopulation).reversed())
            .thenComparingLong(Request::getArrivalNo);

    private final PriorityQueue<Request> queue;
    private final Object lock = new Object();

    public RequestPriorityQueue() {
        this.queue = new PriorityQueue<>(PRIORITY_COMPARATOR);
    }

    /**
     * Inserts a request into the priority queue.
     * Complexity: O(log n).
     */
    public void add(Request request) {
        if (request == null) return;
        synchronized (lock) {
            queue.add(request);
        }
    }

    /**
     * Alias for add(request).
     */
    public void insert(Request request) {
        add(request);
    }

    /**
     * Removes a request from the queue.
     * Complexity: O(n) due to array scan in PriorityQueue.
     */
    public boolean remove(Request request) {
        if (request == null) return false;
        synchronized (lock) {
            return queue.remove(request);
        }
    }

    /**
     * Updates an existing request in the queue by removing and re-inserting it.
     * This forces the binary heap to re-heapify and restore priority order.
     * Complexity: O(n) for removal + O(log n) for re-insertion = O(n).
     */
    public void update(Request request) {
        if (request == null) return;
        synchronized (lock) {
            // Documented: remove() is O(n)
            queue.remove(request);
            if (request.getStatus() == RequestStatus.WAITING ||
                request.getStatus() == RequestStatus.PARTIALLY_ALLOCATED) {
                queue.add(request);
            }
        }
    }

    /**
     * Retrieves and removes the highest priority waiting request.
     * Complexity: O(log n).
     */
    public Request poll() {
        synchronized (lock) {
            return queue.poll();
        }
    }

    /**
     * Retrieves without removing the highest priority request.
     * Complexity: O(1).
     */
    public Request peek() {
        synchronized (lock) {
            return queue.peek();
        }
    }

    /**
     * Returns an ordered snapshot of all currently waiting requests in priority order.
     */
    public List<Request> getOrderedSnapshot() {
        synchronized (lock) {
            List<Request> list = new ArrayList<>(queue);
            list.sort(PRIORITY_COMPARATOR);
            return list;
        }
    }

    public Request find(java.util.function.Predicate<Request> predicate) {
        synchronized (lock) {
            for (Request r : queue) {
                if (predicate.test(r)) return r;
            }
            return null;
        }
    }

    /**
     * Rebuilds the in-memory queue from a collection of persisted requests.
     * Only requests in WAITING or PARTIALLY_ALLOCATED status are queued.
     */
    public void rebuild(Collection<? extends Request> requests) {
        synchronized (lock) {
            queue.clear();
            if (requests != null) {
                for (Request r : requests) {
                    if (r.getStatus() == RequestStatus.WAITING ||
                        r.getStatus() == RequestStatus.PARTIALLY_ALLOCATED) {
                        queue.add(r);
                    }
                }
            }
        }
    }

    public int size() {
        synchronized (lock) {
            return queue.size();
        }
    }

    public boolean isEmpty() {
        synchronized (lock) {
            return queue.isEmpty();
        }
    }

    public void clear() {
        synchronized (lock) {
            queue.clear();
        }
    }

    /**
     * Generates a clear, deterministic justification for a request's rank in the UI.
     * Displays severity explanation, population, arrival sequence number, and need-first position.
     */
    public static String getRankReason(Request r) {
        if (r == null) return "";
        String sevExp = (r.getSeverityExplanation() != null && !r.getSeverityExplanation().isBlank())
                ? r.getSeverityExplanation()
                : ("Severity Level " + r.getSeverity());
        String needFirstInfo = (r.getNeedOrder() != null && !r.getNeedOrder().isEmpty())
                ? " | Need-First: [" + String.join(", ", r.getNeedOrder()) + "]"
                : "";
        return String.format("%s | Pop: %,d | Arrival #%d%s",
                sevExp, r.getAffectedPopulation(), r.getArrivalNo(), needFirstInfo);
    }
}
