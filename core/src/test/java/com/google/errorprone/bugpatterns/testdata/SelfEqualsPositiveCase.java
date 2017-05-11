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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Assert;

/**
 * Positive test cases for {@link SelfEquals} check.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class SelfEqualsPositiveCase {
  protected String simpleField;

  public boolean test1(Object obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SelfEqualsPositiveCase other = (SelfEqualsPositiveCase) obj;
    // BUG: Diagnostic contains: simpleField.equals(other.simpleField);
    return simpleField.equals(simpleField);
  }

  public boolean test2(SelfEqualsPositiveCase obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SelfEqualsPositiveCase other = (SelfEqualsPositiveCase) obj;
    // BUG: Diagnostic contains: simpleField.equals(other.simpleField);
    return simpleField.equals(this.simpleField);
  }

  public boolean test3(SelfEqualsPositiveCase obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SelfEqualsPositiveCase other = (SelfEqualsPositiveCase) obj;
    // BUG: Diagnostic contains: this.simpleField.equals(other.simpleField);
    return this.simpleField.equals(simpleField);
  }

  public boolean test4(SelfEqualsPositiveCase obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SelfEqualsPositiveCase other = (SelfEqualsPositiveCase) obj;
    // BUG: Diagnostic contains: this.simpleField.equals(other.simpleField);
    return this.simpleField.equals(this.simpleField);
  }

  public boolean test5(SelfEqualsPositiveCase obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SelfEqualsPositiveCase other = (SelfEqualsPositiveCase) obj;
    // BUG: Diagnostic contains:
    return equals(this);
  }

  public void testAssertTrue(SelfEqualsPositiveCase obj) {
    Assert.assertTrue(obj.equals(obj));
  }

  public void testAssertThat(SelfEqualsPositiveCase obj) {
    assertThat(obj.equals(obj)).isTrue();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SelfEqualsPositiveCase other = (SelfEqualsPositiveCase) obj;
    return simpleField.equals(((SelfEqualsPositiveCase) other).simpleField);
  }

  private static class SubClass extends SelfEqualsPositiveCase {
    @Override
    public boolean equals(Object obj) {
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      SubClass other = (SubClass) obj;
      return simpleField.equals(((SubClass) other).simpleField);
    }
  }

  public void testSub() {
    SubClass sc = new SubClass();
    // BUG: Diagnostic contains:
    sc.equals(sc);
  }
}
