package com.disasterrelief.service;

import com.disasterrelief.config.SeverityConfig;

import java.util.Locale;

/**
 * Deterministic severity evaluation engine implementing the START-adapted decision table.
 * Pure function with zero database access.
 */
public final class SeverityCalculator {

    private SeverityCalculator() {}

    /**
     * Evaluates field assessment parameters against the prioritized decision table.
     * Evaluated in strict order (Worst finding wins): Severity 5 -> 4 -> 3 -> 2 -> 1.
     * Returns the FIRST match with its deterministic rule ID and natural language explanation.
     */
    public static SeverityResult calculate(int totalAffected,
                                           int red,
                                           int yellow,
                                           int green,
                                           int black,
                                           int trapped,
                                           int vulnerable,
                                           int hoursWithoutSupplies,
                                           boolean ongoingThreat) {

        int immediateRisk = red + trapped;
        int delayed = yellow;
        double vulnerablePct = (totalAffected > 0) ? ((double) vulnerable / totalAffected) : 0.0;

        // --- ROW 1: Severity 5 (Critical) ---
        // immediateRisk >= 5, OR (ongoingThreat AND immediateRisk >= 1)
        if (immediateRisk >= SeverityConfig.SEV5_IMMEDIATE_RISK_THRESHOLD) {
            return new SeverityResult(5, "Critical", "RULE_5_IMMEDIATE_RISK_HIGH",
                    "Critical: " + immediateRisk + " immediate-risk people (threshold " + SeverityConfig.SEV5_IMMEDIATE_RISK_THRESHOLD + ")");
        }
        if (ongoingThreat && immediateRisk >= SeverityConfig.SEV5_THREAT_IMMEDIATE_MIN) {
            return new SeverityResult(5, "Critical", "RULE_5_THREAT_WITH_IMMEDIATE",
                    "Critical: ongoing threat with " + immediateRisk + " immediate-risk people");
        }

        // --- ROW 2: Severity 4 (Severe) ---
        // immediateRisk between 1 and 4
        if (immediateRisk >= SeverityConfig.SEV4_IMMEDIATE_MIN && immediateRisk <= SeverityConfig.SEV4_IMMEDIATE_MAX) {
            return new SeverityResult(4, "Severe", "RULE_4_IMMEDIATE_RISK",
                    "Severe: " + immediateRisk + " immediate-risk people (threshold 1-4)");
        }

        // --- ROW 3: Severity 3 (Serious) ---
        // delayed >= 10, OR ongoingThreat, OR hoursWithoutSupplies > 24
        if (delayed >= SeverityConfig.SEV3_DELAYED_THRESHOLD) {
            return new SeverityResult(3, "Serious", "RULE_3_DELAYED_HIGH",
                    "Serious: " + delayed + " delayed casualties (threshold " + SeverityConfig.SEV3_DELAYED_THRESHOLD + ")");
        }
        if (ongoingThreat) {
            return new SeverityResult(3, "Serious", "RULE_3_ONGOING_THREAT",
                    "Serious: active ongoing threat detected");
        }
        if (hoursWithoutSupplies > SeverityConfig.SEV3_HOURS_WITHOUT_SUPPLIES_THRESHOLD) {
            return new SeverityResult(3, "Serious", "RULE_3_HOURS_WITHOUT_SUPPLIES",
                    "Serious: " + hoursWithoutSupplies + " hours without supplies (threshold >" + SeverityConfig.SEV3_HOURS_WITHOUT_SUPPLIES_THRESHOLD + "h)");
        }

        // --- ROW 4: Severity 2 (Moderate) ---
        // delayed between 1 and 9, OR hoursWithoutSupplies between 12 and 24, OR vulnerablePct >= 0.20
        if (delayed >= SeverityConfig.SEV2_DELAYED_MIN && delayed <= SeverityConfig.SEV2_DELAYED_MAX) {
            return new SeverityResult(2, "Moderate", "RULE_2_DELAYED",
                    "Moderate: " + delayed + " delayed casualties (threshold 1-9)");
        }
        if (hoursWithoutSupplies >= SeverityConfig.SEV2_HOURS_WITHOUT_SUPPLIES_MIN &&
                hoursWithoutSupplies <= SeverityConfig.SEV2_HOURS_WITHOUT_SUPPLIES_MAX) {
            return new SeverityResult(2, "Moderate", "RULE_2_HOURS_WITHOUT_SUPPLIES",
                    "Moderate: " + hoursWithoutSupplies + " hours without supplies (threshold 12-24h)");
        }
        if (vulnerablePct >= SeverityConfig.SEV2_VULNERABLE_PCT_THRESHOLD) {
            return new SeverityResult(2, "Moderate", "RULE_2_VULNERABLE_PCT",
                    String.format(Locale.US, "Moderate: %.1f%% vulnerable population (threshold >=20.0%%)", vulnerablePct * 100.0));
        }

        // --- ROW 5: Severity 1 (Low) ---
        // none of the above
        return new SeverityResult(1, "Low", "RULE_1_LOW",
                "Low: baseline priority with no critical triggers met");
    }

    /**
     * Calculates suggested medicine kit count: ceil(people / 10).
     */
    public static int calculateSuggestedMedicineKits(int people) {
        if (people <= 0) return 0;
        return (int) Math.ceil((double) people / SeverityConfig.MEDICINE_PEOPLE_PER_KIT);
    }

    /**
     * Calculates suggested food kit count: ceil((people * days) / 5).
     */
    public static int calculateSuggestedFoodKits(int people, int days) {
        if (people <= 0 || days <= 0) return 0;
        return (int) Math.ceil((double) (people * days) / SeverityConfig.FOOD_PEOPLE_PER_KIT);
    }
}
