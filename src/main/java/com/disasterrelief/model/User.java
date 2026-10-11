package com.disasterrelief.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * System user entity with role-based access control and assigned jurisdiction regions.
 */
public class User {
    private String id;
    private String username;
    private String passwordHash;
    private String salt;
    private Role role;
    private List<Integer> assignedLocationIds = new ArrayList<>();
    private boolean active = true;
    private Instant createdAt;

    public User() {
        this.createdAt = Instant.now();
    }

    public User(String id, String username, String passwordHash, String salt, Role role, List<Integer> assignedLocationIds) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role != null ? role : Role.FIELD_OFFICER;
        this.assignedLocationIds = assignedLocationIds != null ? assignedLocationIds : new ArrayList<>();
        this.active = true;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<Integer> getAssignedLocationIds() {
        return assignedLocationIds;
    }

    public void setAssignedLocationIds(List<Integer> assignedLocationIds) {
        this.assignedLocationIds = assignedLocationIds != null ? assignedLocationIds : new ArrayList<>();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
