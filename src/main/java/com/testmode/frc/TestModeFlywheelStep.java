package com.testmode.frc;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

import edu.wpi.first.wpilibj.Preferences;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;

public class TestModeFlywheelStep implements TestModeStep {
    private static final String kBaselineVelocityKeySuffix = "_baselineVelocity";

    private DoubleConsumer setMotorVoltage;
    private DoubleSupplier getMotorVelocity;
    private TestStepConfig stepConfig;

    public TestModeFlywheelStep(TestStepConfig stepConfig, DoubleConsumer setMotorVoltage, DoubleSupplier getMotorVelocity) {
        this.setMotorVoltage = setMotorVoltage;
        this.getMotorVelocity = getMotorVelocity;
        this.stepConfig = stepConfig;
    }

    @Override
    public Command runStep(Consumer<TestResult> resultConsumer) {
        AtomicReference<Double> velocitySum = new AtomicReference<>(0.0);
        AtomicInteger sampleCount = new AtomicInteger(0);

        return Commands.sequence(
            Commands.runOnce(() -> {
                velocitySum.set(0.0);
                sampleCount.set(0);
                setMotorVoltage.accept(stepConfig.voltage);
            }),
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
                resultConsumer.accept(new TestResult(passes, averageVelocity, baselineVelocity));
            })
        );
    }
}
