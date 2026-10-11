package com.disasterrelief.config;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Centralized application configuration.
 * All settings are sourced strictly from environment variables or .env.
 */
public final class AppConfig {
    private static final String DEFAULT_DB = "DisasterManagementDB";
    private static final String DEFAULT_TEST_DB = "DisasterManagementDB_test";
    private static final int DEFAULT_PORT = 7070;

    private static String cachedControlPassword;
    private static String cachedOfficerPassword;

    private AppConfig() {}

    /**
     * Gets the MongoDB connection URI.
     * Fails fast with a detailed error message if unset.
     */
    public static String getMongoUri() {
        String uri = DotenvHelper.get("MONGODB_URI");
        if (uri == null || uri.isBlank()) {
            throw new IllegalStateException(
                "\n====================================================================================\n" +
                " FATAL CONFIGURATION ERROR: MONGODB_URI environment variable is missing!\n" +
                "====================================================================================\n" +
                " This application requires a MongoDB Atlas connection string to start.\n" +
                " Please set MONGODB_URI in a '.env' file in your project root or via environment:\n\n" +
                "   Example: MONGODB_URI=mongodb+srv://<user>:<password>@cluster0.abcde.mongodb.net/?retryWrites=true&w=majority\n\n" +
                " Refer to README.md for step-by-step MongoDB Atlas setup instructions.\n" +
                "====================================================================================\n"
            );
        }
        return uri.trim();
    }

    /**
     * Checks if MongoDB URI is provided without throwing an exception.
     */
    public static boolean hasMongoUri() {
        String uri = DotenvHelper.get("MONGODB_URI");
        return uri != null && !uri.isBlank();
    }

    public static String getDatabaseName() {
        return DotenvHelper.getOrDefault("MONGODB_DB", DEFAULT_DB);
    }

    public static String getTestDatabaseName() {
        return DotenvHelper.getOrDefault("MONGODB_TEST_DB", DEFAULT_TEST_DB);
    }

    public static int getPort() {
        String portStr = DotenvHelper.get("PORT");
        if (portStr != null) {
            try {
                return Integer.parseInt(portStr.trim());
            } catch (NumberFormatException ignored) {}
        }
        return DEFAULT_PORT;
    }

    public static synchronized String getDemoControlPassword() {
        if (cachedControlPassword != null) return cachedControlPassword;
        String pw = DotenvHelper.get("DEMO_CONTROL_PASSWORD");
        if (pw != null && !pw.isBlank()) {
            cachedControlPassword = pw.trim();
        } else {
            cachedControlPassword = generateRandomPassword("Control");
            System.out.println("  [NOTICE] DEMO_CONTROL_PASSWORD unset. Generated: " + cachedControlPassword);
        }
        return cachedControlPassword;
    }

    public static synchronized String getDemoOfficerPassword() {
        if (cachedOfficerPassword != null) return cachedOfficerPassword;
        String pw = DotenvHelper.get("DEMO_OFFICER_PASSWORD");
        if (pw != null && !pw.isBlank()) {
            cachedOfficerPassword = pw.trim();
        } else {
            cachedOfficerPassword = generateRandomPassword("Officer");
            System.out.println("  [NOTICE] DEMO_OFFICER_PASSWORD unset. Generated: " + cachedOfficerPassword);
        }
        return cachedOfficerPassword;
    }

    private static String generateRandomPassword(String prefix) {
        byte[] bytes = new byte[6];
        new SecureRandom().nextBytes(bytes);
        return prefix + "@" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
