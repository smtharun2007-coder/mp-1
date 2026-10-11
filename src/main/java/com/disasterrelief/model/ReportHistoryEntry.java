package com.disasterrelief.model;

import java.time.Instant;

/**
 * Historical snapshot entry tracking report amendments and cleared overrides.
 */
public class ReportHistoryEntry {
    private int version;
    private Instant timestamp;
    private String operator;
    private String action; // e.g. "CREATED", "UPDATED", "OVERRIDDEN", "OVERRIDE_CLEARED"
    private ReportCounts counts;
    private ReportSeverity severity;
    private String note;

    public ReportHistoryEntry() {
        this.timestamp = Instant.now();
    }

    public ReportHistoryEntry(int version, String operator, String action, ReportCounts counts, ReportSeverity severity, String note) {
        this.version = version;
        this.timestamp = Instant.now();
        this.operator = operator;
        this.action = action;
        this.counts = counts;
        this.severity = severity;
        this.note = note;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public ReportCounts getCounts() {
        return counts;
    }

    public void setCounts(ReportCounts counts) {
        this.counts = counts;
    }

    public ReportSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ReportSeverity severity) {
        this.severity = severity;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
