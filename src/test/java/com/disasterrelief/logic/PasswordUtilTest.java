package com.disasterrelief.logic;

import com.disasterrelief.util.PasswordUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests PBKDF2 hashing, random salt generation, and verification.
 */
public class PasswordUtilTest {

    @Test
    @DisplayName("Password hashing with PBKDF2 should verify correctly and reject incorrect passwords")
    public void testPasswordHashingAndVerification() {
        String password = "StrongPassword@2026";
        String saltHex = PasswordUtil.generateSaltHex();

        assertNotNull(saltHex);
        assertEquals(32, saltHex.length(), "16-byte salt in hex should be 32 characters");

        String hash = PasswordUtil.hashPassword(password, saltHex);
        assertNotNull(hash);

        // Verification should succeed with correct password
        assertTrue(PasswordUtil.verifyPassword(password, saltHex, hash));

        // Verification must fail with wrong password
        assertFalse(PasswordUtil.verifyPassword("WrongPassword", saltHex, hash));
        assertFalse(PasswordUtil.verifyPassword("", saltHex, hash));
        assertFalse(PasswordUtil.verifyPassword(null, saltHex, hash));
    }
}
