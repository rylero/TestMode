# TestMode

A WPILib vendordep for structured FRC robot testing. Spin up motors, measure steady-state velocity, compare against a saved baseline, and get a pass/fail result — all from a `SendableChooser` on SmartDashboard.

**Vendordep URL:** `https://rylero.github.io/TestMode/TestMode.json`

---

## How It Works

TestMode runs in two phases:

1. **Baseline** — drive each mechanism and record its healthy velocity
2. **Test** — drive each mechanism again, compare to baseline, report pass/fail

Both are built from the same `TestModeBuilder` and selected via a `SendableChooser`.

---

## Quick Start

```java
private void configureTestMode() {
    SendableChooser<Command> testModeChooser = new SendableChooser<>();

    TestModeBuilder testModeBuilder = new TestModeBuilder()
        .withFlywheelStep(new TestStepConfig("flywheel_test"), motor::setVoltage, () -> motor.getVelocity().getValueAsDouble())
        .withTestResultConsumer(new TestModeReport())
        .withOverallResultConsumer((passed) -> System.out.println(passed ? "PASS" : "FAIL")); // You could replace printing with setting CANDLE LEDs to show test results on the robot itself

    testModeChooser.addOption("Test", testModeBuilder.buildTestCommand());
    testModeChooser.addOption("Baseline", testModeBuilder.buildBaselineCommand());

    SmartDashboard.putData(testModeChooser);
    testChooser = testModeChooser;
}
```

Call this from `robotInit()` or your container constructor, then select **Baseline** once on a healthy robot, and **Test** on subsequent matches to verify. Run the command returned by the sendable chooser on `testInit()` in the same way you run an autonomous command.

---

## Step Types

### Flywheel Step

For any spinning mechanism (flywheel, intake roller, etc.):

```java
.withFlywheelStep(
    new TestStepConfig("flywheel_test"),
    motor::setVoltage,           // DoubleConsumer — apply volts
    motor::getVelocityRPM        // DoubleSupplier — read velocity
)
```

### Positional Step

For mechanisms that need to move to a position before testing (arm, elevator, etc.):

```java
.withPositionalStep(
    new TestStepConfig("arm_test").withTargetPosition(45.0).withPositionTolerance(2.0),
    motor::setVoltage,
    motor::getVelocityRPM,
    arm::setPosition,
    arm::getPosition
)
```

### Multi-Flywheel Step

For subsystems with multiple motors sharing a single voltage (e.g., a shooter with top and bottom rollers). One result is reported per motor, named `stepName_0`, `stepName_1`, etc.:

```java
// varargs form
.withMultiFlywheelStep(
    new TestStepConfig("shooter"),
    shooter::setVoltage,
    topMotor::getVelocityRPM,
    bottomMotor::getVelocityRPM
)

// list form
.withMultiFlywheelStep(
    new TestStepConfig("shooter"),
    shooter::setVoltage,
    List.of(topMotor::getVelocityRPM, bottomMotor::getVelocityRPM)
)
```

### Multi-Positional Step

Same as multi-flywheel, but moves to a target position first. Position control is shared across all motors:

```java
.withMultiPositionalStep(
    new TestStepConfig("arm").withTargetPosition(90.0).withPositionTolerance(2.0),
    arm::setVoltage,
    List.of(leftMotor::getVelocityRPM, rightMotor::getVelocityRPM),
    arm::setPosition,
    arm::getPosition
)
```

### Extra Conditions

For pass/fail checks that aren't velocity-based (sensor connectivity, calibration state, etc.):

```java
.withCondition("Left motor connected", leftMotor::isConnected)
```

Conditions are evaluated at the end of the test run and included in the overall pass/fail result. If any condition returns `false`, the overall test fails. The HTML report shows conditions in a separate **Conditions** section above the motor step results.

Condition results have `isConditionCheck = true` in the `TestResult`; velocity fields are not meaningful for them.

---

## TestStepConfig

All parameters have defaults. Override with fluent setters:

| Parameter | Default | Description |
|---|---|---|
| `voltage` | 4 V | Voltage applied to the motor |
| `applyTime` | 3 s | Spin-up time before data collection |
| `dataTime` | 2 s | Duration to average velocity data |
| `tolerance` | 5% | Max deviation from baseline to pass |
| `targetPosition` | 0 | Position to reach before testing (positional only) |
| `positionTolerance` | 0.1 | Acceptable position error (positional only) |
| `moveTimeout` | 5 s | Max wait for position to be reached (positional only) |

```java
new TestStepConfig("my_step")
    .withVoltage(6.0)
    .withApplyTime(2.0)
    .withTolerance(0.10)  // 10%
```

---

## Result Consumers

### TestModeReport (built-in)

Generates an HTML report and serves it on port 5800. Also writes to `/home/lvuser/test-report.html` and `/U/test-report.html` if a USB drive is present.

```java
.withTestResultConsumer(new TestModeReport())
```

View the report at `http://10.TE.AM.2:5800` after running a test.

### Custom Consumer

```java
.withTestResultConsumer(results -> {
    for (TestResult r : results) {
        System.out.println(r.stepName + ": " + (r.passed ? "PASS" : "FAIL"));
    }
})
```

### Overall Pass/Fail

```java
.withOverallResultConsumer(passed -> leds.setColor(passed ? Color.kGreen : Color.kRed))
```

---

## Multiple Steps

Chain as many steps as needed:

```java
new TestModeBuilder()
    .withFlywheelStep(new TestStepConfig("left_flywheel"), leftMotor::setVoltage, leftMotor::getVelocityRPM)
    .withFlywheelStep(new TestStepConfig("right_flywheel"), rightMotor::setVoltage, rightMotor::getVelocityRPM)
    .withPositionalStep(new TestStepConfig("arm").withTargetPosition(90), ...)
    .withTestResultConsumer(new TestModeReport())
    .withOverallResultConsumer(passed -> System.out.println(passed ? "PASS" : "FAIL"))
```

Steps run sequentially. All results are collected and passed to consumers at the end. Multi-motor steps report one result per motor, so a two-motor `withMultiFlywheelStep` adds two entries to the results list.
