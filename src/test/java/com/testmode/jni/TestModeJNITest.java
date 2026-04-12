package com.testmode.jni;

import org.junit.jupiter.api.Test;

public class TestModeJNITest {
  @Test
  void jniLinkTest() {
    // Test to verify that the JNI link works correctly.
    TestModeJNI.initialize();
  }
}
