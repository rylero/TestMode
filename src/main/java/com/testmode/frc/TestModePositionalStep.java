package com.testmode.frc;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

import edu.wpi.first.wpilibj.Preferences;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;

/**
 * A test step that moves a mechanism to a target position before measuring steady-state velocity.
 *
 * <p>The sequence is:
 * <ol>
 *   <li>Command the mechanism to {@code targetPosition}.</li>
 *   <li>Wait until position is within {@code positionTolerance}, up to {@code moveTimeout} seconds.</li>
 *   <li>Apply {@code stepConfig.voltage} and warm up for {@code stepConfig.applyTime - stepConfig.dataTime} seconds.</li>
 *   <li>Collect velocity samples for {@code stepConfig.dataTime} seconds.</li>
 *   <li>Stop the motor, compare against baseline, and report a {@link TestResult}.</li>
 * </ol>
 */
public class TestModePositionalStep implements TestModeStep {
    private static final String kBaselineVelocityKeySuffix = "_baselineVelocity";

    private final TestStepConfig stepConfig;
    private final DoubleConsumer setMotorVoltage;
    private final DoubleSupplier getMotorVelocity;
    private final DoubleConsumer setPosition;
    private final DoubleSupplier getPosition;

    /**
     * Creates a positional test step. Target position, position tolerance, and move timeout
     * are read from {@code stepConfig}.
     *
     * @param stepConfig        configuration for voltage, timing, tolerance, position, and name
     * @param setMotorVoltage   consumer that applies a voltage (volts) to the motor
     * @param getMotorVelocity  supplier that returns the current motor velocity
     * @param setPosition       consumer that commands the mechanism to a position
     * @param getPosition       supplier that returns the current mechanism position
     */
    public TestModePositionalStep(
            TestStepConfig stepConfig,
            DoubleConsumer setMotorVoltage,
            DoubleSupplier getMotorVelocity,
            DoubleConsumer setPosition,
            DoubleSupplier getPosition) {
        this.stepConfig = stepConfig;
        this.setMotorVoltage = setMotorVoltage;
        this.getMotorVelocity = getMotorVelocity;
        this.setPosition = setPosition;
        this.getPosition = getPosition;
    }

    @Override
    public Command runStep(Consumer<TestResult> resultConsumer) {
        AtomicReference<Double> velocitySum = new AtomicReference<>(0.0);
        AtomicInteger sampleCount = new AtomicInteger(0);
        java.util.concurrent.atomic.AtomicBoolean reachedPosition = new java.util.concurrent.atomic.AtomicBoolean(false);

        return Commands.sequence(
            Commands.runOnce(() -> {
                velocitySum.set(0.0);
                sampleCount.set(0);
                reachedPosition.set(false);
                setPosition.accept(stepConfig.targetPosition);
            }),
            Commands.waitUntil(() -> {
                boolean reached = Math.abs(getPosition.getAsDouble() - stepConfig.targetPosition) <= stepConfig.positionTolerance;
                if (reached) reachedPosition.set(true);
                return reached;
            }).withTimeout(stepConfig.moveTimeout),
            Commands.runOnce(() -> setMotorVoltage.accept(stepConfig.voltage)),
            Commands.waitSeconds(stepConfig.applyTime - stepConfig.dataTime),
            Commands.run(() -> {
                velocitySum.updateAndGet(v -> v + getMotorVelocity.getAsDouble());
                sampleCount.incrementAndGet();
            }).withTimeout(stepConfig.dataTime),
            Commands.runOnce(() -> {
                setMotorVoltage.accept(0);
                int count = sampleCount.get();
                double averageVelocity = count > 0 ? velocitySum.get() / count : 0;
                String prefKey = stepConfig.stepName + kBaselineVelocityKeySuffix;
                Preferences.initDouble(prefKey, averageVelocity);
                double baselineVelocity = Preferences.getDouble(prefKey, averageVelocity);
                boolean passes = Math.abs(averageVelocity - baselineVelocity) <= stepConfig.tolerance * averageVelocity;
                resultConsumer.accept(new TestResult(stepConfig.stepName, passes, averageVelocity, baselineVelocity, true, reachedPosition.get()));
            })
        );
    }
}
