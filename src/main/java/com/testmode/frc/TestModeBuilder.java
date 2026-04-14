package com.testmode.frc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
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
    private List<String> conditionTitles;
    private List<BooleanSupplier> conditionChecks;

    /**
     * Creates a new {@code TestModeBuilder} with an empty step list.
     */
    public TestModeBuilder() {
        steps = new ArrayList<>();
        testResultConsumers = new ArrayList<>();
        conditionTitles = new ArrayList<>();
        conditionChecks = new ArrayList<>();
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
     * Adds a multi-flywheel test step that applies a single voltage and measures each motor's
     * steady-state velocity independently. Results are reported as {@code stepName_0},
     * {@code stepName_1}, etc.
     *
     * @param stepConfig         configuration for the step (voltage, timing, tolerance, name)
     * @param setMotorVoltage    consumer that applies a voltage (in volts) to all motors
     * @param getMotorVelocities list of suppliers returning each motor's current velocity
     * @return this builder, for chaining
     */
    public TestModeBuilder withMultiFlywheelStep(TestStepConfig stepConfig, DoubleConsumer setMotorVoltage, List<DoubleSupplier> getMotorVelocities) {
        steps.add(new TestModeMultiFlywheelStep(stepConfig, setMotorVoltage, getMotorVelocities));
        return this;
    }

    /**
     * Adds a multi-flywheel test step that applies a single voltage and measures each motor's
     * steady-state velocity independently. Results are reported as {@code stepName_0},
     * {@code stepName_1}, etc.
     *
     * @param stepConfig         configuration for the step (voltage, timing, tolerance, name)
     * @param setMotorVoltage    consumer that applies a voltage (in volts) to all motors
     * @param getMotorVelocities varargs of suppliers returning each motor's current velocity
     * @return this builder, for chaining
     */
    public TestModeBuilder withMultiFlywheelStep(TestStepConfig stepConfig, DoubleConsumer setMotorVoltage, DoubleSupplier... getMotorVelocities) {
        steps.add(new TestModeMultiFlywheelStep(stepConfig, setMotorVoltage, Arrays.asList(getMotorVelocities)));
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
     * Adds a multi-motor positional test step that moves to a target position, then applies a
     * single voltage and measures each motor's velocity independently. Results are reported as
     * {@code stepName_0}, {@code stepName_1}, etc.
     *
     * @param stepConfig          configuration for voltage, timing, tolerance, position, and name
     * @param setMotorVoltage     consumer that applies a voltage (volts) to all motors
     * @param getMotorVelocities  list of suppliers returning each motor's current velocity
     * @param setPosition         consumer that commands the mechanism to a position
     * @param getPosition         supplier that returns the current mechanism position
     * @return this builder, for chaining
     */
    public TestModeBuilder withMultiPositionalStep(
            TestStepConfig stepConfig,
            DoubleConsumer setMotorVoltage,
            List<DoubleSupplier> getMotorVelocities,
            DoubleConsumer setPosition,
            DoubleSupplier getPosition) {
        steps.add(new TestModeMultiPositionalStep(stepConfig, setMotorVoltage, getMotorVelocities, setPosition, getPosition));
        return this;
    }

    /**
     * Adds an extra condition that must be true for the overall test to pass.
     * Conditions are evaluated when the test command runs and included in the report.
     *
     * <p>Example:
     * <pre>{@code
     * .withCondition("Left motor connected", leftMotor::isConnected)
     * .withCondition("Gyro calibrated", gyro::isCalibrated)
     * }</pre>
     *
     * @param title     human-readable label shown in the test report
     * @param condition supplier that returns {@code true} if the condition is met
     * @return this builder, for chaining
     */
    public TestModeBuilder withCondition(String title, BooleanSupplier condition) {
        conditionTitles.add(title);
        conditionChecks.add(condition);
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

    /**
     * Builds a command that runs every step in test mode, compares measured velocities against
     * saved baselines, evaluates any extra conditions added via {@link #withCondition}, and
     * delivers results to all registered consumers.
     *
     * <p>Steps run sequentially. Conditions are evaluated after all steps complete. The overall
     * pass/fail value passed to {@link #withOverallResultConsumer} is {@code true} only when
     * every step and every condition passes.
     *
     * @return command that runs the full test sequence and reports results
     */
    public Command buildTestCommand() {
        List<TestResult> results = new ArrayList<>();
        Consumer<TestResult> collector = results::add;

        Command[] stepCommands = steps.stream()
            .map(step -> step.runStep(collector))
            .toArray(Command[]::new);

        Command reportCommand = Commands.runOnce(() -> {
            for (int i = 0; i < conditionTitles.size(); i++) {
                boolean passed = conditionChecks.get(i).getAsBoolean();
                results.add(TestResult.forCondition(conditionTitles.get(i), passed));
            }
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
