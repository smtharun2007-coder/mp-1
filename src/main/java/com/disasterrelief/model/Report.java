package com.disasterrelief.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain model representing a unified single assessment report submitted per affected region.
 * Captures START triage casualty counts, physical threats, multi-need demands, and need-first prioritization.
 */
public class Report {
    private String id;
    private String officerId;
    private int regionId;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;
    private String status; // "PENDING_REVIEW", "APPROVED", "REJECTED"
    private ReportCounts counts;
    private int hoursWithoutSupplies;
    private boolean ongoingThreat;
    private String roadAccessObserved; // "OPEN", "BLOCKED", "UNKNOWN"
    private ReportNeeds needs;
    private List<String> needOrder = new ArrayList<>();
    private String notes;
    private ReportSeverity severity;
    private List<String> linkedRequestIds = new ArrayList<>();
    private String rejectionReason;
    private List<ReportHistoryEntry> history = new ArrayList<>();

    public Report() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.version = 1;
        this.status = "PENDING_REVIEW";
        this.counts = new ReportCounts();
        this.needs = new ReportNeeds();
        this.severity = new ReportSeverity();
    }

    public Report(String id, String officerId, int regionId) {
        this();
        this.id = id;
        this.officerId = officerId;
        this.regionId = regionId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOfficerId() {
        return officerId;
    }

    public void setOfficerId(String officerId) {
        this.officerId = officerId;
    }

    public int getRegionId() {
        return regionId;
    }

    public void setRegionId(int regionId) {
        this.regionId = regionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public ReportCounts getCounts() {
        return counts;
    }

    public void setCounts(ReportCounts counts) {
        this.counts = counts;
    }

    public int getHoursWithoutSupplies() {
        return hoursWithoutSupplies;
    }

    public void setHoursWithoutSupplies(int hoursWithoutSupplies) {
        this.hoursWithoutSupplies = hoursWithoutSupplies;
    }

    public boolean isOngoingThreat() {
        return ongoingThreat;
    }

    public void setOngoingThreat(boolean ongoingThreat) {
        this.ongoingThreat = ongoingThreat;
    }

    public String getRoadAccessObserved() {
        return roadAccessObserved;
    }

    public void setRoadAccessObserved(String roadAccessObserved) {
        this.roadAccessObserved = roadAccessObserved;
    }

    public ReportNeeds getNeeds() {
        return needs;
    }

    public void setNeeds(ReportNeeds needs) {
        this.needs = needs;
    }

    public List<String> getNeedOrder() {
        return needOrder;
    }

    public void setNeedOrder(List<String> needOrder) {
        this.needOrder = needOrder != null ? needOrder : new ArrayList<>();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public ReportSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ReportSeverity severity) {
        this.severity = severity;
    }

    public List<String> getLinkedRequestIds() {
        return linkedRequestIds;
    }

    public void setLinkedRequestIds(List<String> linkedRequestIds) {
        this.linkedRequestIds = linkedRequestIds != null ? linkedRequestIds : new ArrayList<>();
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public List<ReportHistoryEntry> getHistory() {
        return history;
    }

    public void setHistory(List<ReportHistoryEntry> history) {
        this.history = history != null ? history : new ArrayList<>();
    }
}
