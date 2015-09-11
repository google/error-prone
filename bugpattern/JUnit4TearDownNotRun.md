---
title: JUnit4TearDownNotRun
summary: tearDown() method will not be run; Please add an @After annotation
layout: bugpattern
category: JUNIT
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
JUnit 3 provides the overridable method tearDown(), to be overridden by  subclasses when the test needs to perform some post-test de-initialization.  In JUnit 4, this is accomplished by annotating such a method with @After. The method that triggered this error matches the definition of tearDown() from JUnit3, but was not annotated with @After and thus won't be run by the JUnit4 runner.

 If you intend for this tearDown() method not to be run by the JUnit4 runner, but perhaps be manually invoked after certain test methods, please rename the method  or mark it private.

 If the method is part of an abstract test class hierarchy where this class's tearDown() is invoked by a superclass method that is annotated with @After, then please rename the abstract method or add @After to the superclass's definition of tearDown()

## Suppression
Suppress false positives by adding an `@SuppressWarnings("JUnit4TearDownNotRun")` annotation to the enclosing element.

----------

## Examples
__JUnit4TearDownNotRunNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Not a JUnit 4 class (no @RunWith annotation on the class).
 */
public class JUnit4TearDownNotRunNegativeCases {
  public void tearDown() {}
}

@RunWith(JUnit38ClassRunner.class)
class J4TearDownDifferentRunner {
  public void tearDown() {}
}

@RunWith(JUnit4.class)
class J4TearDownHasAfter {
  @After
  public void tearDown() {}
}

@RunWith(JUnit4.class)
class J4TearDownExtendsTestCase extends TestCase {
  public void tearDown() {}
}

@RunWith(JUnit4.class)
class J4TearDownPrivateTearDown {
  private void tearDown() {}
}

@RunWith(JUnit4.class)
class J4TearDownPackageLocal {
  void tearDown() {}
}

@RunWith(JUnit4.class)
class J4TearDownNonVoidReturnType {
  int tearDown() { return 42; }
}

@RunWith(JUnit4.class)
class J4TearDownTearDownHasParameters {
  public void tearDown(int ignored) {}
  public void tearDown(boolean ignored) {}
  public void tearDown(String ignored) {}
}

@RunWith(JUnit4.class)
class J4TearDownStaticTearDown {
  public static void tearDown() {}
}

abstract class TearDownAnnotatedBaseClass {
  @After
  public void tearDown() {}
}

@RunWith(JUnit4.class)
class J4TearDownInheritsFromAnnotatedMethod extends TearDownAnnotatedBaseClass {
  public void tearDown() {}
}

@RunWith(JUnit4.class)
class J4TearDownInheritsFromAnnotatedMethod2 extends TearDownAnnotatedBaseClass {
  @After
  public void tearDown() {}
}
{% endhighlight %}

__JUnit4TearDownNotRunPositiveCaseCustomAfter.java__

{% highlight java %}
/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Slightly funky test case with a custom After annotation)
 */
@RunWith(JUnit4.class)
public class JUnit4TearDownNotRunPositiveCaseCustomAfter {
  // This will compile-fail and suggest the import of org.junit.After
  // BUG: Diagnostic contains: @After
  @After public void tearDown() {}
}

@interface After {}
{% endhighlight %}

__JUnit4TearDownNotRunPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author glorioso@google.com
 */
@RunWith(JUnit4.class)
public class JUnit4TearDownNotRunPositiveCases {
  // BUG: Diagnostic contains: @After
  public void tearDown() {}
}

@RunWith(JUnit4.class)
class JUnit4TearDownNotRunPositiveCase2 {
  // BUG: Diagnostic contains: @After
  protected void tearDown() {}
}

@RunWith(JUnit4.class)
class J4BeforeToAfter {
  // BUG: Diagnostic contains: @After
  @Before protected void tearDown() {}
}

@RunWith(JUnit4.class)
class J4BeforeClassToAfterClass {
  // BUG: Diagnostic contains: @AfterClass
  @BeforeClass protected void tearDown() {}
}

class TearDownUnannotatedBaseClass {
  void tearDown() {}
}

@RunWith(JUnit4.class)
class JUnit4TearDownNotRunPositiveCase3 extends TearDownUnannotatedBaseClass {
  // BUG: Diagnostic contains: @After
  protected void tearDown() {}
}

@RunWith(JUnit4.class)
class J4TearDownHasOverride extends TearDownUnannotatedBaseClass {
  // BUG: Diagnostic contains: @After
  @Override protected void tearDown() {}
}

@RunWith(JUnit4.class)
class J4TearDownHasPublicOverride extends TearDownUnannotatedBaseClass {
  // BUG: Diagnostic contains: @After
  @Override public void tearDown() {}
}
{% endhighlight %}

