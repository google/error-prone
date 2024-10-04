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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author glorioso@google.com (Nick Glorioso)
 */
@RunWith(JUnit4.class)
public class JUnit4TearDownNotRunTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(JUnit4TearDownNotRun.class, getClass());

  @Test
  public void positiveCases() {
    compilationHelper
        .addSourceLines(
            "JUnit4TearDownNotRunPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

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
            }""")
        .doTest();
  }

  @Test
  public void positiveCase_customAnnotation() {
    compilationHelper
        .addSourceLines(
            "JUnit4TearDownNotRunPositiveCaseCustomAfter.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            /** Slightly funky test case with a custom After annotation) */
            @RunWith(JUnit4.class)
            public class JUnit4TearDownNotRunPositiveCaseCustomAfter {
              // This will compile-fail and suggest the import of org.junit.After
              // BUG: Diagnostic contains: @After
              @After
              public void tearDown() {}
            }

            @interface After {}""")
        .doTest();
  }

  @Test
  public void positiveCase_customAnnotationDifferentName() {
    compilationHelper
        .addSourceLines(
            "JUnit4TearDownNotRunPositiveCaseCustomAfter2.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            /** Test case with a custom After annotation. */
            @RunWith(JUnit4.class)
            public class JUnit4TearDownNotRunPositiveCaseCustomAfter2 {
              // This will compile-fail and suggest the import of org.junit.After
              // BUG: Diagnostic contains: @After
              @After
              public void tidyUp() {}
            }

            @interface After {}""")
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceLines(
            "JUnit4TearDownNotRunNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import junit.framework.TestCase;
            import org.junit.After;
            import org.junit.internal.runners.JUnit38ClassRunner;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            /** Not a JUnit 4 class (no @RunWith annotation on the class). */
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
              int tearDown() {
                return 42;
              }
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
            }""")
        .doTest();
  }
}
