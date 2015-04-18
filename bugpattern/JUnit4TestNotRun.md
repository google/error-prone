---
title: JUnit4TestNotRun
layout: bugpattern
category: JUNIT
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>JUNIT</td></tr>
<tr><td>Severity</td><td>ERROR</td></tr>
<tr><td>Maturity</td><td>MATURE</td></tr>
</table></div>

# Bug pattern: JUnit4TestNotRun
__Test method will not be run; please add @Test annotation__

## The problem
JUnit 3 required that test methods be named in a special way to be run as part of a test case. JUnit 4 requires that test methods be annotated with @Test. The test method that triggered this error is named like a JUnit 3 test, but is in a JUnit 4 test class.  Thus, it will not be run unless you annotate it with @Test.

If you intend for this test method not to run, please add both an @Test and an @Ignore annotation to make it clear that you are purposely disabling it.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("JUnit4TestNotRun")` annotation to the enclosing element.

----------

# Examples
__JUnit4TestNotRunNegativeCase1.java__

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

package com.google.errorprone.bugpatterns;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Not a JUnit 4 test (no @RunWith annotation on the class).
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class JUnit4TestNotRunNegativeCase1 {
  public void testThisIsATest() {}
}
{% endhighlight %}

__JUnit4TestNotRunNegativeCase2.java__

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

package com.google.errorprone.bugpatterns;

import org.junit.runner.RunWith;
import org.junit.internal.runners.JUnit38ClassRunner;

/**
 * Not a JUnit 4 test (run with a JUnit3 test runner).
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit38ClassRunner.class)
public class JUnit4TestNotRunNegativeCase2 {
  public void testThisIsATest() {}
}
{% endhighlight %}

__JUnit4TestNotRunNegativeCase3.java__

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

package com.google.errorprone.bugpatterns;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class JUnit4TestNotRunNegativeCase3 {
  // Doesn't begin with "test".
  public void thisIsATest() {}
  
  // Isn't public.
  void testTest1() {}
  
  // Have checked annotation.
  @Test
  public void testTest2() {}
    
  @Before
  public void testBefore() {}
  
  @After
  public void testAfter() {}
  
  @BeforeClass
  public void testBeforeClass() {}
  
  @AfterClass
  public void testAfterClass() {}
  
  // Has parameters.
  public void testTest3(int foo) {}

  // Doesn't return void
  public int testSomething() { return 42; }
}
{% endhighlight %}

__JUnit4TestNotRunNegativeCase4.java__

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

package com.google.errorprone.bugpatterns;

import junit.framework.TestCase;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * May be a JUnit 3 test -- has @RunWith annotation on the class but also extends TestCase.
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class JUnit4TestNotRunNegativeCase4 extends TestCase {
  public void testThisIsATest() {}
}
{% endhighlight %}

__JUnit4TestNotRunNegativeCase5.java__

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
 * Methods that override methods with @Test should not trigger an error (JUnit 4 will run them).
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class JUnit4TestNotRunNegativeCase5 extends JUnit4TestNotRunBaseClass {
  @Override
  public void testSetUp() {}

  @Override
  public void testTearDown() {}

  @Override
  public void testOverrideThis() {}
}
{% endhighlight %}

__JUnit4TestNotRunPositiveCase1.java__

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

package com.google.errorprone.bugpatterns;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class JUnit4TestNotRunPositiveCase1 {
  // BUG: Diagnostic contains: @Test
  public void testThisIsATest() {}
  
  // BUG: Diagnostic contains: @Test
  public static void testThisIsAStaticTest() {}
}
{% endhighlight %}

__JUnit4TestNotRunPositiveCase2.java__

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

package com.google.errorprone.bugpatterns;

import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Mockito test runner that uses JUnit 4.
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(MockitoJUnitRunner.class)
public class JUnit4TestNotRunPositiveCase2 {
  // BUG: Diagnostic contains: @Test
  public void testThisIsATest() {}
  
  // BUG: Diagnostic contains: @Test
  public static void testThisIsAStaticTest() {}
}
{% endhighlight %}

