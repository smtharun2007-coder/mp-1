package com.disasterrelief.logic;

import com.disasterrelief.auth.SessionManager;
import com.disasterrelief.model.Role;
import com.disasterrelief.model.User;
import com.disasterrelief.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure logic unit tests verifying Role-Based Access Control (RBAC) and session management.
 * Requires no database.
 */
public class RbacAndSecurityTest {

    private SessionManager sessionManager;
    private User controlUser;
    private User officerUser;

    @BeforeEach
    public void setUp() {
        sessionManager = new SessionManager();

        String salt1 = PasswordUtil.generateSaltHex();
        String hash1 = PasswordUtil.hashPassword("AdminPass123", salt1);
        controlUser = new User("usr_admin", "admin", hash1, salt1, Role.CONTROL_ROOM, List.of(1, 2, 3));

        String salt2 = PasswordUtil.generateSaltHex();
        String hash2 = PasswordUtil.hashPassword("OfficerPass123", salt2);
        officerUser = new User("usr_officer", "officer1", hash2, salt2, Role.FIELD_OFFICER, List.of(1, 2));
    }

    @Test
    @DisplayName("Session manager should issue unique secure tokens and map roles correctly")
    public void testSessionCreationAndRetrieval() {
        SessionManager.UserSession session = sessionManager.createSession(controlUser);
        assertNotNull(session);
        assertNotNull(session.getToken());
        assertEquals("admin", session.getUsername());
        assertEquals(Role.CONTROL_ROOM, session.getRole());

        SessionManager.UserSession retrieved = sessionManager.getSession(session.getToken());
        assertEquals(session, retrieved);

        sessionManager.removeSession(session.getToken());
        assertNull(sessionManager.getSession(session.getToken()));
    }

    @Test
    @DisplayName("Field officer should be strictly identified by Role.FIELD_OFFICER")
    public void testFieldOfficerRoleIdentification() {
        SessionManager.UserSession officerSession = sessionManager.createSession(officerUser);
        assertEquals(Role.FIELD_OFFICER, officerSession.getRole());
        assertNotEquals(Role.CONTROL_ROOM, officerSession.getRole());

        // Field officer assigned jurisdiction
        assertEquals(List.of(1, 2), officerSession.getAssignedLocationIds());
    }

    @Test
    @DisplayName("Password verification should succeed for valid credentials and fail for wrong passwords")
    public void testAuthenticationValidation() {
        assertTrue(PasswordUtil.verifyPassword("AdminPass123", controlUser.getSalt(), controlUser.getPasswordHash()));
        assertFalse(PasswordUtil.verifyPassword("WrongPassword", controlUser.getSalt(), controlUser.getPasswordHash()));
    }
}
