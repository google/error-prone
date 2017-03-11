---
title: SelfEquals
summary: Testing an object for equality with itself will always be true.
layout: bugpattern
category: JDK
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The arguments to equals method are the same object, so it always returns true.
Either change the arguments to point to different objects or substitute true.

For test cases, instead of explicitly testing equals, use
[EqualsTester from Guava](http://static.javadoc.io/com.google.guava/guava-testlib/19.0/com/google/common/testing/EqualsTester.html).

## Suppression
Suppress false positives by adding an `@SuppressWarnings("SelfEquals")` annotation to the enclosing element.

----------

### Positive examples
__SelfEqualsPositiveCase.java__

{% highlight java %}
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
    return simpleField.equals(((SelfEqualsPositiveCase)other).simpleField);
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
{% endhighlight %}

### Negative examples
__SelfEqualsNegativeCases.java__

{% highlight java %}
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

/**
 * Negative test cases for {@link SelfEquals} check.
 *
 * @author alexeagle@google.com (Alex Eagle)
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class SelfEqualsNegativeCases {
  private String field;

  @Override
  public int hashCode() {
    return field != null ? field.hashCode() : 0;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SelfEqualsNegativeCases)) {
      return false;
    }

    SelfEqualsNegativeCases other = (SelfEqualsNegativeCases) o;
    return field.equals(other.field);
  }

  public boolean test() {
    return Boolean.TRUE.toString().equals(Boolean.FALSE.toString());
  }

  public void testAssertThatEq(SelfEqualsNegativeCases obj) {
    assertThat(obj).isEqualTo(obj);
  }

  public void testAssertThatNeq(SelfEqualsNegativeCases obj) {
    assertThat(obj).isNotEqualTo(obj);
  }
}
{% endhighlight %}

