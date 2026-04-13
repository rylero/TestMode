package com.testmode.frc;

import java.util.function.Consumer;

import edu.wpi.first.wpilibj2.command.Command;

/**
 * A single step in a robot test-mode sequence.
 *
 * <p>Implementations apply a stimulus (e.g., a fixed voltage) and report a {@link TestResult}
 * when the step completes.
 */
public interface TestModeStep {
    /**
     * Returns a {@link Command} that executes this test step.
     *
     * <p>The command should call {@code resultConsumer} exactly once when it finishes,
     * passing the measured {@link TestResult}.
     *
     * @param resultConsumer callback that receives the result when the step ends
     * @return command representing the step execution
     */
    public Command runStep(Consumer<TestResult> resultConsumer);

    /**
     * Returns a {@link Command} that runs this step and records the measured velocity as
     * the new baseline, overwriting any existing value. Does not produce a {@link TestResult}.
     *
     * @return command representing the baseline recording
     */
    public Command recordBaseline();
}