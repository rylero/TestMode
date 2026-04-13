package com.testmode.frc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;

/**
 * Fluent builder for constructing a sequence of {@link TestModeStep}s to run during robot test mode.
 *
 * <p>Usage example:
 * <pre>{@code
 * Command testCommand = new TestModeBuilder()
 *     .withFlywheelStep(config, motor::setVoltage, encoder::getVelocity)
 *     .withTestResultConsumer(results -> System.out.println("Done: " + results.size() + " steps"))
 *     .build();
 * }</pre>
 */
public class TestModeBuilder {
    private List<TestModeStep> steps;
    private List<Consumer<List<TestResult>>> testResultConsumers;
    private Consumer<Boolean> overallResultConsumer;

    /**
     * Creates a new {@code TestModeBuilder} with an empty step list.
     */
    public TestModeBuilder() {
        steps = new ArrayList<>();
        testResultConsumers = new ArrayList<>();
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
     * Adds a positional test step that moves to a target position before measuring velocity.
     * Target position, position tolerance, and move timeout are configured via
     * {@link TestStepConfig#withTargetPosition}, {@link TestStepConfig#withPositionTolerance},
     * and {@link TestStepConfig#withMoveTimeout}.
     *
     * @param stepConfig       configuration for voltage, timing, tolerance, position, and name
     * @param setMotorVoltage  consumer that applies a voltage (volts) to the motor
     * @param getMotorVelocity supplier that returns the current motor velocity
     * @param setPosition      consumer that commands the mechanism to a position
     * @param getPosition      supplier that returns the current mechanism position
     * @return this builder, for chaining
     */
    public TestModeBuilder withPositionalStep(
            TestStepConfig stepConfig,
            DoubleConsumer setMotorVoltage,
            DoubleSupplier getMotorVelocity,
            DoubleConsumer setPosition,
            DoubleSupplier getPosition) {
        TestModePositionalStep step = new TestModePositionalStep(
            stepConfig, setMotorVoltage, getMotorVelocity, setPosition, getPosition);
        steps.add(step);
        return this;
    }

    /**
     * Adds a consumer that receives all test results after every step has completed.
     * Multiple consumers can be added and will each be called in order.
     *
     * @param consumer callback that receives the full list of {@link TestResult}s
     * @return this builder, for chaining
     */
    public TestModeBuilder withTestResultConsumer(Consumer<List<TestResult>> consumer) {
        this.testResultConsumers.add(consumer);
        return this;
    }

    /**
     * Sets a consumer that receives {@code true} if every step passed, {@code false} otherwise.
     *
     * @param consumer callback that receives the overall pass/fail result
     * @return this builder, for chaining
     */
    public TestModeBuilder withOverallResultConsumer(Consumer<Boolean> consumer) {
        this.overallResultConsumer = consumer;
        return this;
    }

    /**
     * Builds a command that runs every step in baseline-recording mode, saving each measured
     * velocity as the new baseline. Does not evaluate pass/fail and does not call any consumers.
     *
     * @return command that records baselines for all steps
     */
    public Command buildBaselineCommand() {
        Command[] stepCommands = steps.stream()
            .map(TestModeStep::recordBaseline)
            .toArray(Command[]::new);
        return Commands.sequence(stepCommands);
    }

    public Command buildTestCommand() {
        List<TestResult> results = new ArrayList<>();
        Consumer<TestResult> collector = results::add;

        Command[] stepCommands = steps.stream()
            .map(step -> step.runStep(collector))
            .toArray(Command[]::new);

        Command reportCommand = Commands.runOnce(() -> {
            boolean allPassed = results.stream().allMatch(r -> r.passed);
            for (Consumer<List<TestResult>> consumer : testResultConsumers) {
                consumer.accept(results);
            }
            if (overallResultConsumer != null) {
                overallResultConsumer.accept(allPassed);
            }
        });

        return Commands.sequence(Commands.sequence(stepCommands), reportCommand);
    }
}
