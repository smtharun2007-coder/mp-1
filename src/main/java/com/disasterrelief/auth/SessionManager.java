package com.disasterrelief.auth;

import com.disasterrelief.model.Role;
import com.disasterrelief.model.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory session manager using ConcurrentHashMap.
 */
public class SessionManager {

    public static class UserSession {
        private final String token;
        private final String userId;
        private final String username;
        private final Role role;
        private final List<Integer> assignedLocationIds;
        private final Instant createdAt;

        public UserSession(String token, String userId, String username, Role role, List<Integer> assignedLocationIds) {
            this.token = token;
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.assignedLocationIds = assignedLocationIds != null ? List.copyOf(assignedLocationIds) : List.of();
            this.createdAt = Instant.now();
        }

        public String getToken() { return token; }
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        public Role getRole() { return role; }
        public List<Integer> getAssignedLocationIds() { return assignedLocationIds; }
        public Instant getCreatedAt() { return createdAt; }
    }

    private final ConcurrentHashMap<String, UserSession> sessions = new ConcurrentHashMap<>();

    public UserSession createSession(User user) {
        String token = UUID.randomUUID().toString();
        UserSession session = new UserSession(token, user.getId(), user.getUsername(), user.getRole(), user.getAssignedLocationIds());
        sessions.put(token, session);
        return session;
    }

    public UserSession getSession(String token) {
        if (token == null || token.isBlank()) return null;
        return sessions.get(token.trim());
    }

    public void removeSession(String token) {
        if (token != null) {
            sessions.remove(token.trim());
        }
    }

    public void clearAll() {
        sessions.clear();
    }
}
