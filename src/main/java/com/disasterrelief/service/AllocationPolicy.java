package com.disasterrelief.service;

import com.disasterrelief.graph.Graph;
import com.disasterrelief.model.*;

import java.util.Collection;
import java.util.Comparator;

/**
 * Abstraction defining resource allocation strategy and prioritization policy.
 * Decouples ranking rules and feasibility calculations from persistence and presentation.
 */
public interface AllocationPolicy {

    /**
     * Deterministic Comparator enforcing the exact disaster triage hierarchy:
     * 1. Severity: Critical (4) > High (3) > Medium (2) > Low (1)
     * 2. Affected Population DESC
     * 3. Arrival Sequence ASC (deterministic tie-breaker, avoids starvation)
     */
    Comparator<Request> getRequestRankingComparator();

    /**
     * Evaluates allocation feasibility for a relief request against current graph reachability,
     * reachable depots, available rescue teams, and shelter capacity.
     *
     * @param request  the waiting relief request
     * @param graph    live graph built from currently open roads
     * @param depots   all available supply depots
     * @param teams    all rescue teams
     * @param shelters all evacuation shelters
     * @return preview plan containing routes, assigned teams/shelters, kit allocations, and shortfall
     */
    AllocationPreview evaluate(ReliefRequest request,
                              Graph graph,
                              Collection<Depot> depots,
                              Collection<RescueTeam> teams,
                              Collection<Shelter> shelters);
}
