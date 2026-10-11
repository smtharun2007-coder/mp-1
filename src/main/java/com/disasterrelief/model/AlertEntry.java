package com.disasterrelief.model;

import java.time.Instant;

/**
 * Operational alert generated for the control room (e.g., active missions affected by road closures).
 */
public class AlertEntry {
    private String id;
    private String title;
    private String message;
    private String severity; // INFO, WARNING, CRITICAL
    private String relatedId;
    private boolean acknowledged;
    private Instant timestamp;

    public enum AlertSeverity {
        INFO, WARNING, CRITICAL
    }

    public AlertEntry() {
        this.timestamp = Instant.now();
        this.acknowledged = false;
    }

    public AlertEntry(String id, String title, String message, String severity, String relatedId) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.severity = severity != null ? severity : "WARNING";
        this.relatedId = relatedId;
        this.acknowledged = false;
        this.timestamp = Instant.now();
    }

    public AlertEntry(String id, String title, String message, String severity) {
        this(id, title, message, severity, null);
    }

    public AlertEntry(String id, String title, String message, AlertSeverity severity) {
        this(id, title, message, severity != null ? severity.name() : "WARNING", null);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(String relatedId) {
        this.relatedId = relatedId;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
