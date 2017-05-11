/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns.testdata;

import com.google.common.base.Objects;

/** @author alexeagle@google.com (Alex Eagle) */
public class GuavaSelfEqualsPositiveCase {
  private String field = "";

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GuavaSelfEqualsPositiveCase other = (GuavaSelfEqualsPositiveCase) o;
    boolean retVal;
    // BUG: Diagnostic contains: Objects.equal(field, other.field)
    retVal = Objects.equal(field, field);
    // BUG: Diagnostic contains: Objects.equal(other.field, this.field)
    retVal &= Objects.equal(field, this.field);
    // BUG: Diagnostic contains: Objects.equal(this.field, other.field)
    retVal &= Objects.equal(this.field, field);
    // BUG: Diagnostic contains: Objects.equal(this.field, other.field)
    retVal &= Objects.equal(this.field, this.field);

    return retVal;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(field);
  }

  public static void test() {
    ForTesting tester = new ForTesting();
    // BUG: Diagnostic contains: Objects.equal(tester.testing.testing, tester.testing)
    Objects.equal(tester.testing.testing, tester.testing.testing);
  }

  private static class ForTesting {
    public ForTesting testing;
    public String string;
  }
}
