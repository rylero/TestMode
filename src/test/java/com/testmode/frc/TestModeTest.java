package com.testmode.frc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class TestModeTest {
  @Test
  void testAdd() {
    assertEquals(5.0, TestMode.add(2, 3));
    assertEquals(0.0, TestMode.add(-1, 1));
    assertEquals(-3.0, TestMode.add(-1, -2));
  }
}
