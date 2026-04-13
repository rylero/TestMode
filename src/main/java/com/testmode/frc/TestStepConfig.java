package com.testmode.frc;

/**
 * Configuration for a single test step.
 *
 * <p>Defaults: voltage = 4 V, applyTime = 3 s, dataTime = 2 s, tolerance = 5%,
 * targetPosition = 0, positionTolerance = 0.1, moveTimeout = 5 s.
 */
public class TestStepConfig {
    /** Voltage applied to the motor during the step (volts). */
    public double voltage;
    /** Duration to apply the voltage before collecting data (seconds). */
    public double applyTime;
    /** Duration to collect velocity data after the motor has spun up (seconds). */
    public double dataTime;
    /** Human-readable name identifying this step in test results. */
    public String stepName;
    /** Acceptable fractional deviation from the baseline velocity (0.05 = 5%). */
    public double tolerance;
    /** Target position to move to before applying voltage (positional steps only). */
    public double targetPosition;
    /** Acceptable error from {@code targetPosition} before proceeding (positional steps only). */
    public double positionTolerance;
    /** Maximum seconds to wait for the mechanism to reach position (positional steps only). */
    public double moveTimeout;

    /**
     * Creates a config with the given name and default values.
     *
     * @param stepName human-readable name for this test step
     */
    public TestStepConfig(String stepName) {
        this.stepName = stepName;
        this.voltage = 4;
        this.applyTime = 3;
        this.dataTime = 2;
        this.tolerance = 0.05;
        this.targetPosition = 0;
        this.positionTolerance = 0.1;
        this.moveTimeout = 5;
    }

    /**
     * Sets the voltage applied during the step.
     *
     * @param voltage voltage in volts
     * @return this config, for chaining
     */
    public TestStepConfig withVoltage(double voltage) {
        this.voltage = voltage;
        return this;
    }

    /**
     * Sets how long to apply the voltage before data collection begins.
     *
     * @param applyTime ramp-up duration in seconds
     * @return this config, for chaining
     */
    public TestStepConfig withApplyTime(double applyTime) {
        this.applyTime = applyTime;
        return this;
    }

    /**
     * Sets how long to collect velocity data.
     *
     * @param dataTime data collection duration in seconds
     * @return this config, for chaining
     */
    public TestStepConfig withDataTime(double dataTime) {
        this.dataTime = dataTime;
        return this;
    }

    /**
     * Sets the acceptable fractional deviation from baseline velocity.
     *
     * @param tolerance fractional tolerance (e.g., 0.05 for 5%)
     * @return this config, for chaining
     */
    public TestStepConfig withTolerance(double tolerance) {
        this.tolerance = tolerance;
        return this;
    }

    /**
     * Sets the target position to move to before applying voltage (positional steps only).
     *
     * @param targetPosition desired mechanism position
     * @return this config, for chaining
     */
    public TestStepConfig withTargetPosition(double targetPosition) {
        this.targetPosition = targetPosition;
        return this;
    }

    /**
     * Sets the acceptable error from the target position before proceeding (positional steps only).
     *
     * @param positionTolerance acceptable position error
     * @return this config, for chaining
     */
    public TestStepConfig withPositionTolerance(double positionTolerance) {
        this.positionTolerance = positionTolerance;
        return this;
    }

    /**
     * Sets the maximum seconds to wait for the mechanism to reach position (positional steps only).
     *
     * @param moveTimeout timeout in seconds
     * @return this config, for chaining
     */
    public TestStepConfig withMoveTimeout(double moveTimeout) {
        this.moveTimeout = moveTimeout;
        return this;
    }
}
