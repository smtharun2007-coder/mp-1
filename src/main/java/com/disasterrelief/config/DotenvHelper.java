package com.disasterrelief.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight helper to parse local .env files into system environment overrides
 * without adding third-party heavy dependencies.
 */
public final class DotenvHelper {
    private static final Map<String, String> ENV_CACHE = new HashMap<>();

    static {
        loadDotenv(new File(".env"));
    }

    private DotenvHelper() {}

    public static synchronized void loadDotenv(File envFile) {
        if (!envFile.exists() || !envFile.isFile()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    String value = line.substring(eqIdx + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    ENV_CACHE.put(key, value);
                }
            }
        } catch (IOException ignored) {
            // Silently ignore file reading issues for fallback
        }
    }

    /**
     * Retrieves an environment variable, checking OS environment first, then .env file.
     */
    public static String get(String key) {
        String val = System.getenv(key);
        if (val != null && !val.isBlank()) {
            return val.trim();
        }
        val = System.getProperty(key);
        if (val != null && !val.isBlank()) {
            return val.trim();
        }
        val = ENV_CACHE.get(key);
        return (val != null && !val.isBlank()) ? val.trim() : null;
    }

    /**
     * Retrieves an environment variable or returns the default value.
     */
    public static String getOrDefault(String key, String defaultValue) {
        String val = get(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }
}
