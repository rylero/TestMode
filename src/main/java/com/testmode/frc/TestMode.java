package com.testmode.frc;

import com.testmode.jni.TestModeJNI;

/**
 * Example TestMode vendordep class.
 *
 * <p>This class demonstrates how a robot project would interact with the TestMode library.
 * Call {@link #initialize()} once during robot initialization to load the native driver.
 */
public class TestMode {
  private static boolean initialized = false;

  /**
   * Initializes the TestMode library. Must be called before any other TestMode methods.
   * Safe to call multiple times; subsequent calls are no-ops.
   */
  public static void initialize() {
    if (initialized) {
      return;
    }
    TestModeJNI.forceLoad();
    int result = TestModeJNI.initialize();
    if (result != 0) {
      throw new RuntimeException("TestMode driver initialization failed with code: " + result);
    }
    initialized = true;
  }

  /**
   * Returns whether the TestMode library has been initialized.
   *
   * @return true if {@link #initialize()} has been called successfully
   */
  public static boolean isInitialized() {
    return initialized;
  }

  private TestMode() {}
}
