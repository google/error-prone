// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.errorprone.suppresswarnings;

/**
 * Test cases to ensure SuppressWarnings annotation is respected.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@SuppressWarnings("DeadException")
public class NegativeCases {

  @SuppressWarnings({"EmptyIf", "EmptyStatement"})
  public void testEmptyIf() {
    int i = 0;
    if (i == 10); {
      System.out.println("foo");
    }
  }

  @SuppressWarnings({"bar", "SelfAssignment"})
  public void testSelfAssignment() {
    int i = 0;
    i = i;
  }

  public void testDeadException() {
    new RuntimeException("whoops");
  }
}
