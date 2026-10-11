package com.disasterrelief.model;

import java.time.Instant;

/**
 * Calculated severity and optional control room override on a disaster report.
 */
public class ReportSeverity {
    private int calculated; // 1 to 5
    private String ruleId;
    private String explanation;
    private Integer overrideLevel; // null if no override
    private String overrideReason;
    private String overriddenBy;
    private Instant overriddenAt;

    public ReportSeverity() {}

    public ReportSeverity(int calculated, String ruleId, String explanation) {
        this.calculated = calculated;
        this.ruleId = ruleId;
        this.explanation = explanation;
    }

    /**
     * Effective severity level: returns overrideLevel if set, otherwise calculated.
     */
    public int getEffectiveLevel() {
        return (overrideLevel != null) ? overrideLevel : calculated;
    }

    public int getCalculated() {
        return calculated;
    }

    public void setCalculated(int calculated) {
        this.calculated = calculated;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Integer getOverrideLevel() {
        return overrideLevel;
    }

    public void setOverrideLevel(Integer overrideLevel) {
        this.overrideLevel = overrideLevel;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public String getOverriddenBy() {
        return overriddenBy;
    }

    public void setOverriddenBy(String overriddenBy) {
        this.overriddenBy = overriddenBy;
    }

    public Instant getOverriddenAt() {
        return overriddenAt;
    }

    public void setOverriddenAt(Instant overriddenAt) {
        this.overriddenAt = overriddenAt;
    }
}
