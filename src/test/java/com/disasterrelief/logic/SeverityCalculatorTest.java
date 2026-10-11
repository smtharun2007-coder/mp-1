package com.disasterrelief.logic;

import com.disasterrelief.config.SeverityConfig;
import com.disasterrelief.model.*;
import com.disasterrelief.service.ReportService;
import com.disasterrelief.service.SeverityCalculator;
import com.disasterrelief.service.SeverityResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for SeverityCalculator, kit calculations, and Report validation rules.
 * Tests all boundary conditions, division by zero edge cases, and first-match-wins semantics.
 */
public class SeverityCalculatorTest {

    // ---------------------------------------------------------------------------------------------
    // SEVERITY 5 (CRITICAL) BOUNDARIES
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sev 5: immediateRisk boundaries (0, 1, 4 vs 5)")
    void testImmediateRiskBoundaries() {
        // immediateRisk = red + trapped = 5 -> Sev 5
        SeverityResult r5 = SeverityCalculator.calculate(100, 3, 0, 0, 0, 2, 0, 0, false);
        assertEquals(5, r5.getLevel());
        assertEquals("Critical", r5.getLabel());
        assertEquals("RULE_5_IMMEDIATE_RISK_HIGH", r5.getRuleId());
        assertTrue(r5.getExplanation().contains("5 immediate-risk people"));

        // immediateRisk = 4, threat = false -> Not Sev 5 (falls to Sev 4)
        SeverityResult r4 = SeverityCalculator.calculate(100, 2, 0, 0, 0, 2, 0, 0, false);
        assertEquals(4, r4.getLevel());

        // immediateRisk = 0, threat = false -> Not Sev 5
        SeverityResult r1 = SeverityCalculator.calculate(100, 0, 0, 0, 0, 0, 0, 0, false);
        assertEquals(1, r1.getLevel());
    }

    @Test
    @DisplayName("Sev 5: Ongoing threat with and without immediate-risk people")
    void testOngoingThreatWithImmediateRisk() {
        // Threat true and immediateRisk = 1 (red=1) -> Sev 5
        SeverityResult rThreatWithCasualty = SeverityCalculator.calculate(100, 1, 0, 0, 0, 0, 0, 0, true);
        assertEquals(5, rThreatWithCasualty.getLevel());
        assertEquals("RULE_5_THREAT_WITH_IMMEDIATE", rThreatWithCasualty.getRuleId());

        // Threat true and immediateRisk = 0 -> Not Sev 5! Falls to Sev 3 (ongoing threat without immediate casualty)
        SeverityResult rThreatWithoutCasualty = SeverityCalculator.calculate(100, 0, 0, 0, 0, 0, 0, 0, true);
        assertEquals(3, rThreatWithoutCasualty.getLevel());
        assertEquals("RULE_3_ONGOING_THREAT", rThreatWithoutCasualty.getRuleId());
    }

    // ---------------------------------------------------------------------------------------------
    // SEVERITY 4 (SEVERE) BOUNDARIES
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sev 4: immediateRisk between 1 and 4 without active ongoing threat")
    void testSeverity4Boundaries() {
        // immediateRisk = 1 (boundary lower)
        SeverityResult r1 = SeverityCalculator.calculate(100, 1, 0, 0, 0, 0, 0, 0, false);
        assertEquals(4, r1.getLevel());
        assertEquals("RULE_4_IMMEDIATE_RISK", r1.getRuleId());

        // immediateRisk = 4 (boundary upper: red=2, trapped=2)
        SeverityResult r4 = SeverityCalculator.calculate(100, 2, 0, 0, 0, 2, 0, 0, false);
        assertEquals(4, r4.getLevel());
        assertEquals("RULE_4_IMMEDIATE_RISK", r4.getRuleId());
    }

    // ---------------------------------------------------------------------------------------------
    // SEVERITY 3 (SERIOUS) BOUNDARIES
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sev 3: Delayed casualties boundaries (0, 1, 9 vs 10)")
    void testDelayedCasualtiesBoundaries() {
        // delayed = yellow = 10 -> Sev 3
        SeverityResult r10 = SeverityCalculator.calculate(100, 0, 10, 0, 0, 0, 0, 0, false);
        assertEquals(3, r10.getLevel());
        assertEquals("RULE_3_DELAYED_HIGH", r10.getRuleId());

        // delayed = 9 -> Not Sev 3 (falls to Sev 2)
        SeverityResult r9 = SeverityCalculator.calculate(100, 0, 9, 0, 0, 0, 0, 0, false);
        assertEquals(2, r9.getLevel());
        assertEquals("RULE_2_DELAYED", r9.getRuleId());

        // delayed = 1 -> Sev 2
        SeverityResult r1 = SeverityCalculator.calculate(100, 0, 1, 0, 0, 0, 0, 0, false);
        assertEquals(2, r1.getLevel());

        // delayed = 0 -> Sev 1 (when hours=0, vuln=0)
        SeverityResult r0 = SeverityCalculator.calculate(100, 0, 0, 0, 0, 0, 0, 0, false);
        assertEquals(1, r0.getLevel());
    }

    @Test
    @DisplayName("Sev 3: Hours without supplies boundaries (11, 12, 24, 25)")
    void testHoursWithoutSuppliesBoundaries() {
        // hours = 25 -> Sev 3 (threshold > 24)
        SeverityResult r25 = SeverityCalculator.calculate(100, 0, 0, 0, 0, 0, 0, 25, false);
        assertEquals(3, r25.getLevel());
        assertEquals("RULE_3_HOURS_WITHOUT_SUPPLIES", r25.getRuleId());

        // hours = 24 -> Sev 2 (threshold 12 to 24)
        SeverityResult r24 = SeverityCalculator.calculate(100, 0, 0, 0, 0, 0, 0, 24, false);
        assertEquals(2, r24.getLevel());
        assertEquals("RULE_2_HOURS_WITHOUT_SUPPLIES", r24.getRuleId());

        // hours = 12 -> Sev 2
        SeverityResult r12 = SeverityCalculator.calculate(100, 0, 0, 0, 0, 0, 0, 12, false);
        assertEquals(2, r12.getLevel());

        // hours = 11 -> Sev 1
        SeverityResult r11 = SeverityCalculator.calculate(100, 0, 0, 0, 0, 0, 0, 11, false);
        assertEquals(1, r11.getLevel());
    }

    // ---------------------------------------------------------------------------------------------
    // SEVERITY 2 (MODERATE) BOUNDARIES
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sev 2: Vulnerable population percentage boundaries (19% vs 20%)")
    void testVulnerabilityPercentageBoundaries() {
        // 20 out of 100 = 20.0% -> Sev 2
        SeverityResult r20 = SeverityCalculator.calculate(100, 0, 0, 0, 0, 0, 20, 0, false);
        assertEquals(2, r20.getLevel());
        assertEquals("RULE_2_VULNERABLE_PCT", r20.getRuleId());

        // 19 out of 100 = 19.0% -> Sev 1
        SeverityResult r19 = SeverityCalculator.calculate(100, 0, 0, 0, 0, 0, 19, 0, false);
        assertEquals(1, r19.getLevel());
    }

    @Test
    @DisplayName("Edge Case: Total affected population is 0 (No division by zero)")
    void testZeroTotalPopulationNoDivisionByZero() {
        assertDoesNotThrow(() -> {
            SeverityResult rZero = SeverityCalculator.calculate(0, 0, 0, 0, 0, 0, 0, 0, false);
            assertEquals(1, rZero.getLevel());
        });
    }

    @Test
    @DisplayName("First-Match-Wins Semantic: Case matching both Row 5 and Row 3 returns Sev 5")
    void testFirstMatchWinsSemantics() {
        // Matches Sev 5 (immediateRisk = 6 >= 5), but ALSO matches Sev 3 (delayed=15 >= 10, hours=30 > 24)
        SeverityResult r = SeverityCalculator.calculate(500, 4, 15, 50, 0, 2, 100, 30, true);
        assertEquals(5, r.getLevel(), "Should pick Severity 5 because worst finding wins first");
        assertEquals("RULE_5_IMMEDIATE_RISK_HIGH", r.getRuleId());
    }

    // ---------------------------------------------------------------------------------------------
    // KIT CALCULATION TESTS
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("Kit calculations: Ceil rounding for food and medicine")
    void testKitCalculations() {
        // Medicine: ceil(people / 10)
        assertEquals(1, SeverityConfig.calculateMedicineKits(1));
        assertEquals(1, SeverityConfig.calculateMedicineKits(10));
        assertEquals(2, SeverityConfig.calculateMedicineKits(11));
        assertEquals(10, SeverityConfig.calculateMedicineKits(95));
        assertEquals(0, SeverityConfig.calculateMedicineKits(0));

        // Food: ceil(people * days / 5)
        // 1 person, 1 day = 1/5 -> 1 kit
        assertEquals(1, SeverityConfig.calculateFoodKits(1, 1));
        // 5 people, 1 day = 5/5 -> 1 kit
        assertEquals(1, SeverityConfig.calculateFoodKits(5, 1));
        // 3 people, 2 days = 6/5 -> 2 kits
        assertEquals(2, SeverityConfig.calculateFoodKits(3, 2));
        // 10 people, 3 days = 30/5 -> 6 kits
        assertEquals(6, SeverityConfig.calculateFoodKits(10, 3));
        assertEquals(0, SeverityConfig.calculateFoodKits(0, 5));
    }

    // ---------------------------------------------------------------------------------------------
    // REPORT VALIDATION TESTS
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("Validation: Triage sum exceeding total population throws IllegalArgumentException")
    void testTriageSumExceedingTotalThrows() {
        Report report = new Report("REP_TEST", "officer1", 1);
        report.setCounts(new ReportCounts(50, 20, 20, 15, 0, 0, 5)); // 20+20+15 = 55 > 50
        report.setNeeds(new ReportNeeds(new ReportNeeds.RescueNeed(10)));
        report.setNeedOrder(List.of("RESCUE"));

        ReportService service = new ReportService(null, null, null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.validate(report, false));
        assertTrue(ex.getMessage().contains("exceeds total affected population"));
    }

    @Test
    @DisplayName("Validation: Trapped or vulnerable exceeding total throws")
    void testTrappedOrVulnerableExceedingTotalThrows() {
        ReportService service = new ReportService(null, null, null);

        // Vulnerable > Total
        Report r1 = new Report("REP_TEST", "officer1", 1);
        r1.setCounts(new ReportCounts(50, 0, 0, 0, 0, 0, 60));
        r1.setNeeds(new ReportNeeds(new ReportNeeds.RescueNeed(10)));
        r1.setNeedOrder(List.of("RESCUE"));
        assertThrows(IllegalArgumentException.class, () -> service.validate(r1, false));

        // Trapped > Total
        Report r2 = new Report("REP_TEST", "officer1", 1);
        r2.setCounts(new ReportCounts(50, 0, 0, 0, 0, 55, 0));
        r2.setNeeds(new ReportNeeds(new ReportNeeds.RescueNeed(10)));
        r2.setNeedOrder(List.of("RESCUE"));
        assertThrows(IllegalArgumentException.class, () -> service.validate(r2, false));
    }

    @Test
    @DisplayName("Validation: Negative numbers throw")
    void testNegativeNumbersThrow() {
        ReportService service = new ReportService(null, null, null);
        Report r = new Report("REP_TEST", "officer1", 1);
        r.setCounts(new ReportCounts(50, -1, 0, 0, 0, 0, 0));
        r.setNeeds(new ReportNeeds(new ReportNeeds.RescueNeed(10)));
        r.setNeedOrder(List.of("RESCUE"));
        assertThrows(IllegalArgumentException.class, () -> service.validate(r, false));
    }

    @Test
    @DisplayName("Validation: No need ticked throws")
    void testNoNeedTickedThrows() {
        ReportService service = new ReportService(null, null, null);
        Report r = new Report("REP_TEST", "officer1", 1);
        r.setCounts(new ReportCounts(50, 10, 10, 10, 0, 0, 0));
        r.setNeeds(new ReportNeeds()); // empty needs
        r.setNeedOrder(List.of());
        assertThrows(IllegalArgumentException.class, () -> service.validate(r, false));
    }

    @Test
    @DisplayName("Validation: needOrder not matching ticked needs throws")
    void testNeedOrderMismatchThrows() {
        ReportService service = new ReportService(null, null, null);
        Report r = new Report("REP_TEST", "officer1", 1);
        r.setCounts(new ReportCounts(100, 10, 10, 10, 0, 0, 0));
        ReportNeeds needs = new ReportNeeds();
        needs.setRescue(new ReportNeeds.RescueNeed(10));
        needs.setFood(new ReportNeeds.FoodNeed(50, 3, 30, 30, "reason"));
        r.setNeeds(needs);

        // needOrder only has RESCUE, misses FOOD
        r.setNeedOrder(List.of("RESCUE"));
        assertThrows(IllegalArgumentException.class, () -> service.validate(r, false));

        // needOrder has extra unselected need (MEDICINE)
        r.setNeedOrder(List.of("RESCUE", "FOOD", "MEDICINE"));
        assertThrows(IllegalArgumentException.class, () -> service.validate(r, false));

        // valid exact match in custom order
        r.setNeedOrder(List.of("FOOD", "RESCUE"));
        assertDoesNotThrow(() -> service.validate(r, false));
    }
}
