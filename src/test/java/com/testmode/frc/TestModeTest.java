package com.testmode.frc;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleSupplier;

import org.junit.jupiter.api.*;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.wpilibj.Preferences;
import edu.wpi.first.wpilibj.simulation.DriverStationSim;
import edu.wpi.first.wpilibj.simulation.SimHooks;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestModeTest {

    @BeforeAll
    static void setupHAL() {
        HAL.initialize(500, 0);
        SimHooks.pauseTiming();
        DriverStationSim.setEnabled(true);
        DriverStationSim.setAutonomous(false);
        DriverStationSim.setTest(true);
        DriverStationSim.notifyNewData();
    }

    @AfterAll
    static void teardownHAL() {
        SimHooks.resumeTiming();
    }

    @AfterEach
    void teardown() {
        CommandScheduler.getInstance().cancelAll();
        CommandScheduler.getInstance().unregisterAllSubsystems();
    }

    private void runCommandToCompletion(Command command, double maxSimSeconds) {
        CommandScheduler.getInstance().schedule(command);
        double elapsed = 0;
        while (CommandScheduler.getInstance().isScheduled(command) && elapsed < maxSimSeconds) {
            SimHooks.stepTiming(0.02);
            CommandScheduler.getInstance().run();
            elapsed += 0.02;
        }
    }

    // ---- TestResult ----

    @Test
    void testResultMotorStepFields() {
        TestResult r = new TestResult("motor1", true, 100.0, 98.0, false, true);
        assertEquals("motor1", r.stepName);
        assertTrue(r.passed);
        assertEquals(100.0, r.averageVelocity);
        assertEquals(98.0, r.baselineVelocity);
        assertFalse(r.isPositionalTest);
        assertTrue(r.reachedStartPosition);
        assertFalse(r.isConditionCheck);
    }

    @Test
    void testResultPositionalStepFields() {
        TestResult r = new TestResult("arm", false, 50.0, 100.0, true, false);
        assertTrue(r.isPositionalTest);
        assertFalse(r.reachedStartPosition);
        assertFalse(r.passed);
    }

    @Test
    void testResultForConditionPass() {
        TestResult r = TestResult.forCondition("Motor OK", true);
        assertEquals("Motor OK", r.stepName);
        assertTrue(r.passed);
        assertTrue(r.isConditionCheck);
        assertFalse(r.isPositionalTest);
        assertTrue(r.reachedStartPosition);
        assertEquals(0.0, r.averageVelocity);
        assertEquals(0.0, r.baselineVelocity);
    }

    @Test
    void testResultForConditionFail() {
        TestResult r = TestResult.forCondition("Gyro calibrated", false);
        assertFalse(r.passed);
        assertTrue(r.isConditionCheck);
    }

    // ---- TestStepConfig ----

    @Test
    void testStepConfigDefaults() {
        TestStepConfig config = new TestStepConfig("step1");
        assertEquals("step1", config.stepName);
        assertEquals(4.0, config.voltage);
        assertEquals(3.0, config.applyTime);
        assertEquals(2.0, config.dataTime);
        assertEquals(0.05, config.tolerance);
        assertEquals(0.0, config.targetPosition);
        assertEquals(0.1, config.positionTolerance);
        assertEquals(5.0, config.moveTimeout);
    }

    @Test
    void testStepConfigWithVoltage() {
        assertEquals(8.0, new TestStepConfig("s").withVoltage(8.0).voltage);
    }

    @Test
    void testStepConfigWithApplyTime() {
        assertEquals(5.0, new TestStepConfig("s").withApplyTime(5.0).applyTime);
    }

    @Test
    void testStepConfigWithDataTime() {
        assertEquals(1.5, new TestStepConfig("s").withDataTime(1.5).dataTime);
    }

    @Test
    void testStepConfigWithTolerance() {
        assertEquals(0.1, new TestStepConfig("s").withTolerance(0.1).tolerance, 1e-9);
    }

    @Test
    void testStepConfigWithTargetPosition() {
        assertEquals(45.0, new TestStepConfig("s").withTargetPosition(45.0).targetPosition);
    }

    @Test
    void testStepConfigWithPositionTolerance() {
        assertEquals(0.5, new TestStepConfig("s").withPositionTolerance(0.5).positionTolerance, 1e-9);
    }

    @Test
    void testStepConfigWithMoveTimeout() {
        assertEquals(10.0, new TestStepConfig("s").withMoveTimeout(10.0).moveTimeout);
    }

    @Test
    void testStepConfigChaining() {
        TestStepConfig config = new TestStepConfig("arm")
            .withVoltage(6.0)
            .withApplyTime(4.0)
            .withDataTime(2.5)
            .withTolerance(0.08)
            .withTargetPosition(90.0)
            .withPositionTolerance(0.3)
            .withMoveTimeout(8.0);
        assertEquals(6.0, config.voltage);
        assertEquals(4.0, config.applyTime);
        assertEquals(2.5, config.dataTime);
        assertEquals(0.08, config.tolerance, 1e-9);
        assertEquals(90.0, config.targetPosition);
        assertEquals(0.3, config.positionTolerance, 1e-9);
        assertEquals(8.0, config.moveTimeout);
    }

    // ---- TestMode ----

    private void resetTestModeState() throws Exception {
        Field f = TestMode.class.getDeclaredField("initialized");
        f.setAccessible(true);
        f.set(null, false);
    }

    @Test
    @Order(1)
    void testModeNotInitializedByDefault() throws Exception {
        resetTestModeState();
        assertFalse(TestMode.isInitialized());
    }

    @Test
    @Order(2)
    void testModeInitializeSetFlag() throws Exception {
        resetTestModeState();
        TestMode.initialize();
        assertTrue(TestMode.isInitialized());
    }

    @Test
    @Order(3)
    void testModeInitializeIdempotent() throws Exception {
        resetTestModeState();
        TestMode.initialize();
        TestMode.initialize();
        assertTrue(TestMode.isInitialized());
    }

    // ---- TestModeReport (buildHtml via reflection) ----

    private String buildHtml(List<TestResult> results) throws Exception {
        Method m = TestModeReport.class.getDeclaredMethod("buildHtml", List.class);
        m.setAccessible(true);
        return (String) m.invoke(null, results);
    }

    @Test
    void testReportHtmlStructure() throws Exception {
        String html = buildHtml(List.of());
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("Test Mode Report"));
    }

    @Test
    void testReportAllPassOverallStatus() throws Exception {
        List<TestResult> results = List.of(
            new TestResult("m1", true, 100.0, 100.0, false, true)
        );
        String html = buildHtml(results);
        assertTrue(html.contains("badge-overall\">PASS"));
    }

    @Test
    void testReportAnyFailOverallStatus() throws Exception {
        List<TestResult> results = List.of(
            new TestResult("m1", false, 50.0, 100.0, false, true)
        );
        String html = buildHtml(results);
        assertTrue(html.contains("badge-overall\">FAIL"));
    }

    @Test
    void testReportMotorStepShown() throws Exception {
        List<TestResult> results = List.of(
            new TestResult("shooter", true, 100.0, 100.0, false, true)
        );
        String html = buildHtml(results);
        assertTrue(html.contains("shooter"));
        assertTrue(html.contains("Motor Steps"));
    }

    @Test
    void testReportConditionShown() throws Exception {
        List<TestResult> results = List.of(
            TestResult.forCondition("Motor connected", true)
        );
        String html = buildHtml(results);
        assertTrue(html.contains("Motor connected"));
        assertTrue(html.contains("Conditions"));
    }

    @Test
    void testReportHtmlEscaping() throws Exception {
        List<TestResult> results = List.of(
            new TestResult("<script>alert(1)</script>", true, 0, 0, false, true)
        );
        String html = buildHtml(results);
        assertFalse(html.contains("<script>alert(1)</script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void testReportNullStepNameEscaping() throws Exception {
        List<TestResult> results = new ArrayList<>();
        TestResult r = new TestResult(null, true, 0, 0, false, true);
        results.add(r);
        assertDoesNotThrow(() -> buildHtml(results));
    }

    @Test
    void testReportEmptyResults() throws Exception {
        String html = buildHtml(List.of());
        assertTrue(html.contains("PASS")); // no failures → overall PASS
    }

    // ---- TestModeFlywheelStep ----

    @Test
    void testFlywheelStepRunStepPassesWithinTolerance() {
        Preferences.setDouble("fw1_baselineVelocity", 100.0);
        TestStepConfig config = new TestStepConfig("fw1")
            .withApplyTime(0.1).withDataTime(0.06).withTolerance(0.05);

        List<TestResult> results = new ArrayList<>();
        Command cmd = new TestModeFlywheelStep(config, v -> {}, () -> 100.0).runStep(results::add);
        runCommandToCompletion(cmd, 5.0);

        assertEquals(1, results.size());
        assertTrue(results.get(0).passed);
        assertEquals("fw1", results.get(0).stepName);
        assertFalse(results.get(0).isPositionalTest);
        assertTrue(results.get(0).reachedStartPosition);
    }

    @Test
    void testFlywheelStepRunStepFailsOutsideTolerance() {
        Preferences.setDouble("fw2_baselineVelocity", 100.0);
        TestStepConfig config = new TestStepConfig("fw2")
            .withApplyTime(0.1).withDataTime(0.06).withTolerance(0.05);

        List<TestResult> results = new ArrayList<>();
        Command cmd = new TestModeFlywheelStep(config, v -> {}, () -> 50.0).runStep(results::add);
        runCommandToCompletion(cmd, 5.0);

        assertEquals(1, results.size());
        assertFalse(results.get(0).passed);
    }

    @Test
    void testFlywheelStepAppliesVoltage() {
        Preferences.setDouble("fw3_baselineVelocity", 100.0);
        double[] appliedVoltage = {0};
        TestStepConfig config = new TestStepConfig("fw3")
            .withVoltage(6.0).withApplyTime(0.1).withDataTime(0.06);

        Command cmd = new TestModeFlywheelStep(config, v -> appliedVoltage[0] = v, () -> 100.0)
            .runStep(r -> {});
        runCommandToCompletion(cmd, 5.0);

        assertEquals(0.0, appliedVoltage[0], 0.001); // motor stopped at end
    }

    @Test
    void testFlywheelStepRecordBaseline() {
        TestStepConfig config = new TestStepConfig("fw_baseline")
            .withApplyTime(0.1).withDataTime(0.06);

        Command cmd = new TestModeFlywheelStep(config, v -> {}, () -> 200.0).recordBaseline();
        runCommandToCompletion(cmd, 5.0);

        assertEquals(200.0, Preferences.getDouble("fw_baseline_baselineVelocity", -1), 1.0);
    }

    // ---- TestModeMultiFlywheelStep ----

    @Test
    void testMultiFlywheelStepRunStepAllPass() {
        Preferences.setDouble("mfw1_0_baselineVelocity", 100.0);
        Preferences.setDouble("mfw1_1_baselineVelocity", 200.0);
        TestStepConfig config = new TestStepConfig("mfw1")
            .withApplyTime(0.1).withDataTime(0.06).withTolerance(0.05);

        List<TestResult> results = new ArrayList<>();
        List<DoubleSupplier> velocities = List.of(() -> 100.0, () -> 200.0);
        Command cmd = new TestModeMultiFlywheelStep(config, v -> {}, velocities).runStep(results::add);
        runCommandToCompletion(cmd, 5.0);

        assertEquals(2, results.size());
        assertEquals("mfw1_0", results.get(0).stepName);
        assertEquals("mfw1_1", results.get(1).stepName);
        assertTrue(results.get(0).passed);
        assertTrue(results.get(1).passed);
    }

    @Test
    void testMultiFlywheelStepRunStepOneFails() {
        Preferences.setDouble("mfw2_0_baselineVelocity", 100.0);
        Preferences.setDouble("mfw2_1_baselineVelocity", 200.0);
        TestStepConfig config = new TestStepConfig("mfw2")
            .withApplyTime(0.1).withDataTime(0.06).withTolerance(0.05);

        List<TestResult> results = new ArrayList<>();
        List<DoubleSupplier> velocities = List.of(() -> 100.0, () -> 50.0); // second motor fails
        Command cmd = new TestModeMultiFlywheelStep(config, v -> {}, velocities).runStep(results::add);
        runCommandToCompletion(cmd, 5.0);

        assertEquals(2, results.size());
        assertTrue(results.get(0).passed);
        assertFalse(results.get(1).passed);
    }

    @Test
    void testMultiFlywheelRecordBaseline() {
        TestStepConfig config = new TestStepConfig("mfw_bl")
            .withApplyTime(0.1).withDataTime(0.06);
        List<DoubleSupplier> velocities = List.of(() -> 150.0, () -> 250.0);
        Command cmd = new TestModeMultiFlywheelStep(config, v -> {}, velocities).recordBaseline();
        runCommandToCompletion(cmd, 5.0);

        assertEquals(150.0, Preferences.getDouble("mfw_bl_0_baselineVelocity", -1), 1.0);
        assertEquals(250.0, Preferences.getDouble("mfw_bl_1_baselineVelocity", -1), 1.0);
    }

    // ---- TestModePositionalStep ----

    @Test
    void testPositionalStepReachesPositionAndPasses() {
        Preferences.setDouble("ps1_baselineVelocity", 100.0);
        TestStepConfig config = new TestStepConfig("ps1")
            .withApplyTime(0.1).withDataTime(0.06).withTolerance(0.05)
            .withTargetPosition(90.0).withPositionTolerance(1.0).withMoveTimeout(3.0);

        List<TestResult> results = new ArrayList<>();
        Command cmd = new TestModePositionalStep(
            config, v -> {}, () -> 100.0, pos -> {}, () -> 90.0
        ).runStep(results::add);
        runCommandToCompletion(cmd, 10.0);

        assertEquals(1, results.size());
        assertTrue(results.get(0).isPositionalTest);
        assertTrue(results.get(0).reachedStartPosition);
        assertTrue(results.get(0).passed);
    }

    @Test
    void testPositionalStepTimesOutSetsReachedFalse() {
        Preferences.setDouble("ps2_baselineVelocity", 100.0);
        TestStepConfig config = new TestStepConfig("ps2")
            .withApplyTime(0.1).withDataTime(0.06).withTolerance(0.05)
            .withTargetPosition(90.0).withPositionTolerance(0.1).withMoveTimeout(0.04);

        List<TestResult> results = new ArrayList<>();
        Command cmd = new TestModePositionalStep(
            config, v -> {}, () -> 100.0, pos -> {}, () -> 0.0 // never reaches 90
        ).runStep(results::add);
        runCommandToCompletion(cmd, 5.0);

        assertEquals(1, results.size());
        assertTrue(results.get(0).isPositionalTest);
        assertFalse(results.get(0).reachedStartPosition);
    }

    @Test
    void testPositionalStepRecordBaseline() {
        TestStepConfig config = new TestStepConfig("ps_bl")
            .withApplyTime(0.1).withDataTime(0.06)
            .withTargetPosition(45.0).withPositionTolerance(1.0).withMoveTimeout(3.0);

        Command cmd = new TestModePositionalStep(
            config, v -> {}, () -> 150.0, pos -> {}, () -> 45.0
        ).recordBaseline();
        runCommandToCompletion(cmd, 5.0);

        assertEquals(150.0, Preferences.getDouble("ps_bl_baselineVelocity", -1), 1.0);
    }

    // ---- TestModeMultiPositionalStep ----

    @Test
    void testMultiPositionalStepAllPass() {
        Preferences.setDouble("mps1_0_baselineVelocity", 100.0);
        Preferences.setDouble("mps1_1_baselineVelocity", 200.0);
        TestStepConfig config = new TestStepConfig("mps1")
            .withApplyTime(0.1).withDataTime(0.06).withTolerance(0.05)
            .withTargetPosition(30.0).withPositionTolerance(1.0).withMoveTimeout(3.0);

        List<TestResult> results = new ArrayList<>();
        List<DoubleSupplier> velocities = List.of(() -> 100.0, () -> 200.0);
        Command cmd = new TestModeMultiPositionalStep(
            config, v -> {}, velocities, pos -> {}, () -> 30.0
        ).runStep(results::add);
        runCommandToCompletion(cmd, 10.0);

        assertEquals(2, results.size());
        assertEquals("mps1_0", results.get(0).stepName);
        assertEquals("mps1_1", results.get(1).stepName);
        assertTrue(results.get(0).isPositionalTest);
        assertTrue(results.get(0).reachedStartPosition);
        assertTrue(results.get(0).passed);
        assertTrue(results.get(1).passed);
    }

    @Test
    void testMultiPositionalStepTimesOut() {
        Preferences.setDouble("mps2_0_baselineVelocity", 100.0);
        Preferences.setDouble("mps2_1_baselineVelocity", 200.0);
        TestStepConfig config = new TestStepConfig("mps2")
            .withApplyTime(0.1).withDataTime(0.06).withTolerance(0.05)
            .withTargetPosition(90.0).withPositionTolerance(0.1).withMoveTimeout(0.04);

        List<TestResult> results = new ArrayList<>();
        List<DoubleSupplier> velocities = List.of(() -> 100.0, () -> 200.0);
        Command cmd = new TestModeMultiPositionalStep(
            config, v -> {}, velocities, pos -> {}, () -> 0.0
        ).runStep(results::add);
        runCommandToCompletion(cmd, 5.0);

        assertEquals(2, results.size());
        assertFalse(results.get(0).reachedStartPosition);
        assertFalse(results.get(1).reachedStartPosition);
    }

    @Test
    void testMultiPositionalRecordBaseline() {
        TestStepConfig config = new TestStepConfig("mps_bl")
            .withApplyTime(0.1).withDataTime(0.06)
            .withTargetPosition(30.0).withPositionTolerance(1.0).withMoveTimeout(3.0);

        List<DoubleSupplier> velocities = List.of(() -> 120.0, () -> 240.0);
        Command cmd = new TestModeMultiPositionalStep(
            config, v -> {}, velocities, pos -> {}, () -> 30.0
        ).recordBaseline();
        runCommandToCompletion(cmd, 5.0);

        assertEquals(120.0, Preferences.getDouble("mps_bl_0_baselineVelocity", -1), 1.0);
        assertEquals(240.0, Preferences.getDouble("mps_bl_1_baselineVelocity", -1), 1.0);
    }

    // ---- TestModeBuilder ----

    @Test
    void testBuilderConditionsAllPass() {
        List<TestResult> results = new ArrayList<>();
        AtomicReference<Boolean> overall = new AtomicReference<>();

        Command cmd = new TestModeBuilder()
            .withCondition("A", () -> true)
            .withCondition("B", () -> true)
            .withTestResultConsumer(results::addAll)
            .withOverallResultConsumer(overall::set)
            .buildTestCommand();
        runCommandToCompletion(cmd, 5.0);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> r.passed));
        assertTrue(overall.get());
    }

    @Test
    void testBuilderConditionFails() {
        List<TestResult> results = new ArrayList<>();
        AtomicReference<Boolean> overall = new AtomicReference<>();

        Command cmd = new TestModeBuilder()
            .withCondition("Passes", () -> true)
            .withCondition("Fails", () -> false)
            .withTestResultConsumer(results::addAll)
            .withOverallResultConsumer(overall::set)
            .buildTestCommand();
        runCommandToCompletion(cmd, 5.0);

        assertFalse(overall.get());
        assertTrue(results.stream().filter(r -> r.stepName.equals("Fails")).findFirst().orElseThrow().isConditionCheck);
    }

    @Test
    void testBuilderMultipleConsumersCalledInOrder() {
        List<TestResult> consumer1 = new ArrayList<>();
        List<TestResult> consumer2 = new ArrayList<>();

        Command cmd = new TestModeBuilder()
            .withCondition("x", () -> true)
            .withTestResultConsumer(consumer1::addAll)
            .withTestResultConsumer(consumer2::addAll)
            .buildTestCommand();
        runCommandToCompletion(cmd, 5.0);

        assertEquals(1, consumer1.size());
        assertEquals(1, consumer2.size());
    }

    @Test
    void testBuilderNoOverallConsumerDoesNotThrow() {
        Command cmd = new TestModeBuilder()
            .withCondition("x", () -> true)
            .buildTestCommand();
        assertDoesNotThrow(() -> runCommandToCompletion(cmd, 5.0));
    }

    @Test
    void testBuilderBaselineCommandNoSteps() {
        Command cmd = new TestModeBuilder().buildBaselineCommand();
        assertNotNull(cmd);
        assertDoesNotThrow(() -> runCommandToCompletion(cmd, 1.0));
    }

    @Test
    void testBuilderWithFlywheelStep() {
        Preferences.setDouble("bldr_fw_baselineVelocity", 100.0);
        TestStepConfig config = new TestStepConfig("bldr_fw")
            .withApplyTime(0.1).withDataTime(0.06).withTolerance(0.05);

        List<TestResult> results = new ArrayList<>();
        Command cmd = new TestModeBuilder()
            .withFlywheelStep(config, v -> {}, () -> 100.0)
            .withTestResultConsumer(results::addAll)
            .buildTestCommand();
        runCommandToCompletion(cmd, 5.0);

        assertEquals(1, results.size());
        assertEquals("bldr_fw", results.get(0).stepName);
        assertTrue(results.get(0).passed);
    }

    @Test
    void testBuilderWithMultiFlywheelStepVarargs() {
        Preferences.setDouble("bldr_mfw_0_baselineVelocity", 100.0);
        Preferences.setDouble("bldr_mfw_1_baselineVelocity", 200.0);
        TestStepConfig config = new TestStepConfig("bldr_mfw")
            .withApplyTime(0.1).withDataTime(0.06).withTolerance(0.05);

        List<TestResult> results = new ArrayList<>();
        Command cmd = new TestModeBuilder()
            .withMultiFlywheelStep(config, v -> {}, () -> 100.0, () -> 200.0)
            .withTestResultConsumer(results::addAll)
            .buildTestCommand();
        runCommandToCompletion(cmd, 5.0);

        assertEquals(2, results.size());
        assertTrue(results.get(0).passed);
        assertTrue(results.get(1).passed);
    }

    @Test
    void testBuilderWithPositionalStep() {
        Preferences.setDouble("bldr_ps_baselineVelocity", 100.0);
        TestStepConfig config = new TestStepConfig("bldr_ps")
            .withApplyTime(0.1).withDataTime(0.06).withTolerance(0.05)
            .withTargetPosition(45.0).withPositionTolerance(1.0).withMoveTimeout(3.0);

        List<TestResult> results = new ArrayList<>();
        Command cmd = new TestModeBuilder()
            .withPositionalStep(config, v -> {}, () -> 100.0, pos -> {}, () -> 45.0)
            .withTestResultConsumer(results::addAll)
            .buildTestCommand();
        runCommandToCompletion(cmd, 10.0);

        assertEquals(1, results.size());
        assertTrue(results.get(0).isPositionalTest);
        assertTrue(results.get(0).passed);
    }

    @Test
    void testBuilderWithMultiPositionalStep() {
        Preferences.setDouble("bldr_mps_0_baselineVelocity", 100.0);
        Preferences.setDouble("bldr_mps_1_baselineVelocity", 200.0);
        TestStepConfig config = new TestStepConfig("bldr_mps")
            .withApplyTime(0.1).withDataTime(0.06).withTolerance(0.05)
            .withTargetPosition(30.0).withPositionTolerance(1.0).withMoveTimeout(3.0);

        List<TestResult> results = new ArrayList<>();
        List<DoubleSupplier> velocities = List.of(() -> 100.0, () -> 200.0);
        Command cmd = new TestModeBuilder()
            .withMultiPositionalStep(config, v -> {}, velocities, pos -> {}, () -> 30.0)
            .withTestResultConsumer(results::addAll)
            .buildTestCommand();
        runCommandToCompletion(cmd, 10.0);

        assertEquals(2, results.size());
        assertTrue(results.get(0).isPositionalTest);
        assertTrue(results.get(0).passed);
        assertTrue(results.get(1).passed);
    }

    @Test
    void testBuilderBaselineCommandWithStep() {
        TestStepConfig config = new TestStepConfig("bldr_bl")
            .withApplyTime(0.1).withDataTime(0.06);

        Command cmd = new TestModeBuilder()
            .withFlywheelStep(config, v -> {}, () -> 100.0)
            .buildBaselineCommand();
        assertDoesNotThrow(() -> runCommandToCompletion(cmd, 5.0));

        assertEquals(100.0, Preferences.getDouble("bldr_bl_baselineVelocity", -1), 1.0);
    }

    @Test
    void testBuilderStepsAndConditionsCombined() {
        Preferences.setDouble("bldr_combo_baselineVelocity", 100.0);
        TestStepConfig config = new TestStepConfig("bldr_combo")
            .withApplyTime(0.1).withDataTime(0.06).withTolerance(0.05);

        List<TestResult> results = new ArrayList<>();
        AtomicReference<Boolean> overall = new AtomicReference<>();
        Command cmd = new TestModeBuilder()
            .withFlywheelStep(config, v -> {}, () -> 100.0)
            .withCondition("Sensor OK", () -> true)
            .withTestResultConsumer(results::addAll)
            .withOverallResultConsumer(overall::set)
            .buildTestCommand();
        runCommandToCompletion(cmd, 5.0);

        assertEquals(2, results.size()); // 1 step + 1 condition
        assertTrue(overall.get());
        assertTrue(results.stream().anyMatch(r -> r.isConditionCheck));
        assertTrue(results.stream().anyMatch(r -> !r.isConditionCheck));
    }
}
