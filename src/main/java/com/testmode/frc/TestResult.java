package com.testmode.frc;

/**
 * Holds the outcome of a single test step or extra condition check.
 */
public class TestResult {
    /** Human-readable name of the test step or condition that produced this result. */
    public String stepName;
    /** {@code true} if the step/condition passed. */
    public boolean passed;
    /** Average motor velocity measured during the data-collection window. */
    public double averageVelocity;
    /** Baseline velocity the step was compared against. */
    public double baselineVelocity;
    /** {@code true} if this result came from a positional test step. */
    public boolean isPositionalTest;
    /**
     * {@code true} if the mechanism reached the start position before the move timeout.
     * Always {@code true} for non-positional steps and condition checks.
     */
    public boolean reachedStartPosition;
    /**
     * {@code true} if this result is from an extra condition check rather than a motor step.
     * When {@code true}, velocity fields are not meaningful.
     */
    public boolean isConditionCheck;

    /**
     * Creates a {@code TestResult} for a motor step.
     *
     * @param stepName             human-readable name of the test step
     * @param passed               whether the step passed
     * @param averageVelocity      measured average velocity during data collection
     * @param baselineVelocity     expected baseline velocity for comparison
     * @param isPositionalTest     whether this step involved moving to a start position first
     * @param reachedStartPosition whether the mechanism reached the start position before timeout
     */
    public TestResult(String stepName, boolean passed, double averageVelocity, double baselineVelocity,
            boolean isPositionalTest, boolean reachedStartPosition) {
        this.stepName = stepName;
        this.passed = passed;
        this.averageVelocity = averageVelocity;
        this.baselineVelocity = baselineVelocity;
        this.isPositionalTest = isPositionalTest;
        this.reachedStartPosition = reachedStartPosition;
        this.isConditionCheck = false;
    }

    /**
     * Creates a {@code TestResult} for an extra condition check.
     *
     * @param title  human-readable name of the condition
     * @param passed whether the condition was met
     * @return a condition-check result
     */
    public static TestResult forCondition(String title, boolean passed) {
        TestResult r = new TestResult(title, passed, 0, 0, false, true);
        r.isConditionCheck = true;
        return r;
    }
}
