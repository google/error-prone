---
title: SelfComparison
summary: An object is compared to itself
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The arguments to compareTo method are the same object, so it always returns 0.
Either change the arguments to point to different objects or substitute 0.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("SelfComparison")` annotation to the enclosing element.

----------

### Positive examples
__SelfComparisonPositiveCase.java__

{% highlight java %}
/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

/**
 * Positive test case for {@link SelfComparison} check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class SelfComparisonPositiveCase implements Comparable<Object> {

  public int test1() {
    SelfComparisonPositiveCase obj = new SelfComparisonPositiveCase();
    // BUG: Diagnostic contains: An object is compared to itself
    return obj.compareTo(obj);
  }

  private SelfComparisonPositiveCase obj = new SelfComparisonPositiveCase();

  public int test2() {
    // BUG: Diagnostic contains: An object is compared to itself
    return obj.compareTo(this.obj);
  }

  public int test3() {
    // BUG: Diagnostic contains: An object is compared to itself
    return this.obj.compareTo(obj);
  }

  public int test4() {
    // BUG: Diagnostic contains: An object is compared to itself
    return this.obj.compareTo(this.obj);
  }

  public int test5() {
    // BUG: Diagnostic contains: An object is compared to itself
    return compareTo(this);
  }

  @Override
  public int compareTo(Object o) {
    return 0;
  }

  public static class ComparisonTest implements Comparable<ComparisonTest> {
    private String testField;

    @Override
    public int compareTo(ComparisonTest s) {
      return testField.compareTo(s.testField);
    }

    public int test1() {
      ComparisonTest obj = new ComparisonTest();
      // BUG: Diagnostic contains: An object is compared to itself
      return obj.compareTo(obj);
    }

    private ComparisonTest obj = new ComparisonTest();

    public int test2() {
      // BUG: Diagnostic contains: An object is compared to itself
      return obj.compareTo(this.obj);
    }

    public int test3() {
      // BUG: Diagnostic contains: An object is compared to itself
      return this.obj.compareTo(obj);
    }

    public int test4() {
      // BUG: Diagnostic contains: An object is compared to itself
      return this.obj.compareTo(this.obj);
    }

    public int test5() {
      // BUG: Diagnostic contains: An object is compared to itself
      return compareTo(this);
    }
  }
}
{% endhighlight %}

### Negative examples
__SelfComparisonNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

/**
 * Negative test cases for {@link SelfComparison} check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class SelfComparisonNegativeCases implements Comparable<Object> {
  private String field;

  @Override
  public int hashCode() {
    return field != null ? field.hashCode() : 0;
  }

  @Override
  public int compareTo(Object o) {
    if (!(o instanceof SelfComparisonNegativeCases)) {
      return -1;
    }

    SelfComparisonNegativeCases other = (SelfComparisonNegativeCases) o;
    return field.compareTo(other.field);
  }

  public int test() {
    return Boolean.TRUE.toString().compareTo(Boolean.FALSE.toString());
  }

  public static class CopmarisonTest implements Comparable<CopmarisonTest> {
    private String testField;

    @Override
    public int compareTo(CopmarisonTest obj) {
      return testField.compareTo(obj.testField);
    }
  }
}
{% endhighlight %}

