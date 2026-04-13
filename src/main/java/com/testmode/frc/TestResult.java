package com.testmode.frc;

/**
 * Holds the outcome of a single test step.
 */
public class TestResult {
    /** Human-readable name of the test step that produced this result. */
    public String stepName;
    /** {@code true} if the measured velocity was within the configured tolerance of the baseline. */
    public boolean passed;
    /** Average motor velocity measured during the data-collection window. */
    public double averageVelocity;
    /** Baseline velocity the step was compared against. */
    public double baselineVelocity;
    /** {@code true} if this result came from a positional test step. */
    public boolean isPositionalTest;
    /**
     * {@code true} if the mechanism reached the start position before the move timeout.
     * Always {@code true} for non-positional steps.
     */
    public boolean reachedStartPosition;

    /**
     * Creates a {@code TestResult}.
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
    }
}
