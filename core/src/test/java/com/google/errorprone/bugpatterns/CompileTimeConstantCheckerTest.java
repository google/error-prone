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
import com.google.errorprone.annotations.CompileTimeConstant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link CompileTimeConstantChecker}Test */
@RunWith(JUnit4.class)
public class CompileTimeConstantCheckerTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(CompileTimeConstantChecker.class, getClass())
          // Disable any javac warnings (ex. [removal]) so that only the CompileTimeConstant
          // warnings are reported.
          .setArgs("-Xlint:none");

  @Test
  public void matches_fieldAccessFailsWithNonConstant() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              public void m(String p, @CompileTimeConstant String q) {}

              public void r(String s) {
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                this.m("boo", s);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_fieldAccessFailsWithNonConstantExpression() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              public void m(String p, @CompileTimeConstant String q) {}

              public void r(String s) {
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                this.m("boo", s + "boo");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_fieldAccessSucceedsWithLiteral() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              public void m(String s, @CompileTimeConstant String p) {}

              public void r(String x) {
                this.m(x, "boo");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_fieldAccessSucceedsWithStaticFinal() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              public static final String S = "Hello";

              public void m(String s, @CompileTimeConstant String p) {}

              public void r(String x) {
                this.m(x, S);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_fieldAccessSucceedsWithConstantConcatenation() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              public static final String S = "Hello";

              public void m(String s, @CompileTimeConstant String p) {}

              public void r(String x) {
                this.m(x, S + " World!");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_identCallFailsWithNonConstant() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              public void m(@CompileTimeConstant String p, int i) {}

              public void r(String s) {
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                m(s, 19);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_identCallSucceedsWithLiteral() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              public void m(String s, @CompileTimeConstant String p) {}

              public void r(@CompileTimeConstant final String x) {
                m(x, x);
              }

              public void s() {
                r("boo");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_staticCallFailsWithNonConstant() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              public static void m(@CompileTimeConstant String p, int i) {}

              public static void r(String s) {
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                m(s, 19);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_staticCallSucceedsWithLiteral() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              public static void m(String s, @CompileTimeConstant String p) {}

              public static void r(String x) {
                m(x, "boo");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_qualifiedStaticCallFailsWithNonConstant() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              public static class Inner {
                public static void m(@CompileTimeConstant String p, int i) {}
              }

              public static void r(String s) {
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                Inner.m(s, 19);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_qualifiedStaticCallSucceedsWithLiteral() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              public static class Inner {
                public static void m(String s, @CompileTimeConstant String p) {}
              }

              public static void r(String x) {
                Inner.m(x, "boo");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_ctorSucceedsWithLiteral() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              public CompileTimeConstantTestCase(String s, @CompileTimeConstant String p) {}

              public static CompileTimeConstantTestCase makeNew(String x) {
                return new CompileTimeConstantTestCase(x, "boo");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_ctorFailsWithNonConstant() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              public CompileTimeConstantTestCase(String s, @CompileTimeConstant String p) {}

              public static CompileTimeConstantTestCase makeNew(String x) {
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                return new CompileTimeConstantTestCase("boo", x);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_identCallSucceedsWithinCtorWithLiteral() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              public CompileTimeConstantTestCase(String s, @CompileTimeConstant final String p) {
                m(p);
              }

              public void m(@CompileTimeConstant String r) {}

              public static CompileTimeConstantTestCase makeNew(String x) {
                return new CompileTimeConstantTestCase(x, "boo");
              }
            }
            """)
        .doTest();
  }

  /** Holder for a method we wish to reference from a test. */
  public static final class Holder {
    public static void m(String s, @CompileTimeConstant String... p) {}

    private Holder() {}
  }

  @Test
  public void matches_varargsInDifferentCompilationUnit() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "import " + Holder.class.getCanonicalName() + ";",
            "public class CompileTimeConstantTestCase {",
            "  public static void r(String s) {",
            "    // BUG: Diagnostic contains: Non-compile-time constant expression passed",
            "    Holder.m(s, \"foo\", s);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void matches_varargsSuccess() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              public static void m(String s, @CompileTimeConstant String... p) {}

              public static void r(String s) {
                m(s);
                m(s, "foo");
                m(s, "foo", "bar");
                m(s, "foo", "bar", "baz");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_effectivelyFinalCompileTimeConstantParam() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              public static void m(@CompileTimeConstant String y) {}

              public static void r(@CompileTimeConstant String x) {
                m(x);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_nonFinalCompileTimeConstantParam() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              public static void m(@CompileTimeConstant String y) {}

              public static void r(@CompileTimeConstant String x) {
                x = x + "!";
                // BUG: Diagnostic contains: . Did you mean to make 'x' final?
                m(x);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_override() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            abstract class CompileTimeConstantTestCase {
              abstract void m(String y);

              static class C extends CompileTimeConstantTestCase {
                @Override
                // BUG: Diagnostic contains: Method with @CompileTimeConstant parameter
                void m(@CompileTimeConstant String s) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_methodReference() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;
            import java.util.function.Consumer;

            public class CompileTimeConstantTestCase {
              public static void m(@CompileTimeConstant String s) {}

              public static Consumer<String> r(String x) {
                // BUG: Diagnostic contains: Method with @CompileTimeConstant parameter
                return CompileTimeConstantTestCase::m;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_constructorReference() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;
            import java.util.function.Function;

            public class CompileTimeConstantTestCase {
              CompileTimeConstantTestCase(@CompileTimeConstant String s) {}

              public static Function<String, CompileTimeConstantTestCase> r(String x) {
                // BUG: Diagnostic contains: Method with @CompileTimeConstant parameter
                return CompileTimeConstantTestCase::new;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_methodReferenceCorrectOverrideMethod() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              interface ConstantFn {
                void apply(@CompileTimeConstant String s);
              }

              public static void m(@CompileTimeConstant String s) {}

              public static ConstantFn r(String x) {
                return CompileTimeConstantTestCase::m;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_methodReferenceCorrectOverrideConstructor() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              interface ConstantFn {
                CompileTimeConstantTestCase apply(@CompileTimeConstant String s);
              }

              CompileTimeConstantTestCase(@CompileTimeConstant String s) {}

              public static ConstantFn r(String x) {
                return CompileTimeConstantTestCase::new;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void matches_lambdaExpression() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;
            import java.util.function.Consumer;

            public class CompileTimeConstantTestCase {
              // BUG: Diagnostic contains: Method with @CompileTimeConstant parameter
              Consumer<String> c = (@CompileTimeConstant String s) -> {};
            }
            """)
        .doTest();
  }

  @Test
  public void doesNotMatch_lambdaExpression_correctOverride() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              interface ConstantFn {
                void apply(@CompileTimeConstant String s);
              }

              ConstantFn c =
                  (@CompileTimeConstant String s) -> {
                    doFoo(s);
                  };

              void doFoo(final @CompileTimeConstant String foo) {}
            }
            """)
        .doTest();
  }

  @Test
  public void matches_lambdaExpressionWithoutAnnotatedParameters() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              interface ConstantFn {
                void apply(@CompileTimeConstant String s);
              }

              ConstantFn c =
                  s -> {
                    // BUG: Diagnostic contains: Non-compile-time constant expression
                    doFoo(s);
                  };

              void doFoo(final @CompileTimeConstant String foo) {}
            }
            """)
        .doTest();
  }

  @Test
  public void matches_lambdaExpressionWithoutExplicitFormalParameters() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              @FunctionalInterface
              interface I {
                void f(@CompileTimeConstant String x);
              }

              void f(String s) {
                I i = x -> {};
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                i.f(s);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void reportsDiagnostic_whenConstantFieldDeclaredWithoutFinal() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              // BUG: Diagnostic contains: . Did you mean to make 's' final?
              @CompileTimeConstant String s = "s";
            }
            """)
        .doTest();
  }

  @Test
  public void noDiagnostic_whenConstantFieldDeclaredFinal() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              @CompileTimeConstant final String s = "s";
            }
            """)
        .doTest();
  }

  @Test
  public void reportsDiagnostic_whenInitialisingFinalFieldWithNonConstant() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              @CompileTimeConstant final String s;

              CompileTimeConstantTestCase(String s) {
                // BUG: Diagnostic contains: Non-compile-time constant expression
                this.s = s;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void noDiagnostic_whenInitialisingFinalFieldWithConstant() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              @CompileTimeConstant final String s;

              CompileTimeConstantTestCase(@CompileTimeConstant String s) {
                this.s = s;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void noDiagnostic_whenInvokingMethodWithFinalField() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public class CompileTimeConstantTestCase {
              @CompileTimeConstant final String s;

              CompileTimeConstantTestCase(@CompileTimeConstant String s) {
                this.s = s;
              }

              void invokeCTCMethod() {
                ctcMethod(s);
              }

              void ctcMethod(@CompileTimeConstant String s) {}
            }
            """)
        .doTest();
  }

  @Test
  public void reportsDiagnostic_whenConstantEnumFieldDeclaredWithoutFinal() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CompileTimeConstant;

            public enum Test {
              A("A");
              // BUG: Diagnostic contains: . Did you mean to make 's' final?
              @CompileTimeConstant String s;

              Test(@CompileTimeConstant String s) {
                this.s = s;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void noDiagnostic_whenConstantEnumFieldDeclaredFinal() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CompileTimeConstant;

            public enum Test {
              A("A");
              @CompileTimeConstant final String s;

              Test(@CompileTimeConstant String s) {
                this.s = s;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void reportsDiagnostic_whenInitialisingFinalEnumFieldWithNonConstant() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CompileTimeConstant;

            public enum Test {
              A("A");
              @CompileTimeConstant final String s;

              Test(String s) {
                // BUG: Diagnostic contains: Non-compile-time constant expression
                this.s = s;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void noDiagnostic_whenInvokingMethodWithFinalEnumField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CompileTimeConstant;

            public enum Test {
              A("A");
              @CompileTimeConstant final String s;

              Test(@CompileTimeConstant String s) {
                this.s = s;
              }

              void invokeCTCMethod() {
                ctcMethod(s);
              }

              void ctcMethod(@CompileTimeConstant String s) {}
            }
            """)
        .doTest();
  }

  @Test
  public void nonConstantField_positive() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;

            public abstract class CompileTimeConstantTestCase {
              abstract String something();

              // BUG: Diagnostic contains: Non-compile-time constant expression
              @CompileTimeConstant final String x = something();
            }
            """)
        .doTest();
  }

  @Test
  public void constantField_immutableList() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.common.collect.ImmutableList;
            import com.google.errorprone.annotations.CompileTimeConstant;

            public abstract class CompileTimeConstantTestCase {
              @CompileTimeConstant final ImmutableList<String> x = ImmutableList.of("a");
            }
            """)
        .doTest();
  }

  @Test
  public void constantField_collectionTypes() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.common.collect.ImmutableMap;
            import com.google.common.collect.ImmutableMultiset;
            import com.google.common.collect.ImmutableSet;
            import com.google.common.collect.ImmutableSortedMap;
            import com.google.common.collect.ImmutableSortedSet;
            import com.google.errorprone.annotations.CompileTimeConstant;
            import java.util.List;
            import java.util.Map;
            import java.util.Set;

            public abstract class CompileTimeConstantTestCase {
              @CompileTimeConstant final ImmutableSet<String> immutableSet = ImmutableSet.of("a");
              @CompileTimeConstant final ImmutableMap<String, Integer> immutableMap =
                  ImmutableMap.of("a", 1);
              @CompileTimeConstant final ImmutableMultiset<String> immutableMultiset =
                  ImmutableMultiset.of("a");
              @CompileTimeConstant final ImmutableSortedSet<String> immutableSortedSet =
                  ImmutableSortedSet.of("a");
              @CompileTimeConstant final ImmutableSortedMap<String, Integer> immutableSortedMap =
                  ImmutableSortedMap.of("a", 1);
              @CompileTimeConstant final List<String> jdkList = List.of("a");
              @CompileTimeConstant final Set<String> jdkSet = Set.of("a");
              @CompileTimeConstant final Map<String, Integer> jdkMap = Map.of("a", 1);
            }
            """)
        .doTest();
  }

  @Test
  public void methodCall_collectionTypes() {
    compilationHelper
        .addSourceLines(
            "test/CompileTimeConstantTestCase.java",
            """
            package test;

            import com.google.common.collect.ImmutableMap;
            import com.google.common.collect.ImmutableMultiset;
            import com.google.common.collect.ImmutableSet;
            import com.google.common.collect.ImmutableSortedMap;
            import com.google.common.collect.ImmutableSortedSet;
            import com.google.errorprone.annotations.CompileTimeConstant;
            import java.util.List;
            import java.util.Map;
            import java.util.Set;

            public class CompileTimeConstantTestCase {
              public static void acceptObject(@CompileTimeConstant Object obj) {}

              public static void testSuccess(@CompileTimeConstant final String constParam) {
                acceptObject(ImmutableSet.of("a", constParam));
                acceptObject(ImmutableMap.of("a", 1, "b", 2));
                acceptObject(ImmutableMultiset.of("a", "b"));
                acceptObject(ImmutableSortedSet.of("a", "b"));
                acceptObject(ImmutableSortedMap.of("a", 1));
                acceptObject(List.of("a", constParam));
                acceptObject(Set.of("a", "b"));
                acceptObject(Map.of("a", List.of(1, 2)));
              }

              public static void testFailure(String nonConst) {
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(ImmutableMap.of("a", nonConst));
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(List.of(nonConst));
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(Set.of("a", nonConst));
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(Map.of(nonConst, 1));
              }
            }
            """)
        .doTest();
  }

  @Test
  public void switchExpression_allBranchesConstant() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CompileTimeConstant;

            public class Test {
              @CompileTimeConstant
              final String s =
                  switch (1) {
                    case 1 -> "a";
                    case 2 -> "b";
                    default -> "c";
                  };
            }
            """)
        .doTest();
  }

  @Test
  public void switchExpression_notAllBranchesConstant() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CompileTimeConstant;

            public class Test {
              @CompileTimeConstant
              final String s =
                  // BUG: Diagnostic contains:
                  switch (1) {
                    case 1 -> "a";
                    case 2 -> toString();
                    default -> "c";
                  };
            }
            """)
        .doTest();
  }

  @Test
  public void switchExpression_onlyConsiderReturningBranches() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CompileTimeConstant;

            public class Test {
              @CompileTimeConstant
              final String s =
                  switch (1) {
                    case 1 -> "a";
                    case 2 -> "b";
                    default -> throw new RuntimeException();
                  };
            }
            """)
        .doTest();
  }

  @Test
  public void recordAndInterfaceFields() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;
            import com.google.errorprone.annotations.CompileTimeConstant;

            public class Test {
              interface ConstInterface {
                @CompileTimeConstant ImmutableList<String> LIST = ImmutableList.of("a");
              }

              record GoodRecord(@CompileTimeConstant String s) {
                void invoke(GoodRecord other) {
                  ctcMethod(s);
                  ctcMethod(this.s);
                  ctcMethod(other.s);
                  ctcListMethod(ConstInterface.LIST);
                }
              }

              record BadRecord(@CompileTimeConstant String s) {
                BadRecord(String s) {
                  // BUG: Diagnostic contains: Non-compile-time constant expression
                  this.s = s;
                }
              }

              static void ctcMethod(@CompileTimeConstant String s) {}

              static void ctcListMethod(@CompileTimeConstant ImmutableList<String> list) {}

              static void testRecordConstruction(String nonConst) {
                new GoodRecord("const");
                // BUG: Diagnostic contains: Non-compile-time constant expression
                new GoodRecord(nonConst);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void qualifiedCompileTimeConstantFieldAccess() {
    compilationHelper
        .addSourceLines(
            "Test.java",
"""
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CompileTimeConstant;

public class Test {
  static class Other {
    @CompileTimeConstant static final ImmutableList<String> LIST = ImmutableList.of("a");
  }

  @CompileTimeConstant final String s;

  Test(@CompileTimeConstant String s) {
    this.s = s;
  }

  void invoke(Test other) {
    ctcMethod(this.s);
    ctcMethod(other.s);
    ctcListMethod(Other.LIST);
  }

  static void ctcMethod(@CompileTimeConstant String s) {}

  static void ctcListMethod(@CompileTimeConstant ImmutableList<String> list) {}
}
""")
        .doTest();
  }

  @Test
  public void classLiteralsAndEnumConstants() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            package test;

            import com.google.common.collect.ImmutableList;
            import com.google.errorprone.annotations.CompileTimeConstant;
            import java.time.DayOfWeek;

            public class Test {
              enum MyEnum {
                A,
                B
              }

              @CompileTimeConstant final Class<?> clazz = String.class;
              @CompileTimeConstant final Class<?> primitiveClazz = int.class;
              @CompileTimeConstant final MyEnum enumConst = MyEnum.A;
              @CompileTimeConstant final String enumName = MyEnum.A.name();

              // BUG: Diagnostic contains: Non-compile-time constant expression
              @CompileTimeConstant final String enumToString = MyEnum.A.toString();

              public static void acceptObject(@CompileTimeConstant Object obj) {}

              public static void testSuccess() {
                acceptObject(String.class);
                acceptObject(int.class);
                acceptObject(void.class);
                acceptObject(String[].class);
                acceptObject(ImmutableList.of(String.class, int.class));
                acceptObject(DayOfWeek.MONDAY);
                acceptObject(MyEnum.B);
                acceptObject(MyEnum.B.name());
              }

              public static void testFailure(Class<?> runtimeClass, MyEnum runtimeEnum) {
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(runtimeClass);
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(runtimeEnum);
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(MyEnum.B.toString());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void compatibleMethods() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;
            import java.math.BigInteger;
            import java.nio.charset.Charset;
            import java.nio.charset.StandardCharsets;
            import java.time.Duration;
            import java.time.LocalDate;
            import java.time.ZoneId;
            import java.time.format.DateTimeFormatter;
            import java.util.Locale;
            import java.util.Objects;
            import java.util.Optional;

            public class Test {
              @CompileTimeConstant final Duration duration = Duration.ofSeconds(10).plusMinutes(1);
              @CompileTimeConstant final String formattedDate =
                  LocalDate.of(2025, 1, 1).format(DateTimeFormatter.ISO_LOCAL_DATE);

              public static void acceptObject(@CompileTimeConstant Object obj) {}

              public static void testSuccess(@CompileTimeConstant final String constStr) {
                acceptObject(Integer.valueOf(42));
                acceptObject(String.valueOf(42));
                acceptObject(constStr.trim().toLowerCase(Locale.ROOT));
                acceptObject(Math.max(1, 2));
                acceptObject(Optional.of(constStr).orElse("fallback"));
                acceptObject(Objects.requireNonNull(constStr));
                acceptObject(StandardCharsets.UTF_8.name());
              }

              public static void testFailure(
                  String nonConstStr,
                  @CompileTimeConstant final char[] constCharArray,
                  @CompileTimeConstant final BigInteger constBigInt) {
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(nonConstStr.trim());
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject("abc".substring(nonConstStr.length()));
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject("abc".intern());
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject("abc".toLowerCase());
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(String.valueOf(constCharArray));
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(Character.codePointAt(constCharArray, 0));
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(ZoneId.of("UTC"));
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(Charset.forName("UTF-8"));
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(constBigInt.add(BigInteger.ONE));
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(Objects.toString(constBigInt));
              }
            }
            """)
        .doTest();
  }

  @Test
  public void compatibleConstructors() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;
            import java.math.BigDecimal;
            import java.math.BigInteger;
            import java.math.MathContext;
            import java.math.RoundingMode;
            import java.util.Locale;
            import java.util.UUID;

            public class Test {
              @CompileTimeConstant final BigInteger bigInt = new BigInteger("12345");
              @CompileTimeConstant final BigDecimal bigDec =
                  new BigDecimal("12.34", new MathContext(10, RoundingMode.HALF_UP));

              public static void acceptObject(@CompileTimeConstant Object obj) {}

              public static void testSuccess(@CompileTimeConstant final String constStr) {
                acceptObject(new String());
                acceptObject(new String(constStr));
                acceptObject(new Integer(42));
                acceptObject(new BigInteger("ff", 16));
                acceptObject(new BigDecimal(100L));
                acceptObject(new Locale("en", "US"));
                acceptObject(new UUID(0L, 1L));
              }

              public static void testFailure(
                  String nonConstStr, @CompileTimeConstant final char[] constCharArray) {
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(new String(nonConstStr));
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(new String(constCharArray));
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(new BigInteger(nonConstStr));
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(new BigInteger("123") {});
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(new BigDecimal(BigInteger.ONE));
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(new Object());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void compatibleFields() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            package test;

            import com.google.errorprone.annotations.CompileTimeConstant;
            import java.math.BigDecimal;
            import java.math.BigInteger;
            import java.math.MathContext;
            import java.nio.charset.Charset;
            import java.nio.charset.StandardCharsets;
            import java.time.Duration;
            import java.time.Instant;
            import java.time.LocalDate;
            import java.time.ZoneOffset;
            import java.time.format.DateTimeFormatter;
            import java.util.Collections;
            import java.util.Locale;

            public class Test {
              private static final Object UNANNOTATED_STATIC_FINAL = new Object();

              @CompileTimeConstant final Charset utf8 = StandardCharsets.UTF_8;
              @CompileTimeConstant final Instant epoch = Instant.EPOCH;
              @CompileTimeConstant final Locale rootLocale = Locale.ROOT;

              public static void acceptObject(@CompileTimeConstant Object obj) {}

              public static void testSuccess() {
                acceptObject(Boolean.TRUE);
                acceptObject(Integer.TYPE);
                acceptObject(String.CASE_INSENSITIVE_ORDER);
                acceptObject(BigInteger.ZERO);
                acceptObject(BigDecimal.TEN);
                acceptObject(MathContext.DECIMAL64);
                acceptObject(Duration.ZERO);
                acceptObject(LocalDate.EPOCH);
                acceptObject(ZoneOffset.UTC);
                acceptObject(DateTimeFormatter.ISO_INSTANT);
                acceptObject(Collections.EMPTY_LIST);
                acceptObject(StandardCharsets.UTF_8);
              }

              public static void testFailure() {
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(System.out);
                // BUG: Diagnostic contains: Non-compile-time constant expression passed
                acceptObject(UNANNOTATED_STATIC_FINAL);
              }
            }
            """)
        .doTest();
  }
}
