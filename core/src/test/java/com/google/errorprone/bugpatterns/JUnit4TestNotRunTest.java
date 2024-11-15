/*
 * Copyright 2013 The Error Prone Authors.
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
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class JUnit4TestNotRunTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(JUnit4TestNotRun.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(JUnit4TestNotRun.class, getClass());

  @Test
  public void positiveCase1() {
    compilationHelper
        .addSourceLines(
            "JUnit4TestNotRunPositiveCase1.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

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
            }\
            """)
        .doTest();
  }

  @Test
  public void positiveCase2() {
    compilationHelper
        .addSourceLines(
            "JUnit4TestNotRunPositiveCase2.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import org.junit.runner.RunWith;
            import org.mockito.junit.MockitoJUnitRunner;

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
            }\
            """)
        .doTest();
  }

  @Test
  public void containsVerifyAsIdentifier_shouldBeTest() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.mockito.Mockito.verify;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class Test {
              // BUG: Diagnostic contains: @Test
              public void shouldDoSomething() {
                verify(null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void containsQualifiedVerify_shouldBeTest() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;
            import org.mockito.Mockito;

            @RunWith(JUnit4.class)
            public class Test {
              // BUG: Diagnostic contains: @Test
              public void shouldDoSomething() {
                Mockito.verify(null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void containsAssertAsIdentifier_shouldBeTest() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;
            import static com.google.common.truth.Truth.assertThat;
            import java.util.Collections;

            @RunWith(JUnit4.class)
            public class Test {
              // BUG: Diagnostic contains: @Test
              public void shouldDoSomething() {
                assertThat(2).isEqualTo(2);
              }

              // BUG: Diagnostic contains: @Test
              public void shouldDoTwoThings() {
                Collections.sort(Collections.<Integer>emptyList());
                assertThat(3).isEqualTo(3);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void containsQualifiedAssert_shouldBeTest() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;
            import com.google.common.truth.Truth;

            @RunWith(JUnit4.class)
            public class Test {
              // BUG: Diagnostic contains: @Test
              public void shouldDoSomething() {
                Truth.assertThat(1).isEqualTo(1);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void containsCheckAsIdentifier_shouldBeTest() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.base.Preconditions.checkState;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class Test {
              // BUG: Diagnostic contains: @Test
              public void shouldDoSomething() {
                checkState(false);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void containsQualifiedCheck_shouldBeTest() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Preconditions;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class Test {
              // BUG: Diagnostic contains: @Test
              public void shouldDoSomething() {
                Preconditions.checkState(false);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void containsFailAsIdentifier_shouldBeTest() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.fail;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class Test {
              // BUG: Diagnostic contains: @Test
              public void shouldDoSomething() {
                fail();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void containsQualifiedFail_shouldBeTest() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import org.junit.Assert;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class Test {
              // BUG: Diagnostic contains: @Test
              public void shouldDoSomething() {
                Assert.fail();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void containsExpectAsIdentifier_shouldBeTest() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class Test {
              // BUG: Diagnostic contains: @Test
              public void shouldDoSomething() {
                assertThrows(null, null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void containsQualifiedExpect_shouldBeTest() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import org.junit.Assert;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class Test {
              // BUG: Diagnostic contains: @Test
              public void shouldDoSomething() {
                Assert.assertThrows(null, null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void noTestKeyword_notATest() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;
            import java.util.Collections;

            @RunWith(JUnit4.class)
            public class Test {
              public void shouldDoSomething() {
                Collections.sort(Collections.<Integer>emptyList());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void staticMethodWithTestKeyword_notATest() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;
            import java.util.Collections;

            @RunWith(JUnit4.class)
            public class Test {
              private static void assertDoesSomething() {}

              public static void shouldDoSomething() {
                assertDoesSomething();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void hasOtherAnnotation_notATest() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class Test {
              @Deprecated
              public void shouldDoSomething() {
                verify();
              }

              private void verify() {}
            }
            """)
        .doTest();
  }

  @Test
  public void hasOtherAnnotationAndNamedTest_shouldBeTest() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;
            import java.util.Collections;

            @RunWith(JUnit4.class)
            public class Test {
              @Deprecated
              // BUG: Diagnostic contains: @Test
              public void testShouldDoSomething() {
                Collections.sort(Collections.<Integer>emptyList());
              }

              private void verify() {}
            }
            """)
        .doTest();
  }

  @Test
  public void shouldNotDetectMethodsOnAbstractClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public abstract class Test {
              public void testDoSomething() {}
            }
            """)
        .doTest();
  }

  @Test
  public void fix() {
    refactoringHelper
        .addInputLines(
            "in/TestStuff.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class TestStuff {
              public void testDoSomething() {}
            }
            """)
        .addOutputLines(
            "out/TestStuff.java",
            """
            import org.junit.Test;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class TestStuff {
              @Test
              public void testDoSomething() {}
            }
            """)
        .setFixChooser(FixChoosers.FIRST)
        .doTest();
  }

  @Test
  public void ignoreFix() {
    refactoringHelper
        .addInputLines(
            "in/TestStuff.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class TestStuff {
              public void testDoSomething() {}
            }
            """)
        .addOutputLines(
            "out/TestStuff.java",
            """
            import org.junit.Ignore;
            import org.junit.Test;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class TestStuff {
              @Test
              @Ignore
              public void testDoSomething() {}
            }
            """)
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void makePrivateFix() {
    refactoringHelper
        .addInputLines(
            "in/TestStuff.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class TestStuff {
              public void testDoSomething() {}
            }
            """)
        .addOutputLines(
            "out/TestStuff.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class TestStuff {
              private void testDoSomething() {}
            }
            """)
        .setFixChooser(FixChoosers.THIRD)
        .doTest();
  }

  @Test
  public void ignoreFixComesFirstWhenTestNamedDisabled() {
    refactoringHelper
        .addInputLines(
            "in/TestStuff.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class TestStuff {
              public void disabledTestDoSomething() {
                verify();
              }

              void verify() {}
            }
            """)
        .addOutputLines(
            "out/TestStuff.java",
            """
            import org.junit.Ignore;
            import org.junit.Test;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class TestStuff {
              @Test
              @Ignore
              public void disabledTestDoSomething() {
                verify();
              }

              void verify() {}
            }
            """)
        .setFixChooser(FixChoosers.FIRST)
        .doTest();
  }

  @Test
  public void helperMethodCalledElsewhere() {
    compilationHelper
        .addSourceLines(
            "TestStuff.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;
            import org.junit.Test;

            @RunWith(JUnit4.class)
            public class TestStuff {
              public void shouldDoSomething() {
                verify();
              }

              @Test
              public void testDoesSomething() {
                shouldDoSomething();
              }

              void verify() {}
            }
            """)
        .doTest();
  }

  @Test
  public void helperMethodCallFoundInNestedInvocation() {
    compilationHelper
        .addSourceLines(
            "TestStuff.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;
            import org.junit.Test;
            import java.util.function.Consumer;

            @RunWith(JUnit4.class)
            public class TestStuff {
              public void shouldDoSomething() {
                verify();
              }

              @Test
              public void testDoesSomething() {
                doSomething(u -> shouldDoSomething());
              }

              void doSomething(Consumer f) {}

              void verify() {}
            }
            """)
        .doTest();
  }

  @Test
  public void runWithNotRequired() {
    compilationHelper
        .addSourceLines(
            "TestStuff.java",
            """
            import org.junit.Test;

            public class TestStuff {
              // BUG: Diagnostic contains: @Test
              public void testDoesSomething() {}

              @Test
              public void foo() {}
            }
            """)
        .doTest();
  }

  @Test
  public void suppression() {
    compilationHelper
        .addSourceLines(
            "TestStuff.java",
            """
            import org.junit.Test;

            public class TestStuff {
              @SuppressWarnings("JUnit4TestNotRun")
              public void testDoesSomething() {}

              @Test
              public void foo() {}
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase1() {
    compilationHelper
        .addSourceLines(
            "JUnit4TestNotRunNegativeCase1.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * Not a JUnit 4 test (no @RunWith annotation on the class).
             *
             * @author eaftan@google.com (Eddie Aftandilian)
             */
            public class JUnit4TestNotRunNegativeCase1 {
              public void testThisIsATest() {}
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase2() {
    compilationHelper
        .addSourceLines(
            "JUnit4TestNotRunNegativeCase2.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import org.junit.internal.runners.JUnit38ClassRunner;
            import org.junit.runner.RunWith;

            /**
             * Not a JUnit 4 test (run with a JUnit3 test runner).
             *
             * @author eaftan@google.com (Eddie Aftandilian)
             */
            @RunWith(JUnit38ClassRunner.class)
            public class JUnit4TestNotRunNegativeCase2 {
              public void testThisIsATest() {}
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase3() {
    compilationHelper
        .addSourceLines(
            "JUnit4TestNotRunNegativeCase3.java",
            """
package com.google.errorprone.bugpatterns.testdata;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class JUnit4TestNotRunNegativeCase3 {
  // Doesn't begin with "test", and doesn't contain any assertion-like method invocations.
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
  public int testSomething() {
    return 42;
  }
}\
""")
        .doTest();
  }

  @Test
  public void negativeCase4() {
    compilationHelper
        .addSourceLines(
            "JUnit4TestNotRunNegativeCase4.java",
            """
package com.google.errorprone.bugpatterns.testdata;

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
}\
""")
        .doTest();
  }

  @Test
  public void negativeCase5() {
    compilationHelper
        .addSourceLines(
            "JUnit4TestNotRunBaseClass.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import org.junit.After;
            import org.junit.Before;
            import org.junit.Test;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            /**
             * Base class for test cases to extend.
             *
             * @author eaftan@google.com (Eddie Aftandilian)
             */
            @RunWith(JUnit4.class)
            public class JUnit4TestNotRunBaseClass {
              @Before
              public void testSetUp() {}

              @After
              public void testTearDown() {}

              @Test
              public void testOverrideThis() {}
            }\
            """)
        .addSourceLines(
            "JUnit4TestNotRunNegativeCase5.java",
            """
package com.google.errorprone.bugpatterns.testdata;

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
}\
""")
        .doTest();
  }

  @Test
  public void junit3TestWithIgnore() {
    compilationHelper
        .addSourceLines(
            "TestStuff.java",
            """
            import org.junit.Ignore;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class TestStuff {
              @Ignore
              public void testMethod() {}
            }
            """)
        .doTest();
  }

  @Test
  public void junit4Theory_isTestAnnotation() {
    compilationHelper
        .addSourceLines(
            "TestTheories.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.experimental.theories.Theories;
            import org.junit.experimental.theories.Theory;

            @RunWith(Theories.class)
            public class TestTheories {
              @Theory
              public void testMyTheory() {}
            }
            """)
        .doTest();
  }

  @Test
  public void methodsWithParameters_areStillTests() {
    compilationHelper
        .addSourceLines(
            "TestTheories.java",
            """
            import static org.junit.Assert.fail;
            import org.junit.runner.RunWith;
            import org.junit.experimental.theories.Theories;
            import org.junit.experimental.theories.FromDataPoints;

            @RunWith(Theories.class)
            public class TestTheories {
              // BUG: Diagnostic contains:
              public void testMyTheory(@FromDataPoints("foo") Object foo) {
                fail();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void annotationOnSuperMethod() {
    compilationHelper
        .addSourceLines(
            "TestSuper.java",
            """
            import org.junit.Test;

            public class TestSuper {
              @Test
              public void testToOverride() {}
            }
            """)
        .addSourceLines(
            "TestSub.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class TestSub extends TestSuper {
              @Override
              public void testToOverride() {}
            }
            """)
        .doTest();
  }

  @Test
  public void underscoreInName_mustBeATest() {
    compilationHelper
        .addSourceLines(
            "T.java",
            """
            import org.junit.Test;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class T {
              // BUG: Diagnostic contains:
              public void givenFoo_thenBar() {}
            }
            """)
        .doTest();
  }
}
