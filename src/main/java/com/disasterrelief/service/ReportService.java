package com.disasterrelief.service;

import com.disasterrelief.config.SeverityConfig;
import com.disasterrelief.model.*;
import com.disasterrelief.queue.RequestPriorityQueue;
import com.disasterrelief.repository.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Service managing the unified single assessment report lifecycle,
 * START casualty triage, deterministic severity evaluation, need-first linked requests,
 * and control room approvals / overrides.
 */
public class ReportService {

    private final ReportRepository reportRepo;
    private final RequestRepository requestRepo;
    private final CounterRepository counterRepo;
    private final AlertRepository alertRepo;
    private final AuditLogRepository auditRepo;
    private final RequestPriorityQueue priorityQueue;
    private final AllocationRepository allocationRepo;

    public ReportService(MongoDatabase database, MongoClient mongoClient, RequestPriorityQueue priorityQueue) {
        this.reportRepo = database != null ? new ReportRepository(database) : null;
        this.requestRepo = database != null ? new RequestRepository(database) : null;
        this.counterRepo = database != null ? new CounterRepository(database) : null;
        this.alertRepo = database != null ? new AlertRepository(database) : null;
        this.auditRepo = database != null ? new AuditLogRepository(database) : null;
        this.allocationRepo = database != null ? new AllocationRepository(database) : null;
        this.priorityQueue = priorityQueue;
        if (this.reportRepo != null) {
            this.reportRepo.createIndexes();
        }
    }

    /**
     * Client / Server validation of the single report form.
     */
    public void validate(Report report, boolean checkDuplicate) {
        if (report == null) throw new IllegalArgumentException("Report cannot be null");
        ReportCounts c = report.getCounts();
        if (c == null) throw new IllegalArgumentException("Casualty counts cannot be null");

        // 1. Non-negative checks
        if (c.getTotal() < 0 || c.getRed() < 0 || c.getYellow() < 0 || c.getGreen() < 0 ||
                c.getBlack() < 0 || c.getTrapped() < 0 || c.getVulnerable() < 0 || report.getHoursWithoutSupplies() < 0) {
            throw new IllegalArgumentException("Casualty counts and condition values must be whole numbers >= 0");
        }

        // 2. Bound constraints
        int triageSum = c.getRed() + c.getYellow() + c.getGreen() + c.getBlack();
        if (triageSum > c.getTotal()) {
            throw new IllegalArgumentException("Sum of Red (" + c.getRed() + ") + Yellow (" + c.getYellow() +
                    ") + Green (" + c.getGreen() + ") + Black (" + c.getBlack() + ") = " + triageSum +
                    " exceeds total affected population (" + c.getTotal() + ")");
        }
        if (c.getVulnerable() > c.getTotal()) {
            throw new IllegalArgumentException("Vulnerable population (" + c.getVulnerable() +
                    ") cannot exceed total affected population (" + c.getTotal() + ")");
        }
        if (c.getTrapped() > c.getTotal()) {
            throw new IllegalArgumentException("Trapped / missing count (" + c.getTrapped() +
                    ") cannot exceed total affected population (" + c.getTotal() + ")");
        }

        // 3. Needs validation
        ReportNeeds needs = report.getNeeds();
        if (needs == null) throw new IllegalArgumentException("Needs section cannot be null");

        boolean hasRescue = needs.getRescue() != null && needs.getRescue().getPeople() > 0;
        boolean hasShelter = needs.getShelter() != null && needs.getShelter().getPeople() > 0;
        boolean hasMedicine = needs.getMedicine() != null && (needs.getMedicine().getPeople() > 0 || needs.getMedicine().getKitsRequested() > 0);
        boolean hasFood = needs.getFood() != null && (needs.getFood().getPeople() > 0 || needs.getFood().getKitsRequested() > 0);

        if (!hasRescue && !hasShelter && !hasMedicine && !hasFood) {
            throw new IllegalArgumentException("At least one need (Rescue, Shelter, Medicine, or Food) must be ticked and requested");
        }

        if (needs.getRescue() != null) {
            if (needs.getRescue().getPeople() < 0) throw new IllegalArgumentException("Rescue count must be >= 0");
            if (needs.getRescue().getPeople() > c.getTotal()) {
                throw new IllegalArgumentException("Rescue count (" + needs.getRescue().getPeople() +
                        ") cannot exceed total affected (" + c.getTotal() + ")");
            }
        }
        if (needs.getShelter() != null) {
            if (needs.getShelter().getPeople() < 0) throw new IllegalArgumentException("Shelter count must be >= 0");
            if (needs.getShelter().getPeople() > c.getTotal()) {
                throw new IllegalArgumentException("Shelter count (" + needs.getShelter().getPeople() +
                        ") cannot exceed total affected (" + c.getTotal() + ")");
            }
        }
        if (needs.getMedicine() != null) {
            if (needs.getMedicine().getPeople() < 0 || needs.getMedicine().getKitsRequested() < 0) {
                throw new IllegalArgumentException("Medicine numbers must be >= 0");
            }
        }
        if (needs.getFood() != null) {
            if (needs.getFood().getPeople() < 0 || needs.getFood().getDays() < 0 || needs.getFood().getKitsRequested() < 0) {
                throw new IllegalArgumentException("Food numbers must be >= 0");
            }
        }

        // 4. Validate needOrder contains exactly the ticked needs
        Set<String> tickedNeeds = new HashSet<>();
        if (hasRescue) tickedNeeds.add("RESCUE");
        if (hasShelter) tickedNeeds.add("SHELTER");
        if (hasMedicine) tickedNeeds.add("MEDICINE");
        if (hasFood) tickedNeeds.add("FOOD");

        List<String> order = report.getNeedOrder();
        if (order == null || order.isEmpty()) {
            throw new IllegalArgumentException("needOrder cannot be empty when needs are selected");
        }
        Set<String> orderSet = new HashSet<>(order);
        if (order.size() != orderSet.size()) {
            throw new IllegalArgumentException("needOrder cannot contain duplicate needs");
        }
        if (!tickedNeeds.equals(orderSet)) {
            throw new IllegalArgumentException("needOrder " + order + " must contain exactly the ticked needs " + tickedNeeds);
        }

        // 5. Duplicate rejection (10-minute identical submission check)
        if (checkDuplicate && report.getOfficerId() != null && reportRepo != null) {
            Instant tenMinAgo = Instant.now().minus(Duration.ofMinutes(10));
            List<Report> recents = reportRepo.findRecentByOfficerAndRegion(report.getOfficerId(), report.getRegionId(), tenMinAgo);
            for (Report r : recents) {
                if (!r.getId().equals(report.getId()) && "PENDING_REVIEW".equals(r.getStatus())) {
                    throw new IllegalArgumentException("Duplicate report detected: an active open report for Region " +
                            report.getRegionId() + " was already submitted within the last 10 minutes (Report " + r.getId() + ")");
                }
            }
        }
    }

    /**
     * Submits a report. If submitted by Control Room (phone-in), auto-approves into the priority queue.
     */
    public Report submit(Report report, User user, boolean allowDuplicate) {
        validate(report, !allowDuplicate);

        if (report.getId() == null || report.getId().isBlank()) {
            report.setId("REP_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        report.setOfficerId(user.getUsername());
        report.setCreatedAt(Instant.now());
        report.setUpdatedAt(Instant.now());
        report.setVersion(1);

        // Calculate severity
        ReportCounts c = report.getCounts();
        SeverityResult result = SeverityCalculator.calculate(
                c.getTotal(), c.getRed(), c.getYellow(), c.getGreen(), c.getBlack(),
                c.getTrapped(), c.getVulnerable(), report.getHoursWithoutSupplies(), report.isOngoingThreat());

        report.getSeverity().setCalculated(result.getLevel());
        report.getSeverity().setRuleId(result.getRuleId());
        report.getSeverity().setExplanation(result.getExplanation());

        // Add history entry
        report.getHistory().add(new ReportHistoryEntry(1, user.getUsername(), "SUBMITTED",
                report.getCounts(), report.getSeverity(), "Initial assessment submitted"));

        if (user.getRole() == Role.CONTROL_ROOM) {
            // Control Room phone-in: directly approved & queued
            report.setStatus("APPROVED");
            reportRepo.create(report);
            approveInternal(report, user);
        } else {
            // Field Officer: queued for review
            report.setStatus("PENDING_REVIEW");
            reportRepo.create(report);
            auditRepo.log(user.getUsername(), user.getRole().name(), "REPORT_SUBMITTED",
                    "Submitted report " + report.getId() + " for Region " + report.getRegionId() + " (Calculated Severity " + result.getLevel() + ")");
        }

        return report;
    }

    /**
     * Approves a PENDING_REVIEW report, creating linked RescueRequest and SupplyRequest entries
     * and placing them in the RequestPriorityQueue according to needOrder.
     */
    public Report approve(String reportId, User controlRoomUser) {
        if (controlRoomUser.getRole() != Role.CONTROL_ROOM) {
            throw new SecurityException("Only Control Room operators can approve reports");
        }

        Report report = reportRepo.findById(reportId);
        if (report == null) throw new IllegalArgumentException("Report not found: " + reportId);
        if (!"PENDING_REVIEW".equals(report.getStatus())) {
            throw new IllegalStateException("Report " + reportId + " is already " + report.getStatus());
        }

        report.setStatus("APPROVED");
        report.setUpdatedAt(Instant.now());
        approveInternal(report, controlRoomUser);
        reportRepo.update(report);

        auditRepo.log(controlRoomUser.getUsername(), controlRoomUser.getRole().name(), "REPORT_APPROVED",
                "Approved report " + reportId + " and created linked requests: " + report.getLinkedRequestIds());
        return report;
    }

    /**
     * Core logic generating linked requests and applying the need-first tie-breaking sequence.
     *
     * MEANING OF NEED-FIRST:
     * When a report creates two linked requests (RescueRequest holding rescue/shelter, SupplyRequest holding medicine/food),
     * the request containing the first-ranked need gets the lower arrivalNo, winning the final tie-break
     * (Severity DESC -> Population DESC -> ArrivalNo ASC).
     * Inside each request, the items array is ordered per needOrder.
     */
    private void approveInternal(Report report, User operator) {
        ReportNeeds needs = report.getNeeds();
        boolean needsRescueOrShelter = (needs.getRescue() != null && needs.getRescue().getPeople() > 0) ||
                                       (needs.getShelter() != null && needs.getShelter().getPeople() > 0);
        boolean needsMedicineOrFood = (needs.getMedicine() != null && (needs.getMedicine().getPeople() > 0 || needs.getMedicine().getKitsRequested() > 0)) ||
                                      (needs.getFood() != null && (needs.getFood().getPeople() > 0 || needs.getFood().getKitsRequested() > 0));

        int effectiveSeverity = report.getSeverity().getEffectiveLevel();
        int population = report.getCounts().getTotal();
        List<String> needOrder = report.getNeedOrder();
        String firstNeed = !needOrder.isEmpty() ? needOrder.get(0) : "RESCUE";

        boolean rescueIsFirst = "RESCUE".equalsIgnoreCase(firstNeed) || "SHELTER".equalsIgnoreCase(firstNeed);

        long arrival1 = counterRepo.getNextArrivalNumber();
        long arrival2 = counterRepo.getNextArrivalNumber();

        long rescueArrival = rescueIsFirst ? arrival1 : arrival2;
        long supplyArrival = rescueIsFirst ? arrival2 : arrival1;

        List<String> linkedIds = new ArrayList<>();

        // 1. Create RescueRequest if needed
        if (needsRescueOrShelter) {
            String reqId = "REQ_RES_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            int rescuePeople = (needs.getRescue() != null) ? needs.getRescue().getPeople() : 0;
            int shelterPeople = (needs.getShelter() != null) ? needs.getShelter().getPeople() : 0;
            int totalEvac = Math.max(rescuePeople, shelterPeople);

            RescueRequest rr = new RescueRequest(reqId, report.getId(), report.getRegionId(),
                    effectiveSeverity, population, rescueArrival, RequestStatus.WAITING,
                    operator.getUsername(), UrgentNeed.RESCUE, totalEvac);
            rr.setRescueCount(rescuePeople);
            rr.setShelterCount(shelterPeople);
            rr.setSeverityLevel(effectiveSeverity);
            rr.setSeverityRuleId(report.getSeverity().getRuleId());
            rr.setSeverityExplanation(report.getSeverity().getExplanation());
            rr.setNeedOrder(needOrder);

            // Populate itemized needs ordered per needOrder
            for (String n : needOrder) {
                if ("RESCUE".equalsIgnoreCase(n) && rescuePeople > 0) {
                    rr.getItems().add(new RequestItem("RESCUE", rescuePeople));
                } else if ("SHELTER".equalsIgnoreCase(n) && shelterPeople > 0) {
                    rr.getItems().add(new RequestItem("SHELTER", shelterPeople));
                }
            }

            requestRepo.create(rr);
            priorityQueue.insert(rr);
            linkedIds.add(reqId);
        }

        // 2. Create SupplyRequest if needed
        if (needsMedicineOrFood) {
            String reqId = "REQ_SUP_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            int medKits = (needs.getMedicine() != null) ? needs.getMedicine().getKitsRequested() : 0;
            int foodKits = (needs.getFood() != null) ? needs.getFood().getKitsRequested() : 0;

            UrgentNeed primaryNeed = "MEDICINE".equalsIgnoreCase(firstNeed) ? UrgentNeed.MEDICINE : UrgentNeed.FOOD;

            SupplyRequest sr = new SupplyRequest(reqId, report.getId(), report.getRegionId(),
                    effectiveSeverity, population, supplyArrival, RequestStatus.WAITING,
                    operator.getUsername(), primaryNeed, medKits, foodKits);
            sr.setSeverityLevel(effectiveSeverity);
            sr.setSeverityRuleId(report.getSeverity().getRuleId());
            sr.setSeverityExplanation(report.getSeverity().getExplanation());
            sr.setNeedOrder(needOrder);

            // Populate itemized needs ordered per needOrder
            for (String n : needOrder) {
                if ("MEDICINE".equalsIgnoreCase(n) && medKits > 0) {
                    sr.getItems().add(new RequestItem("MEDICINE", medKits));
                } else if ("FOOD".equalsIgnoreCase(n) && foodKits > 0) {
                    sr.getItems().add(new RequestItem("FOOD", foodKits));
                }
            }

            requestRepo.create(sr);
            priorityQueue.insert(sr);
            linkedIds.add(reqId);
        }

        report.setLinkedRequestIds(linkedIds);
    }

    /**
     * Control Room severity override with mandatory justification reason.
     */
    public Report overrideSeverity(String reportId, int newLevel, String reason, User user) {
        if (user.getRole() != Role.CONTROL_ROOM) {
            throw new SecurityException("Only Control Room operators can override severity (HTTP 403)");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A mandatory justification reason is required for severity override");
        }
        if (newLevel < 1 || newLevel > 5) {
            throw new IllegalArgumentException("Severity level must be an integer between 1 and 5");
        }

        Report report = reportRepo.findById(reportId);
        if (report == null) throw new IllegalArgumentException("Report not found: " + reportId);

        report.getSeverity().setOverrideLevel(newLevel);
        report.getSeverity().setOverrideReason(reason.trim());
        report.getSeverity().setOverriddenBy(user.getUsername());
        report.getSeverity().setOverriddenAt(Instant.now());
        report.setUpdatedAt(Instant.now());

        report.getHistory().add(new ReportHistoryEntry(report.getVersion(), user.getUsername(), "OVERRIDDEN",
                report.getCounts(), report.getSeverity(), "Severity overridden to Level " + newLevel + ": " + reason));

        // If requests already exist in priority queue, update their severity level and re-sort
        for (String reqId : report.getLinkedRequestIds()) {
            Request r = requestRepo.findById(reqId);
            if (r != null) {
                r.setSeverityLevel(newLevel);
                r.setSeverity(newLevel);
                requestRepo.update(r);
                priorityQueue.update(r);
            }
        }

        reportRepo.update(report);
        auditRepo.log(user.getUsername(), user.getRole().name(), "SEVERITY_OVERRIDE",
                "Report " + reportId + " severity overridden to " + newLevel + ". Reason: " + reason);
        return report;
    }

    /**
     * Control Room rejection with required reason.
     */
    public Report reject(String reportId, String reason, User user) {
        if (user.getRole() != Role.CONTROL_ROOM) {
            throw new SecurityException("Only Control Room operators can reject reports");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A rejection reason is mandatory");
        }

        Report report = reportRepo.findById(reportId);
        if (report == null) throw new IllegalArgumentException("Report not found: " + reportId);

        report.setStatus("REJECTED");
        report.setRejectionReason(reason.trim());
        report.setUpdatedAt(Instant.now());
        report.getHistory().add(new ReportHistoryEntry(report.getVersion(), user.getUsername(), "REJECTED",
                report.getCounts(), report.getSeverity(), "Report rejected: " + reason));

        reportRepo.update(report);
        auditRepo.log(user.getUsername(), user.getRole().name(), "REPORT_REJECTED",
                "Rejected report " + reportId + ". Reason: " + reason);
        return report;
    }

    /**
     * Field Officer / Control Room updates report casualty counts or needs.
     */
    public Report updateReport(String reportId, Report updatedData, User user) {
        Report existing = reportRepo.findById(reportId);
        if (existing == null) throw new IllegalArgumentException("Report not found: " + reportId);

        // Role check
        if (user.getRole() == Role.FIELD_OFFICER) {
            if (!existing.getOfficerId().equals(user.getUsername())) {
                throw new SecurityException("Field officers may only update their own reports");
            }
            if (user.getAssignedLocationIds() != null && !user.getAssignedLocationIds().contains(existing.getRegionId())) {
                throw new SecurityException("Field officer is not assigned to Region " + existing.getRegionId());
            }
        }

        validate(updatedData, false);

        // Check if any mission has already been dispatched
        for (String reqId : existing.getLinkedRequestIds()) {
            List<Allocation> allocs = allocationRepo.findByRequestId(reqId);
            for (Allocation a : allocs) {
                if (a.getStatus() == Allocation.AllocationStatus.DISPATCHED ||
                    a.getStatus() == Allocation.AllocationStatus.DELIVERED) {
                    alertRepo.create(new AlertEntry("ALERT_" + UUID.randomUUID().toString().substring(0, 8),
                            "REPORT_UPDATE_DURING_DISPATCH",
                            "Report " + reportId + " modified while Mission " + a.getId() + " is already " + a.getStatus() + ". Human operational review required.",
                            AlertEntry.AlertSeverity.WARNING));
                }
            }
        }

        // Recalculate severity
        ReportCounts nc = updatedData.getCounts();
        SeverityResult newResult = SeverityCalculator.calculate(
                nc.getTotal(), nc.getRed(), nc.getYellow(), nc.getGreen(), nc.getBlack(),
                nc.getTrapped(), nc.getVulnerable(), updatedData.getHoursWithoutSupplies(), updatedData.isOngoingThreat());

        // Check if an existing override was cleared
        if (existing.getSeverity().getOverrideLevel() != null) {
            existing.getHistory().add(new ReportHistoryEntry(existing.getVersion(), user.getUsername(), "OVERRIDE_CLEARED",
                    existing.getCounts(), existing.getSeverity(), "Previous override of Level " + existing.getSeverity().getOverrideLevel() + " removed due to field recount update"));

            existing.getSeverity().setOverrideLevel(null);
            existing.getSeverity().setOverrideReason(null);
            existing.getSeverity().setOverriddenBy(null);
            existing.getSeverity().setOverriddenAt(null);

            alertRepo.create(new AlertEntry("ALERT_" + UUID.randomUUID().toString().substring(0, 8),
                    "OVERRIDE_REMOVED",
                    "Severity recalculated, previous override removed for report " + reportId,
                    AlertEntry.AlertSeverity.INFO));
        }

        existing.setCounts(updatedData.getCounts());
        existing.setNeeds(updatedData.getNeeds());
        existing.setNeedOrder(updatedData.getNeedOrder());
        existing.setHoursWithoutSupplies(updatedData.getHoursWithoutSupplies());
        existing.setOngoingThreat(updatedData.isOngoingThreat());
        existing.setRoadAccessObserved(updatedData.getRoadAccessObserved());
        existing.setNotes(updatedData.getNotes());
        existing.setVersion(existing.getVersion() + 1);
        existing.setUpdatedAt(Instant.now());

        existing.getSeverity().setCalculated(newResult.getLevel());
        existing.getSeverity().setRuleId(newResult.getRuleId());
        existing.getSeverity().setExplanation(newResult.getExplanation());

        // Update linked requests and PriorityQueue
        int effectiveSev = existing.getSeverity().getEffectiveLevel();
        for (String reqId : existing.getLinkedRequestIds()) {
            Request r = requestRepo.findById(reqId);
            if (r != null) {
                r.setSeverity(effectiveSev);
                r.setSeverityLevel(effectiveSev);
                r.setSeverityRuleId(newResult.getRuleId());
                r.setSeverityExplanation(newResult.getExplanation());
                r.setAffectedPopulation(nc.getTotal());

                // Check quantities: never silently reduce already allocated resources
                if (r instanceof SupplyRequest) {
                    SupplyRequest sr = (SupplyRequest) r;
                    int newMed = updatedData.getNeeds().getMedicine() != null ? updatedData.getNeeds().getMedicine().getKitsRequested() : 0;
                    int newFood = updatedData.getNeeds().getFood() != null ? updatedData.getNeeds().getFood().getKitsRequested() : 0;

                    if (newMed < sr.getMedicineAllocated()) {
                        alertRepo.create(new AlertEntry("ALERT_" + UUID.randomUUID().toString().substring(0, 8),
                                "OVER_ALLOCATED_SHORTFALL",
                                "New medicine need (" + newMed + ") is lower than allocated (" + sr.getMedicineAllocated() + ") for report " + reportId + ". Release extra inventory manually.",
                                AlertEntry.AlertSeverity.WARNING));
                    }
                    if (newFood < sr.getFoodAllocated()) {
                        alertRepo.create(new AlertEntry("ALERT_" + UUID.randomUUID().toString().substring(0, 8),
                                "OVER_ALLOCATED_SHORTFALL",
                                "New food need (" + newFood + ") is lower than allocated (" + sr.getFoodAllocated() + ") for report " + reportId + ". Release extra inventory manually.",
                                AlertEntry.AlertSeverity.WARNING));
                    }
                    sr.setMedicineNeeded(newMed);
                    sr.setFoodNeeded(newFood);
                } else if (r instanceof RescueRequest) {
                    RescueRequest rr = (RescueRequest) r;
                    int newRescue = updatedData.getNeeds().getRescue() != null ? updatedData.getNeeds().getRescue().getPeople() : 0;
                    int newShelter = updatedData.getNeeds().getShelter() != null ? updatedData.getNeeds().getShelter().getPeople() : 0;
                    int newEvac = Math.max(newRescue, newShelter);

                    if (newEvac < rr.getPeopleEvacuated()) {
                        alertRepo.create(new AlertEntry("ALERT_" + UUID.randomUUID().toString().substring(0, 8),
                                "OVER_ALLOCATED_SHORTFALL",
                                "New evacuation need (" + newEvac + ") is lower than allocated (" + rr.getPeopleEvacuated() + ") for report " + reportId + ". Release extra capacity manually.",
                                AlertEntry.AlertSeverity.WARNING));
                    }
                    rr.setPeopleToEvacuate(newEvac);
                    rr.setRescueCount(newRescue);
                    rr.setShelterCount(newShelter);
                }

                requestRepo.update(r);
                priorityQueue.update(r); // remove and re-insert, preserving arrivalNo!
            }
        }

        existing.getHistory().add(new ReportHistoryEntry(existing.getVersion(), user.getUsername(), "UPDATED",
                existing.getCounts(), existing.getSeverity(), "Assessment details updated to Version " + existing.getVersion()));

        reportRepo.update(existing);
        auditRepo.log(user.getUsername(), user.getRole().name(), "REPORT_UPDATED",
                "Updated report " + reportId + " to Version " + existing.getVersion() + " (Recalculated Severity " + newResult.getLevel() + ")");
        return existing;
    }

    public Report getById(String id) {
        return reportRepo.findById(id);
    }

    public List<Report> getPending() {
        return reportRepo.findByStatus("PENDING_REVIEW");
    }

    public List<Report> getMine(String officerId) {
        return reportRepo.findByOfficerId(officerId);
    }

    public List<Report> getAll() {
        return reportRepo.findAll();
    }
}
