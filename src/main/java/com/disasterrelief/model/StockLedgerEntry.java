package com.disasterrelief.model;

import java.time.Instant;

/**
 * Immutable ledger entry recording every unit change in warehouse stock.
 * Used for strict mathematical audit: available + reserved + consumed == initial total.
 */
public class StockLedgerEntry {
    private String id;
    private String depotId;
    private String user;
    private String role;
    private String item; // "MEDICINE" or "FOOD"
    private int delta;
    private String actionType; // "RESERVE", "RELEASE", "CONSUME", "RESTOCK", "ADJUST"
    private String reason;
    private Instant timestamp;

    public StockLedgerEntry() {
        this.timestamp = Instant.now();
    }

    public StockLedgerEntry(String id, String depotId, String user, String role,
                            String item, int delta, String actionType, String reason) {
        this.id = id;
        this.depotId = depotId;
        this.user = user != null ? user : "SYSTEM";
        this.role = role != null ? role : "CONTROL_ROOM";
        this.item = item;
        this.delta = delta;
        this.actionType = actionType;
        this.reason = reason;
        this.timestamp = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDepotId() {
        return depotId;
    }

    public void setDepotId(String depotId) {
        this.depotId = depotId;
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

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public int getDelta() {
        return delta;
    }

    public void setDelta(int delta) {
        this.delta = delta;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "StockLedgerEntry{" +
                "depotId='" + depotId + '\'' +
                ", user='" + user + '\'' +
                ", item='" + item + '\'' +
                ", delta=" + delta +
                ", actionType='" + actionType + '\'' +
                ", reason='" + reason + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
