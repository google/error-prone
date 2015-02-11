package com.google.errorprone.bugpatterns;

import com.google.common.base.Objects;

/**
 * @author sebastian.h.monte@gmail.com (Sebastian Monte)
 */
public class AssertFalseNegativeCases {

  public void assertTrue() {
    assert true;
  }

  public void assertFalseFromCondition() {
    assert 0 == 1;
  }
}