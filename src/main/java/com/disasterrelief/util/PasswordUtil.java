package com.disasterrelief.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;

/**
 * Cryptographic utility for password hashing and verification using PBKDF2WithHmacSHA256.
 * Enforces 120,000 iterations with a random 16-byte salt.
 */
public final class PasswordUtil {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 125_000;
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {}

    /**
     * Generates a cryptographically secure random 16-byte salt.
     */
    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    /**
     * Returns the hex representation of the salt.
     */
    public static String generateSaltHex() {
        return HexFormat.of().formatHex(generateSalt());
    }

    /**
     * Hashes a plaintext password with the provided salt using PBKDF2WithHmacSHA256.
     */
    public static String hashPassword(String password, String saltHex) {
        if (password == null || saltHex == null) {
            throw new IllegalArgumentException("Password and salt must not be null");
        }
        byte[] salt = HexFormat.of().parseHex(saltHex);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to calculate PBKDF2 hash", e);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Verifies that a plaintext password matches the stored salt and hash.
     */
    public static boolean verifyPassword(String password, String saltHex, String expectedHashHex) {
        if (password == null || saltHex == null || expectedHashHex == null) {
            return false;
        }
        String computedHash = hashPassword(password, saltHex);
        return computedHash.equalsIgnoreCase(expectedHashHex);
    }
}
