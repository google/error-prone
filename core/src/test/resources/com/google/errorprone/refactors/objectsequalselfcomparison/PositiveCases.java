// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.refactors.objectsequalselfcomparison;

import com.google.common.base.Objects;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class PositiveCases {
  private String field = "";

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PositiveCases other = (PositiveCases)o;
    return Objects.equal(field, field); //oops, should have been other.field
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(field);
  }
}
