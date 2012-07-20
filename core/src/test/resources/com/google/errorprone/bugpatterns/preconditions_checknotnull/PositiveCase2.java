package com.google.errorprone.bugpatterns.preconditions_checknotnull;

/**
 * Test case for fully qualified methodIs call.
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class PositiveCase2 {
  public void error() {
    com.google.common.base.Preconditions.checkNotNull("string literal");    //BUG
  }
}
