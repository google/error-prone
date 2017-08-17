---
title: JUnit3TestNotRun
summary: Test method will not be run; please correct method signature (Should be public, non-static, and method name should begin with "test").
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
JUnit 3 requires that test method names start with "`test`". The method that
triggered this error looks like it is supposed to be a test, but misspells the
required prefix; has `@Test` annotation, but no prefix; or has the wrong method
signature. As a consequence, JUnit 3 will ignore it.

If you meant to disable this test on purpose, or this is a helper method, change
the name to something more descriptive, like "`disabledTestSomething()`". You
don't need an `@Test` annotation, but if you want to keep it, add `@Ignore` too.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("JUnit3TestNotRun")` annotation to the enclosing element.

----------

### Positive examples
__JUnit3TestNotRunPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
import org.junit.Test;

/** @author rburny@google.com (Radoslaw Burny) */
public class JUnit3TestNotRunPositiveCases extends TestCase {
  // BUG: Diagnostic contains: JUnit3TestNotRun
  public static void tesNameStatic() {}

  // These names are trickier to correct, but we should still indicate the bug
  // BUG: Diagnostic contains: JUnit3TestNotRun
  public void tetsName() {}

  // BUG: Diagnostic contains: JUnit3TestNotRun
  public void tesstName() {}

  // BUG: Diagnostic contains: JUnit3TestNotRun
  public void tesetName() {}

  // BUG: Diagnostic contains: JUnit3TestNotRun
  public void tesgName() {}

  // tentative - can cause false positives
  // BUG: Diagnostic contains: JUnit3TestNotRun
  public void textName() {}
}
{% endhighlight %}

### Negative examples
__JUnit3TestNotRunNegativeCase1.java__

{% highlight java %}
/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
import org.junit.Ignore;
import org.junit.Test;

/** @author rburny@google.com (Radoslaw Burny) */
public class JUnit3TestNotRunNegativeCase1 extends TestCase {

  // correctly spelled
  public void test() {}

  public void testCorrectlySpelled() {}

  // real words
  public void bestNameEver() {}

  public void destroy() {}

  public void restore() {}

  public void establish() {}

  public void estimate() {}

  // different signature
  public boolean teslaInventedLightbulb() {
    return true;
  }

  public void tesselate(float f) {}

  // surrounding class is not a JUnit3 TestCase
  private static class TestCase {
    private void tesHelper() {}

    private void destroy() {}
  }

  // correct test, despite redundant annotation
  @Test
  public void testILikeAnnotations() {}

  // both @Test & @Ignore
  @Test
  @Ignore
  public void ignoredTest2() {}

  @Ignore
  @Test
  public void ignoredTest() {}
}
{% endhighlight %}

__JUnit3TestNotRunNegativeCase2.java__

{% highlight java %}
/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * JUnit4 test class - we should not issue errors on that.
 *
 * @author rburny@google.com (Radoslaw Burny)
 */
@RunWith(JUnit4.class)
public class JUnit3TestNotRunNegativeCase2 {

  //JUnit4 tests should be ignored, no matter what their names are.
  @Test
  public void nameDoesNotStartWithTest() {}

  @Test
  public void tesName() {}

  @Test
  public void tstName() {}

  @Test
  public void TestName() {}
}
{% endhighlight %}

__JUnit3TestNotRunNegativeCase3.java__

{% highlight java %}
/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;

/**
 * Tricky case - mixed JUnit3 and JUnit4.
 *
 * @author rburny@google.com (Radoslaw Burny)
 */
@RunWith(Runner.class)
public class JUnit3TestNotRunNegativeCase3 extends TestCase {

  @Test
  public void name() {}

  public void tesMisspelled() {}

  @Test
  public void tesBothIssuesAtOnce() {}
}
{% endhighlight %}

__JUnit3TestNotRunNegativeCase4.java__

{% highlight java %}
/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
import org.junit.Test;

/**
 * Abstract class - let's ignore those for now, it's hard to say what are they run with.
 *
 * @author rburny@google.com (Radoslaw Burny)
 */
public abstract class JUnit3TestNotRunNegativeCase4 extends TestCase {

  @Test
  public void name() {}

  public void tesMisspelled() {}

  @Test
  public void tesBothIssuesAtOnce() {}
}
{% endhighlight %}

__JUnit3TestNotRunNegativeCase5.java__

{% highlight java %}
/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

import org.junit.Test;

/**
 * Class inherits RunWith from superclass, so should not emit errors.
 *
 * @author rburny@google.com (Radoslaw Burny)
 */
public class JUnit3TestNotRunNegativeCase5 extends JUnit3TestNotRunNegativeCase3 {

  public void testEasyCase() {}

  @Test
  public void name() {}

  public void tesMisspelled() {}

  @Test
  public void tesBothIssuesAtOnce() {}
}
{% endhighlight %}

