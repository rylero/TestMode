package com.testmode.frc;

/**
 * Example TestMode vendordep class.
 *
 * <p>This class demonstrates how a robot project would interact with the TestMode library.
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

  /**
   * Adds two numbers together.
   *
   * @param num1 the first number
   * @param num2 the second number
   * @return the sum of num1 and num2
   */
  public static double add(double num1, double num2) {
    return num1 + num2;
  }

  private TestMode() {}
}
