package com.disasterrelief.model;

/**
 * Lifecycle statuses for a relief or rescue request.
 */
public enum RequestStatus {
    PENDING_REVIEW,
    WAITING,
    PARTIALLY_ALLOCATED,
    ALLOCATED,
    DELIVERED,
    BLOCKED,
    REJECTED
}
