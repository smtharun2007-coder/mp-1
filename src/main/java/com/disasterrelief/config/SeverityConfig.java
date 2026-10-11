package com.disasterrelief.config;

/**
 * Project-defined assumptions for START-adapted mass casualty triage and Sphere humanitarian standards.
 * All thresholds, kit ratios, and parameters are defined in one centralized class for audit transparency.
 *
 * HONEST LABELLING:
 * - START (Simple Triage and Rapid Treatment) is for adult responders; JumpSTART for pediatric triage is not implemented.
 * - These thresholds and kit calculations are PROJECT-DEFINED ASSUMPTIONS adapted from START & Sphere guidance.
 * - National disaster-level escalation is an adapted conceptual model.
 */
public final class SeverityConfig {
    private SeverityConfig() {}

    // --- Severity 5 (Critical) Thresholds ---
    public static final int SEV5_IMMEDIATE_RISK_THRESHOLD = 5;
    public static final int SEV5_THREAT_IMMEDIATE_MIN = 1;

    // --- Severity 4 (Severe) Thresholds ---
    public static final int SEV4_IMMEDIATE_MIN = 1;
    public static final int SEV4_IMMEDIATE_MAX = 4;

    // --- Severity 3 (Serious) Thresholds ---
    public static final int SEV3_DELAYED_THRESHOLD = 10;
    public static final int SEV3_HOURS_WITHOUT_SUPPLIES_THRESHOLD = 24;

    // --- Severity 2 (Moderate) Thresholds ---
    public static final int SEV2_DELAYED_MIN = 1;
    public static final int SEV2_DELAYED_MAX = 9;
    public static final int SEV2_HOURS_WITHOUT_SUPPLIES_MIN = 12;
    public static final int SEV2_HOURS_WITHOUT_SUPPLIES_MAX = 24;
    public static final double SEV2_VULNERABLE_PCT_THRESHOLD = 0.20; // 20%

    // --- Relief Supply Kit Sizing Ratios (Sphere Humanitarian Standard Assumptions) ---
    // 1 Medicine Kit supports 10 people
    public static final int MEDICINE_PEOPLE_PER_KIT = 10;
    // 1 Food Pack supports 5 people for 1 day (assumes 2,100 kcal ration for a 5-person household/day)
    public static final int FOOD_PEOPLE_PER_KIT = 5;

    public static int calculateMedicineKits(int people) {
        if (people <= 0) return 0;
        return (int) Math.ceil((double) people / MEDICINE_PEOPLE_PER_KIT);
    }

    public static int calculateFoodKits(int people, int days) {
        if (people <= 0 || days <= 0) return 0;
        return (int) Math.ceil((double) (people * days) / FOOD_PEOPLE_PER_KIT);
    }
}
