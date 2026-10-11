package com.disasterrelief.model;

import java.time.Instant;

/**
 * Base abstract class for emergency incident requests.
 * Prioritised according to: Severity DESC, Population DESC, ArrivalNo ASC.
 */
public abstract class Request {
    private String id;
    private String reportId;
    private int regionId;
    private int severity; // 1 to 5 (5 is most severe)
    private int affectedPopulation;
    private long arrivalNo; // Sequence counter, strictly unique and immutable once assigned
    private RequestStatus status;
    private String createdBy;
    private String type; // "RESCUE" or "SUPPLY" discriminator
    private UrgentNeed mostUrgentNeed;
    private String rejectionReason;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    // Spec additions: itemized need tracking & transparent rule explanations
    private java.util.List<RequestItem> items = new java.util.ArrayList<>();
    private int severityLevel; // Effective severity
    private String severityRuleId;
    private String severityExplanation;
    private java.util.List<String> needOrder = new java.util.ArrayList<>();

    public Request() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = RequestStatus.PENDING_REVIEW;
    }

    public Request(String id, String reportId, int regionId, int severity, int affectedPopulation,
                   long arrivalNo, RequestStatus status, String createdBy, String type, UrgentNeed mostUrgentNeed) {
        this.id = id;
        this.reportId = reportId;
        this.regionId = regionId;
        this.severity = Math.max(1, Math.min(5, severity));
        this.severityLevel = this.severity;
        this.affectedPopulation = Math.max(0, affectedPopulation);
        this.arrivalNo = arrivalNo;
        this.status = status != null ? status : RequestStatus.PENDING_REVIEW;
        this.createdBy = createdBy != null ? createdBy : "system";
        this.type = type;
        this.mostUrgentNeed = mostUrgentNeed != null ? mostUrgentNeed : UrgentNeed.RESCUE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Determines whether this request can be fulfilled given current graph reachability and resource status.
     * Implemented by specific request subclasses.
     */
    public abstract boolean isFulfillable();

    // --- Getters & Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public int getRegionId() {
        return regionId;
    }

    public void setRegionId(int regionId) {
        this.regionId = regionId;
    }

    public int getSeverity() {
        return severity;
    }

    public void setSeverity(int severity) {
        this.severity = Math.max(1, Math.min(5, severity));
        this.updatedAt = Instant.now();
    }

    public int getAffectedPopulation() {
        return affectedPopulation;
    }

    public void setAffectedPopulation(int affectedPopulation) {
        this.affectedPopulation = Math.max(0, affectedPopulation);
        this.updatedAt = Instant.now();
    }

    public long getArrivalNo() {
        return arrivalNo;
    }

    public void setArrivalNo(long arrivalNo) {
        this.arrivalNo = arrivalNo;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UrgentNeed getMostUrgentNeed() {
        return mostUrgentNeed;
    }

    public void setMostUrgentNeed(UrgentNeed mostUrgentNeed) {
        this.mostUrgentNeed = mostUrgentNeed;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public java.util.List<RequestItem> getItems() {
        return items;
    }

    public void setItems(java.util.List<RequestItem> items) {
        this.items = items != null ? items : new java.util.ArrayList<>();
    }

    public int getSeverityLevel() {
        return severityLevel > 0 ? severityLevel : severity;
    }

    public void setSeverityLevel(int severityLevel) {
        this.severityLevel = Math.max(1, Math.min(5, severityLevel));
        this.severity = this.severityLevel;
        this.updatedAt = Instant.now();
    }

    public String getSeverityRuleId() {
        return severityRuleId;
    }

    public void setSeverityRuleId(String severityRuleId) {
        this.severityRuleId = severityRuleId;
    }

    public String getSeverityExplanation() {
        return severityExplanation;
    }

    public void setSeverityExplanation(String severityExplanation) {
        this.severityExplanation = severityExplanation;
    }

    public java.util.List<String> getNeedOrder() {
        return needOrder;
    }

    public void setNeedOrder(java.util.List<String> needOrder) {
        this.needOrder = needOrder != null ? needOrder : new java.util.ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Request)) return false;
        Request request = (Request) o;
        return id != null && id.equals(request.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
