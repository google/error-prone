// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.refactors.objects_equal_self_comparison;

import com.google.common.base.Objects;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class NegativeCases {
  private String field;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    NegativeCases other = ((NegativeCases)o);
    return Objects.equal(field, other.field);
  }

  @Override
  public int hashCode() {
    return field != null ? field.hashCode() : 0;
  }
}
