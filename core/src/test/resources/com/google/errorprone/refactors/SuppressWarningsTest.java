// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.errorprone.refactors;

/**
 * Test to ensure SuppressWarnings annotation is respected.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@SuppressWarnings("Dead exception")
public class SuppressWarningsTest {

  @SuppressWarnings("Empty if")
  public void testEmptyIf() {
    int i = 0;
    if (i == 10); {
      System.out.println("foo");
    }
  }

  @SuppressWarnings({"bar", "Self assignment"})
  public void testSelfAssignment() {
    int i = 0;
    i = i;
  }

  public void testDeadException() {
    new RuntimeException("whoops");
  }
}
