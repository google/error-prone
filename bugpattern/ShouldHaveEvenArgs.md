---
title: ShouldHaveEvenArgs
summary: This method must be called with an even number of arguments.
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ShouldHaveEvenArgs")` to the enclosing element.

----------

### Positive examples
__ShouldHaveEvenArgsPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.common.truth.Correspondence;
import java.util.HashMap;
import java.util.Map;

/**
 * Positive test cases for {@link ShouldHaveEvenArgs} check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class ShouldHaveEvenArgsPositiveCases {

  private static final Map map = new HashMap<String, String>();

  public void testWithOddArgs() {
    // BUG: Diagnostic contains: even number of arguments
    assertThat(map).containsExactly("hello", "there", "rest");

    // BUG: Diagnostic contains: even number of arguments
    assertThat(map).containsExactly("hello", "there", "hello", "there", "rest");

    // BUG: Diagnostic contains: even number of arguments
    assertThat(map).containsExactly(null, null, null, null, new Object[] {});
  }

  public void testWithArrayArgs() {
    String key = "hello";
    Object[] value = new Object[] {};
    Object[][] args = new Object[][] {};

    // BUG: Diagnostic contains: even number of arguments
    assertThat(map).containsExactly(key, value, (Object) args);
  }

  public void testWithOddArgsWithCorrespondence() {
    assertThat(map)
        .comparingValuesUsing(new TestCorrespondence())
        // BUG: Diagnostic contains: even number of arguments
        .containsExactly("hello", "there", "rest");

    assertThat(map)
        .comparingValuesUsing(new TestCorrespondence())
        // BUG: Diagnostic contains: even number of arguments
        .containsExactly("hello", "there", "hello", "there", "rest");
  }

  private static class TestCorrespondence extends Correspondence<String, String> {

    @Override
    public boolean compare(String str1, String str2) {
      return true;
    }

    @Override
    public String toString() {
      return "test_correspondence";
    }
  }
}
{% endhighlight %}

### Negative examples
__ShouldHaveEvenArgsNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import java.util.HashMap;
import java.util.Map;

/**
 * Negative test cases for {@link ShouldHaveEvenArgs} check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class ShouldHaveEvenArgsNegativeCases {

  private static final Map<String, String> map = new HashMap<String, String>();

  public void testWithNoArgs() {
    assertThat(map).containsExactly();
  }

  public void testWithMinimalArgs() {
    assertThat(map).containsExactly("hello", "there");
  }

  public void testWithEvenArgs() {
    assertThat(map).containsExactly("hello", "there", "hello", "there");
  }

  public void testWithVarargs(Object... args) {
    assertThat(map).containsExactly("hello", args);
    assertThat(map).containsExactly("hello", "world", args);
  }

  public void testWithArray() {
    String[] arg = {"hello", "there"};
    assertThat(map).containsExactly("yolo", arg);

    String key = "hello";
    Object[] value = new Object[] {};
    Object[][] args = new Object[][] {};

    assertThat(map).containsExactly(key, value);
    assertThat(map).containsExactly(key, value, (Object[]) args);
    assertThat(map).containsExactly(key, value, key, value, key, value);
  }
}
{% endhighlight %}

