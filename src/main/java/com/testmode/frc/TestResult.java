package com.testmode.frc;

/**
 * Holds the outcome of a single test step.
 */
public class TestResult {
    /** {@code true} if the measured velocity was within the configured tolerance of the baseline. */
    public boolean passed;
    /** Average motor velocity measured during the data-collection window. */
    public double averageVelocity;
    /** Baseline velocity the step was compared against. */
    public double baselineVelocity;

    /**
     * Creates a {@code TestResult}.
     *
     * @param passed           whether the step passed
     * @param averageVelocity  measured average velocity during data collection
     * @param baselineVelocity expected baseline velocity for comparison
     */
    public TestResult(boolean passed, double averageVelocity, double baselineVelocity) {
        this.passed = passed;
        this.averageVelocity = averageVelocity;
        this.baselineVelocity = baselineVelocity;
    }
}
