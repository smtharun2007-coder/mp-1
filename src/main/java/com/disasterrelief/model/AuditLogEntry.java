package com.disasterrelief.model;

import java.time.Instant;

/**
 * Audit log recording administrative and operational user events.
 */
public class AuditLogEntry {
    private String id;
    private String user;
    private String role;
    private String action;
    private String details;
    private Instant timestamp;

    public AuditLogEntry() {
        this.timestamp = Instant.now();
    }

    public AuditLogEntry(String id, String user, String role, String action, String details) {
        this.id = id;
        this.user = user != null ? user : "SYSTEM";
        this.role = role != null ? role : "SYSTEM";
        this.action = action;
        this.details = details;
        this.timestamp = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "AuditLogEntry{" +
                "user='" + user + '\'' +
                ", role='" + role + '\'' +
                ", action='" + action + '\'' +
                ", details='" + details + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
