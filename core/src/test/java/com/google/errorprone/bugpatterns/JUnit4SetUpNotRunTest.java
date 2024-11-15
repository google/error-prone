/*
 * Copyright 2014 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.JUnit4;
import org.junit.runners.ParentRunner;

/**
 * @author glorioso@google.com (Nick Glorioso)
 */
@RunWith(JUnit4.class)
public class JUnit4SetUpNotRunTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(JUnit4SetUpNotRun.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(JUnit4SetUpNotRun.class, getClass());

  @Test
  public void positiveCases() {
    compilationHelper
        .addSourceLines(
            "JUnit4SetUpNotRunPositiveCases.java",
            """
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
}\
""")
        .doTest();
  }

  @Test
  public void positiveCase_customBefore() {
    compilationHelper
        .addSourceLines(
            "JUnit4SetUpNotRunPositiveCaseCustomBefore.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            /** Slightly funky test case with a custom Before annotation */
            @RunWith(JUnit4.class)
            public class JUnit4SetUpNotRunPositiveCaseCustomBefore {
              // This will compile-fail and suggest the import of org.junit.Before
              // BUG: Diagnostic contains: @Before
              @Before
              public void setUp() {}
            }

            @interface Before {}\
            """)
        .doTest();
  }

  @Test
  public void customBefore_refactoring() {
    refactoringTestHelper
        .addInputLines("Before.java", "  @interface Before {}")
        .expectUnchanged()
        .addInputLines(
            "in/Foo.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class Foo {
              @Before
              public void initMocks() {}

              @Before
              protected void badVisibility() {}
            }
            """)
        .addOutputLines(
            "out/Foo.java",
            """
            import org.junit.Before;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class Foo {
              @Before
              public void initMocks() {}

              @Before
              public void badVisibility() {}
            }
            """)
        .doTest();
  }

  @Test
  public void positiveCase_customBeforeDifferentName() {
    compilationHelper
        .addSourceLines(
            "JUnit4SetUpNotRunPositiveCaseCustomBefore2.java",
            """
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

            @interface Before {}\
            """)
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceLines(
            "JUnit4SetUpNotRunNegativeCases.java",
            """
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
}\
""")
        .doTest();
  }

  public abstract static class SuperTest {
    @Before
    public void setUp() {}
  }

  @Test
  public void noBeforeOnClasspath() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import " + SuperTest.class.getCanonicalName() + ";",
            "@RunWith(JUnit4.class)",
            "class Test extends SuperTest {",
            "  @Override public void setUp() {}",
            "}")
        .withClasspath(
            RunWith.class,
            JUnit4.class,
            BlockJUnit4ClassRunner.class,
            ParentRunner.class,
            SuperTest.class,
            SuperTest.class.getEnclosingClass())
        .doTest();
  }
}
