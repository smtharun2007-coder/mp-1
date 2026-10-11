package com.disasterrelief.service;

import com.disasterrelief.graph.DijkstraResult;
import com.disasterrelief.graph.Graph;
import com.disasterrelief.model.*;

import java.util.*;

/**
 * Standard implementation of AllocationPolicy for District Aruvi disaster operations.
 * Enforces:
 * - 4-tier severity (Critical, High, Medium, Low)
 * - Deterministic population & arrival-sequence tie breakers
 * - Dijkstra min-heap shortest travel-time pathfinding
 * - Independent food & medicine kit tracking with partial allocation
 * - Rescue team single-assignment and shelter capacity verification
 */
public class AruviAllocationPolicy implements AllocationPolicy {

    public static final Comparator<Request> COMPARATOR = Comparator
            .comparingInt(Request::getSeverity).reversed()
            .thenComparing(Comparator.comparingInt(Request::getAffectedPopulation).reversed())
            .thenComparingLong(Request::getArrivalNo);

    @Override
    public Comparator<Request> getRequestRankingComparator() {
        return COMPARATOR;
    }

    @Override
    public AllocationPreview evaluate(ReliefRequest request,
                                      Graph graph,
                                      Collection<Depot> depots,
                                      Collection<RescueTeam> teams,
                                      Collection<Shelter> shelters) {
        AllocationPreview preview = new AllocationPreview();
        preview.setRequestId(request.getId());
        preview.setRequestType(request.getType());
        preview.setTargetRegionId(request.getRegionId());
        preview.setTargetRegionName(request.getLocationCode());

        // Verify physical road reachability from district control room (LOC01 / node 1)
        boolean roadReachable = graph.bfsReachableFrom(1).contains(request.getRegionId());
        preview.setReachable(roadReachable);

        int foodNeeded = request.getOutstandingFood();
        int medNeeded = request.getOutstandingMedicine();
        preview.setFoodNeeded(foodNeeded);
        preview.setMedicineNeeded(medNeeded);

        boolean needsSupplies = (foodNeeded > 0 || medNeeded > 0 || request instanceof SupplyRequest);
        boolean needsRescue = (request.isRescueRequired() || request instanceof RescueRequest);

        boolean supplySuccess = true;
        boolean rescueSuccess = true;

        // 1. Evaluate Supply Allocation if requested
        if (needsSupplies && (foodNeeded > 0 || medNeeded > 0)) {
            class DepotRoute {
                final Depot depot;
                final DijkstraResult route;
                DepotRoute(Depot depot, DijkstraResult route) {
                    this.depot = depot;
                    this.route = route;
                }
            }

            List<DepotRoute> reachableDepots = new ArrayList<>();
            for (Depot d : depots) {
                DijkstraResult res = graph.findShortestPath(d.getNode(), request.getRegionId());
                if (res.isReachable()) {
                    reachableDepots.add(new DepotRoute(d, res));
                }
            }

            if (reachableDepots.isEmpty()) {
                preview.setReachable(false);
                preview.setFeasible(false);
                preview.setStatusMessage("Region is completely cut off from all depots due to closed roads.");
                preview.setOutstandingFood(foodNeeded);
                preview.setOutstandingMedicine(medNeeded);
                supplySuccess = false;
            } else {
                // Nearest depot first (Dijkstra minimum travel time)
                reachableDepots.sort(Comparator.comparingInt(dr -> dr.route.getDistance()));
                preview.setReachable(true);

                int remainingFood = foodNeeded;
                int remainingMed = medNeeded;
                int totalFoodDrawn = 0;
                int totalMedDrawn = 0;

                for (DepotRoute dr : reachableDepots) {
                    if (remainingFood == 0 && remainingMed == 0) break;

                    int availFood = dr.depot.getAvailableFood();
                    int availMed = dr.depot.getAvailableMedicine();

                    int drawFood = Math.min(remainingFood, availFood);
                    int drawMed = Math.min(remainingMed, availMed);

                    if (drawFood > 0 || drawMed > 0) {
                        preview.getDepotPlans().add(new AllocationPreview.DepotPlan(
                                dr.depot.getId(),
                                dr.depot.getName(),
                                dr.depot.getNode(),
                                drawMed,
                                drawFood,
                                dr.route.getDistance(),
                                dr.route.getPath()
                        ));
                        remainingFood -= drawFood;
                        remainingMed -= drawMed;
                        totalFoodDrawn += drawFood;
                        totalMedDrawn += drawMed;
                    }
                }

                preview.setFoodAllocated(totalFoodDrawn);
                preview.setMedicineAllocated(totalMedDrawn);
                preview.setOutstandingFood(remainingFood);
                preview.setOutstandingMedicine(remainingMed);

                if (totalFoodDrawn == 0 && totalMedDrawn == 0 && (foodNeeded > 0 || medNeeded > 0)) {
                    supplySuccess = false;
                }
            }
        }

        // 2. Evaluate Rescue & Shelter Allocation if requested
        if (needsRescue) {
            int toEvacuate = request.getShelterPlacesRequested() > 0 ?
                    request.getShelterPlacesRequested() : request.getAffectedPopulation();
            preview.setPeopleToEvacuate(toEvacuate);

            // Find nearest AVAILABLE team that can reach target location
            RescueTeam bestTeam = null;
            DijkstraResult bestTeamRoute = null;

            for (RescueTeam t : teams) {
                if (t.isAvailable()) {
                    DijkstraResult res = graph.findShortestPath(t.getBaseNode(), request.getRegionId());
                    if (res.isReachable()) {
                        if (bestTeamRoute == null || res.getDistance() < bestTeamRoute.getDistance()) {
                            bestTeam = t;
                            bestTeamRoute = res;
                        }
                    }
                }
            }

            if (bestTeam == null) {
                preview.setFeasible(false);
                preview.setStatusMessage("No available rescue team can reach this region (all teams busy or roads blocked).");
                preview.setPeopleUnsheltered(toEvacuate);
                rescueSuccess = false;
            } else {
                preview.setTeamId(bestTeam.getId());
                preview.setTeamName(bestTeam.getName());
                preview.setTeamTravelTime(bestTeamRoute.getDistance());
                preview.setTeamRoute(bestTeamRoute.getPath());

                // Find nearest shelter with available capacity
                Shelter bestShelter = null;
                DijkstraResult bestShelterRoute = null;

                for (Shelter s : shelters) {
                    if (s.getAvailableCapacity() > 0) {
                        DijkstraResult res = graph.findShortestPath(request.getRegionId(), s.getNode());
                        if (res.isReachable()) {
                            if (bestShelterRoute == null || res.getDistance() < bestShelterRoute.getDistance()) {
                                bestShelter = s;
                                bestShelterRoute = res;
                            }
                        }
                    }
                }

                if (bestShelter == null) {
                    preview.setFeasible(false);
                    preview.setStatusMessage("All reachable shelters are completely full. Evacuation refused; request remains waiting.");
                    preview.setPeopleEvacuated(0);
                    preview.setPeopleUnsheltered(toEvacuate);
                    rescueSuccess = false;
                } else {
                    preview.setShelterId(bestShelter.getId());
                    preview.setShelterName(bestShelter.getName());
                    preview.setShelterTravelTime(bestShelterRoute.getDistance());
                    preview.setShelterRoute(bestShelterRoute.getPath());

                    int capacityAvailable = bestShelter.getAvailableCapacity();
                    int evacuated = Math.min(toEvacuate, capacityAvailable);
                    preview.setPeopleEvacuated(evacuated);
                    preview.setPeopleUnsheltered(Math.max(0, toEvacuate - evacuated));
                }
            }
        }

        // 3. Final Overall Status
        boolean hasAllocations = (preview.getFoodAllocated() > 0 ||
                                  preview.getMedicineAllocated() > 0 ||
                                  preview.getPeopleEvacuated() > 0);

        preview.setFeasible(hasAllocations && preview.isReachable());

        if (preview.isFeasible()) {
            boolean partialSupply = (preview.getOutstandingFood() > 0 || preview.getOutstandingMedicine() > 0);
            boolean partialRescue = (preview.getPeopleUnsheltered() > 0 || (needsRescue && preview.getTeamId() == null));
            if (partialSupply || partialRescue) {
                preview.setStatusMessage("Partial allocation planned: outstanding quantities remain due to limited capacity.");
            } else {
                preview.setStatusMessage("Full allocation ready: all required resources and travel routes confirmed.");
            }
        } else if (!preview.isReachable()) {
            preview.setStatusMessage("Region is unreachable due to road closures.");
        } else {
            preview.setStatusMessage("Zero resources could be allocated: stock exhausted or all teams/shelters busy.");
        }

        return preview;
    }
}
