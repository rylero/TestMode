package com.testmode.frc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Fluent builder for constructing a sequence of {@link TestModeStep}s to run during robot test mode.
 *
 * <p>Usage example:
 * <pre>{@code
 * List<TestModeStep> steps = new TestModeBuilder()
 *     .withFlywheelStep(config, motor::setVoltage, encoder::getVelocity)
 *     .build();
 * }</pre>
 */
public class TestModeBuilder {
    private List<TestModeStep> steps;

    /**
     * Creates a new {@code TestModeBuilder} with an empty step list.
     */
    public TestModeBuilder() {
        steps = new ArrayList<>();
    }

    /**
     * Adds a flywheel test step that applies a voltage and measures steady-state velocity.
     *
     * @param stepConfig       configuration for the step (voltage, timing, tolerance, name)
     * @param setMotorVoltage  consumer that applies a voltage (in volts) to the motor
     * @param getMotorVelocity supplier that returns the current motor velocity (in RPM or rad/s)
     * @return this builder, for chaining
     */
    public TestModeBuilder withFlywheelStep(TestStepConfig stepConfig, DoubleConsumer setMotorVoltage, DoubleSupplier getMotorVelocity) {
        TestModeFlywheelStep step = new TestModeFlywheelStep(stepConfig, setMotorVoltage, getMotorVelocity);
        steps.add(step);
        return this;
    }

    /**
     * Adds a positional test step to the sequence.
     *
     * @return this builder, for chaining
     */
    public TestModeBuilder withPositionalStep() {
        TestModePositionalStep step = new TestModePositionalStep();
        steps.add(step);
        return this;
    }
}
