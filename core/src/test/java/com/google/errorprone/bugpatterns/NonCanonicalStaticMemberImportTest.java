/*
 * Copyright 2015 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link NonCanonicalStaticMemberImport}Test */
@RunWith(JUnit4.class)
public class NonCanonicalStaticMemberImportTest {
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(NonCanonicalStaticMemberImport.class, getClass());

  @Test
  public void positiveMethod() {
    refactoringHelper
        .addInputLines(
            "a/A.java",
            """
            package a;

            public class A {
              public static final int foo() {
                return 42;
              }

              public static final int bar() {
                return 42;
              }
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "b/B.java",
            """
            package b;

            import a.A;

            public class B extends A {}
            """)
        .expectUnchanged()
        .addInputLines(
            "b/Test.java",
            """
            package b;

            import static b.B.bar;
            import static b.B.foo;

            class Test {}
            """)
        .addOutputLines(
            "b/Test.java",
            """
            package b;

            import static a.A.bar;
            import static a.A.foo;

            class Test {}
            """)
        .doTest();
  }

  @Test
  public void positiveField() {
    refactoringHelper
        .addInputLines(
            "a/A.java",
            """
            package a;

            public class A {
              public static final int CONST = 42;
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "b/B.java",
            """
            package b;

            import a.A;

            public class B extends A {}
            """)
        .expectUnchanged()
        .addInputLines(
            "b/Test.java",
            """
            package b;

            import static b.B.CONST;

            class Test {}
            """)
        .addOutputLines(
            "b/Test.java",
            """
            package b;

            import static a.A.CONST;

            class Test {}
            """)
        .doTest();
  }

  // We can't test e.g. a.B.Inner.CONST (a double non-canonical reference), because
  // they're illegal.
  @Test
  public void positiveClassAndField() {
    refactoringHelper
        .addInputLines(
            "a/Super.java",
            """
            package a;

            public class Super {
              public static final int CONST = 42;
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "a/A.java",
            """
            package a;

            public class A {
              public static class Inner extends Super {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "b/B.java",
            """
            package b;

            import a.A;

            public class B extends A {}
            """)
        .expectUnchanged()
        .addInputLines(
            "b/Test.java",
            """
            package b;

            import static a.A.Inner.CONST;

            class Test {}
            """)
        .addOutputLines(
            "b/Test.java",
            """
            package b;

            import static a.Super.CONST;

            class Test {}
            """)
        .doTest();
  }

  @Test
  public void positiveMockitoAny() {
    refactoringHelper
        .addInputLines(
            "b/Test.java",
            """
            package b;

            import static org.mockito.Mockito.any;

            class Test {}
            """)
        .addOutputLines(
            "b/Test.java",
            """
            package b;

            import static org.mockito.ArgumentMatchers.any;

            class Test {}
            """)
        .doTest();
  }

  @Test
  public void negativeMethod() {
    refactoringHelper
        .addInputLines(
            "a/A.java",
            """
            package a;

            public class A {
              public static final int foo() {
                return 42;
              }
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "b/B.java",
            """
            package b;

            import a.A;

            public class B extends A {}
            """)
        .expectUnchanged()
        .addInputLines(
            "b/Test.java",
            """
            package b;

            import static a.A.foo;

            class Test {}
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeField() {
    refactoringHelper
        .addInputLines(
            "a/A.java",
            """
            package a;

            public class A {
              public static final int CONST = 42;
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "b/B.java",
            """
            package b;

            import a.A;

            public class B extends A {}
            """)
        .expectUnchanged()
        .addInputLines(
            "b/Test.java",
            """
            package b;

            import static a.A.CONST;

            class Test {}
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeClassAndField() {
    refactoringHelper
        .addInputLines(
            "a/Super.java",
            """
            package a;

            public class Super {
              public static final int CONST = 42;
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "a/A.java",
            """
            package a;

            public class A {
              public static class Inner extends Super {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "b/B.java",
            """
            package b;

            import a.A;

            public class B extends A {}
            """)
        .expectUnchanged()
        .addInputLines(
            "b/Test.java",
            """
            package b;

            import static a.Super.CONST;

            class Test {}
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void packagePrivateSuperInterface_differentPackage_unchanged() {
    refactoringHelper
        .addInputLines(
            "a/Super.java",
            """
            package a;

            interface Super {
              int CONST = 42;
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "a/Sub.java",
            """
            package a;

            public interface Sub extends Super {}
            """)
        .expectUnchanged()
        .addInputLines(
            "b/Test.java",
            """
            package b;

            import static a.Sub.CONST;

            class Test {
              int x = CONST;
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void packagePrivateSuperInterface_samePackage_importsFromSuperInterface() {
    refactoringHelper
        .addInputLines(
            "a/Super.java",
            """
            package a;

            interface Super {
              int CONST = 42;
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "a/Sub.java",
            """
            package a;

            public interface Sub extends Super {}
            """)
        .expectUnchanged()
        .addInputLines(
            "a/Test.java",
            """
            package a;

            import static a.Sub.CONST;

            class Test {
              int x = CONST;
            }
            """)
        .addOutputLines(
            "a/Test.java",
            """
            package a;

            import static a.Super.CONST;

            class Test {
              int x = CONST;
            }
            """)
        .doTest();
  }

  @Test
  public void packagePrivateSuperClass_publicIntermediateClass_importsFromIntermediateClass() {
    refactoringHelper
        .addInputLines(
            "a/HiddenSuper.java",
            """
            package a;

            class HiddenSuper {
              public static final int CONST = 42;
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "a/Middle.java",
            """
            package a;

            public class Middle extends HiddenSuper {}
            """)
        .expectUnchanged()
        .addInputLines(
            "b/Sub.java",
            """
            package b;

            import a.Middle;

            public class Sub extends Middle {}
            """)
        .expectUnchanged()
        .addInputLines(
            "b/Test.java",
            """
            package b;

            import static b.Sub.CONST;

            class Test {
              int x = CONST;
            }
            """)
        .addOutputLines(
            "b/Test.java",
            """
            package b;

            import static a.Middle.CONST;

            class Test {
              int x = CONST;
            }
            """)
        .doTest();
  }

  @Test
  public void packagePrivateEnclosingClass_differentPackage_unchanged() {
    refactoringHelper
        .addInputLines(
            "a/Outer.java",
            """
            package a;

            class Outer {
              public static class Inner {
                public static final int CONST = 42;
              }
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "a/Sub.java",
            """
            package a;

            public class Sub extends Outer.Inner {}
            """)
        .expectUnchanged()
        .addInputLines(
            "b/Test.java",
            """
            package b;

            import static a.Sub.CONST;

            class Test {
              int x = CONST;
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void inaccessibleSubtypeMethodOverloads_differentPackage_importsFromSuperClass() {
    refactoringHelper
        .addInputLines(
            "a/Super.java",
            """
            package a;

            public class Super {
              public static int foo() {
                return 42;
              }
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "a/Sub.java",
            """
            package a;

            public class Sub extends Super {
              static int foo(int x) {
                return x;
              }

              private static int foo(String s) {
                return 0;
              }
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "b/Test.java",
            """
            package b;

            import static a.Sub.foo;

            class Test {
              int x = foo();
            }
            """)
        .addOutputLines(
            "b/Test.java",
            """
            package b;

            import static a.Super.foo;

            class Test {
              int x = foo();
            }
            """)
        .doTest();
  }

  @Test
  public void packagePrivateSuperClass_publicStaticMemberClass_unchanged() {
    BugCheckerRefactoringTestHelper.newInstance(NonCanonicalStaticImport.class, getClass())
        .addInputLines(
            "a/Super.java",
            """
            package a;

            class Super {
              public static class Inner {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "a/Sub.java",
            """
            package a;

            public class Sub extends Super {}
            """)
        .expectUnchanged()
        .addInputLines(
            "b/Test.java",
            """
            package b;

            import static a.Sub.Inner;

            class Test {
              Inner x;
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void packagePrivateSuperClass_publicIntermediateClass_staticMemberClass() {
    refactoringHelper
        .addInputLines(
            "a/HiddenSuper.java",
            """
            package a;

            class HiddenSuper {
              public static class Inner {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "a/Middle.java",
            """
            package a;

            public class Middle extends HiddenSuper {}
            """)
        .expectUnchanged()
        .addInputLines(
            "b/Sub.java",
            """
            package b;

            import a.Middle;

            public class Sub extends Middle {}
            """)
        .expectUnchanged()
        .addInputLines(
            "b/Test.java",
            """
            package b;

            import static b.Sub.Inner;

            class Test {
              Inner x;
            }
            """)
        .addOutputLines(
            "b/Test.java",
            """
            package b;

            import static a.Middle.Inner;

            class Test {
              Inner x;
            }
            """)
        .doTest();
  }
}
