package com.disasterrelief.service;

/**
 * Result of evaluating field conditions against the deterministic START-adapted triage decision table.
 */
public class SeverityResult {
    private final int level; // 1 to 5
    private final String label; // Critical, Severe, Serious, Moderate, Low
    private final String ruleId;
    private final String explanation;

    public SeverityResult(int level, String label, String ruleId, String explanation) {
        this.level = level;
        this.label = label;
        this.ruleId = ruleId;
        this.explanation = explanation;
    }

    public int getLevel() {
        return level;
    }

    public String getLabel() {
        return label;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getExplanation() {
        return explanation;
    }

    @Override
    public String toString() {
        return "SeverityResult{" +
                "level=" + level +
                ", label='" + label + '\'' +
                ", ruleId='" + ruleId + '\'' +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}
