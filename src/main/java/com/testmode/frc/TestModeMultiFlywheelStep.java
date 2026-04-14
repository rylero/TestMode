package com.testmode.frc;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

import edu.wpi.first.wpilibj.Preferences;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;

/**
 * A test step that applies a single voltage to a multi-motor subsystem and measures
 * the steady-state velocity of each motor independently.
 *
 * <p>Baselines and results are stored per-motor using the step name suffixed with
 * {@code _<index>} (e.g., {@code "shooter_0"}, {@code "shooter_1"}).
 */
public class TestModeMultiFlywheelStep implements TestModeStep {
    private static final String kBaselineVelocityKeySuffix = "_baselineVelocity";

    private final DoubleConsumer setMotorVoltage;
    private final List<DoubleSupplier> getMotorVelocities;
    private final TestStepConfig stepConfig;

    /**
     * Creates a multi-flywheel test step.
     *
     * @param stepConfig         configuration for the step (voltage, timing, tolerance, name)
     * @param setMotorVoltage    consumer that applies a voltage (in volts) to all motors
     * @param getMotorVelocities list of suppliers returning each motor's current velocity
     */
    public TestModeMultiFlywheelStep(TestStepConfig stepConfig, DoubleConsumer setMotorVoltage, List<DoubleSupplier> getMotorVelocities) {
        this.stepConfig = stepConfig;
        this.setMotorVoltage = setMotorVoltage;
        this.getMotorVelocities = getMotorVelocities;
    }

    @Override
    public Command recordBaseline() {
        int motorCount = getMotorVelocities.size();
        @SuppressWarnings("unchecked")
        AtomicReference<Double>[] velocitySums = new AtomicReference[motorCount];
        AtomicInteger[] sampleCounts = new AtomicInteger[motorCount];
        for (int i = 0; i < motorCount; i++) {
            velocitySums[i] = new AtomicReference<>(0.0);
            sampleCounts[i] = new AtomicInteger(0);
        }

        return Commands.sequence(
            Commands.runOnce(() -> {
                for (int i = 0; i < motorCount; i++) {
                    velocitySums[i].set(0.0);
                    sampleCounts[i].set(0);
                }
                setMotorVoltage.accept(stepConfig.voltage);
            }),
            Commands.waitSeconds(stepConfig.applyTime - stepConfig.dataTime),
            Commands.run(() -> {
                for (int i = 0; i < motorCount; i++) {
                    final int idx = i;
                    velocitySums[idx].updateAndGet(v -> v + getMotorVelocities.get(idx).getAsDouble());
                    sampleCounts[i].incrementAndGet();
                }
            }).withTimeout(stepConfig.dataTime),
            Commands.runOnce(() -> {
                setMotorVoltage.accept(0);
                for (int i = 0; i < motorCount; i++) {
                    int count = sampleCounts[i].get();
                    double averageVelocity = count > 0 ? velocitySums[i].get() / count : 0;
                    Preferences.setDouble(stepConfig.stepName + "_" + i + kBaselineVelocityKeySuffix, averageVelocity);
                }
            })
        );
    }

    @Override
    public Command runStep(Consumer<TestResult> resultConsumer) {
        int motorCount = getMotorVelocities.size();
        @SuppressWarnings("unchecked")
        AtomicReference<Double>[] velocitySums = new AtomicReference[motorCount];
        AtomicInteger[] sampleCounts = new AtomicInteger[motorCount];
        for (int i = 0; i < motorCount; i++) {
            velocitySums[i] = new AtomicReference<>(0.0);
            sampleCounts[i] = new AtomicInteger(0);
        }

        return Commands.sequence(
            Commands.runOnce(() -> {
                for (int i = 0; i < motorCount; i++) {
                    velocitySums[i].set(0.0);
                    sampleCounts[i].set(0);
                }
                setMotorVoltage.accept(stepConfig.voltage);
            }),
            Commands.waitSeconds(stepConfig.applyTime - stepConfig.dataTime),
            Commands.run(() -> {
                for (int i = 0; i < motorCount; i++) {
                    final int idx = i;
                    velocitySums[idx].updateAndGet(v -> v + getMotorVelocities.get(idx).getAsDouble());
                    sampleCounts[i].incrementAndGet();
                }
            }).withTimeout(stepConfig.dataTime),
            Commands.runOnce(() -> {
                setMotorVoltage.accept(0);
                for (int i = 0; i < motorCount; i++) {
                    int count = sampleCounts[i].get();
                    double averageVelocity = count > 0 ? velocitySums[i].get() / count : 0;
                    String motorName = stepConfig.stepName + "_" + i;
                    String prefKey = motorName + kBaselineVelocityKeySuffix;
                    Preferences.initDouble(prefKey, Double.POSITIVE_INFINITY);
                    double baselineVelocity = Preferences.getDouble(prefKey, Double.POSITIVE_INFINITY);
                    boolean passes = Math.abs(averageVelocity - baselineVelocity) <= stepConfig.tolerance * averageVelocity;
                    resultConsumer.accept(new TestResult(motorName, passes, averageVelocity, baselineVelocity, false, true));
                }
            })
        );
    }
}
