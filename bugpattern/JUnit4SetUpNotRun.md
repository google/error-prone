---
title: JUnit4SetUpNotRun
summary: setUp() method will not be run; please add JUnit's @Before annotation
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
JUnit 3 provides the method setUp(), to be overridden by subclasses when the test needs to perform some pre-test initialization. In JUnit 4, this is accomplished by annotating such a method with @Before.

 The method that triggered this error matches the definition of setUp() from JUnit3, but was not annotated with @Before and thus won't be run by the JUnit4 runner.

 If you intend for this setUp() method not to run by the JUnit4 runner, but perhaps manually be invoked in certain test methods, please rename the method or mark it private. 

 If the method is part of an abstract test class hierarchy where this class's setUp() is invoked by a superclass method that is annotated with @Before, then please rename the abstract method or add @Before to the superclass's definition of setUp()

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JUnit4SetUpNotRun")` to the enclosing element.

----------

### Positive examples
__JUnit4SetUpNotRunPositiveCaseCustomBefore.java__

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

package com.google.errorprone.bugpatterns.testdata;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Slightly funky test case with a custom Before annotation
 */
@RunWith(JUnit4.class)
public class JUnit4SetUpNotRunPositiveCaseCustomBefore {
  // This will compile-fail and suggest the import of org.junit.Before
  // BUG: Diagnostic contains: @Before
  @Before public void setUp() {}
}

@interface Before {}
{% endhighlight %}

__JUnit4SetUpNotRunPositiveCaseCustomBefore2.java__

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

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test case with a custom Before annotation. */
@RunWith(JUnit4.class)
public class JUnit4SetUpNotRunPositiveCaseCustomBefore2 {
  // This will compile-fail and suggest the import of org.junit.Before
  // BUG: Diagnostic contains: @Before
  @Before
  public void initMocks() {}

  // BUG: Diagnostic contains: @Before
  @Before
  protected void badVisibility() {}
}

@interface Before {}
{% endhighlight %}

__JUnit4SetUpNotRunPositiveCases.java__

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

package com.google.errorprone.bugpatterns.testdata;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Basic class with an untagged setUp method */
@RunWith(JUnit4.class)
public class JUnit4SetUpNotRunPositiveCases {
  // BUG: Diagnostic contains: @Before
  public void setUp() {}
}

@RunWith(JUnit4.class)
class J4PositiveCase2 {
  // BUG: Diagnostic contains: @Before
  protected void setUp() {}
}

/**
 * Replace @After with @Before
 */
@RunWith(JUnit4.class)
class J4AfterToBefore {
  // BUG: Diagnostic contains: @Before
  @After
  protected void setUp() {}
}

/**
 * Replace @AfterClass with @BeforeClass
 */
@RunWith(JUnit4.class)
class J4AfterClassToBeforeClass {
  // BUG: Diagnostic contains: @BeforeClass
  @AfterClass
  protected void setUp() {}
}

class BaseTestClass {
  void setUp() {}
}

/**
 * This is the ambiguous case that we want the developer to make the determination as to
 * whether to rename setUp()
 */
@RunWith(JUnit4.class)
class J4Inherit extends BaseTestClass {
  // BUG: Diagnostic contains: @Before
  protected void setUp() {}
}

/**
 * setUp() method overrides parent method with @Override, but that method isn't @Before in the
 * superclass
 */
@RunWith(JUnit4.class)
class J4OverriddenSetUp extends BaseTestClass {
  // BUG: Diagnostic contains: @Before
  @Override protected void setUp() {}
}

@RunWith(JUnit4.class)
class J4OverriddenSetUpPublic extends BaseTestClass {
  // BUG: Diagnostic contains: @Before
  @Override public void setUp() {}
}
{% endhighlight %}

### Negative examples
__JUnit4SetUpNotRunNegativeCases.java__

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

package com.google.errorprone.bugpatterns.testdata;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Not a JUnit 4 test (no @RunWith annotation on the class). */
public class JUnit4SetUpNotRunNegativeCases {
  public void setUp() {}
}

@RunWith(JUnit38ClassRunner.class)
class J4SetUpWrongRunnerType {
  public void setUp() {}
}

@RunWith(JUnit4.class)
class J4SetUpCorrectlyDone {
  @Before
  public void setUp() {}
}

/** May be a JUnit 3 test -- has @RunWith annotation on the class but also extends TestCase. */
@RunWith(JUnit4.class)
class J4SetUpJUnit3Class extends TestCase {
  public void setUp() {}
}

/** setUp() method is private and wouldn't be run by JUnit3 */
@RunWith(JUnit4.class)
class J4PrivateSetUp {
  private void setUp() {}
}

/**
 * setUp() method is package-local. You couldn't have a JUnit3 test class with a package-private
 * setUp() method (narrowing scope from protected to package)
 */
@RunWith(JUnit4.class)
class J4PackageLocalSetUp {
  void setUp() {}
}

@RunWith(JUnit4.class)
class J4SetUpNonVoidReturnType {
  int setUp() {
    return 42;
  }
}

/** setUp() has parameters */
@RunWith(JUnit4.class)
class J4SetUpWithParameters {
  public void setUp(int ignored) {}

  public void setUp(boolean ignored) {}

  public void setUp(String ignored) {}
}

/** setUp() method is static and wouldn't be run by JUnit3 */
@RunWith(JUnit4.class)
class J4StaticSetUp {
  public static void setUp() {}
}

abstract class SetUpAnnotatedBaseClass {
  @Before
  public void setUp() {}
}

/** setUp() method overrides parent method with @Before. It will be run by JUnit4BlockRunner */
@RunWith(JUnit4.class)
class J4SetUpExtendsAnnotatedMethod extends SetUpAnnotatedBaseClass {
  public void setUp() {}
}
{% endhighlight %}

