package com.google.errorprone.bugpatterns;

import com.google.common.base.Objects;

/**
 * @author sebastian.h.monte@gmail.com (Sebastian Monte)
 */
public class AssertFalsePositiveCases {

  public void assertFalse() {
  // BUG: Diagnostic contains: throw new AssertionError()
    assert false;
  }
}