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

package com.google.errorprone.matchers;

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.inLoop;
import static com.google.errorprone.matchers.Matchers.isPrimitiveOrVoidType;
import static com.google.errorprone.matchers.Matchers.isPrimitiveType;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.isVoidType;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;
import static org.junit.Assert.assertThrows;

import com.google.errorprone.BugPattern;
import com.google.errorprone.ClassName;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.MatcherChecker;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.field.FieldMatchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.matchers.method.MethodMatchers.MethodClassMatcher;
import com.google.errorprone.predicates.TypePredicates;
import com.google.errorprone.scanner.ErrorProneScanner;
import com.google.errorprone.scanner.ScannerSupplier;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link Matchers}. */
@RunWith(JUnit4.class)
public class MatchersTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(InLoopChecker.class, getClass());

  @Test
  public void methodNameWithParenthesisThrows() {
    MethodClassMatcher methodClassMatcher =
        Matchers.instanceMethod().onExactClass("java.lang.String");
    assertThrows(IllegalArgumentException.class, () -> methodClassMatcher.named("getBytes()"));
    assertThrows(IllegalArgumentException.class, () -> methodClassMatcher.named("getBytes)"));
    assertThrows(IllegalArgumentException.class, () -> methodClassMatcher.named("getBytes("));
  }

  @Test
  public void hasMetaAnnotation() {
    CompilationTestHelper.newInstance(HasMetaAnnotationChecker.class, getClass())
        .addSourceLines(
            "Annotations.java",
            """
            @interface Meta {}

            @Meta
            @interface Composed {}

            @interface Plain {}
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: HasMetaAnnotation
              @Composed
              void composed() {}

              @Plain
              void plain() {}
            }
            """)
        .doTest();
  }

  @Test
  public void inLoopShouldMatchInWhileLoop() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            public class Test {
              public void doIt() {
                while (true) {
                  // BUG: Diagnostic contains:
                  System.out.println();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void inLoopShouldMatchInDoLoop() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            public class Test {
              public void doIt() {
                do {
                  // BUG: Diagnostic contains:
                  System.out.println();
                } while (true);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void inLoopShouldMatchInForLoop() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            public class Test {
              public void doIt() {
                for (; true; ) {
                  // BUG: Diagnostic contains:
                  System.out.println();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void inLoopShouldMatchInEnhancedForLoop() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            public class Test {
              public void doIt(List<String> strings) {
                for (String s : strings) {
                  // BUG: Diagnostic contains:
                  System.out.println();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void inLoopShouldNotMatchInInitializerWithoutLoop() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            public class Test {
              {
                System.out.println();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void inLoopShouldMatchInInitializerInLoop() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            public class Test {
              {
                int count = 0;
                while (count < 10) {
                  // BUG: Diagnostic contains:
                  System.out.println();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void inLoopShouldNotMatchInAnonymousInnerClassDefinedInLoop() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.*;

            public class Test {
              public void sort(List<List<String>> stringLists) {
                for (List<String> stringList : stringLists) {
                  Collections.sort(
                      stringList,
                      new Comparator<String>() {
                        {
                          System.out.println();
                        }

                        public int compare(String s1, String s2) {
                          return 0;
                        }

                        public boolean equals(Object obj) {
                          return false;
                        }
                      });
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void methodWithClassAndName() {
    Matcher<MethodTree> matcher =
        Matchers.methodWithClassAndName("com.google.errorprone.foo.bar.Test", "myMethod");
    CompilationTestHelper.newInstance(methodTreeCheckerSupplier(matcher), getClass())
        .addSourceLines(
            "com/google/errorprone/foo/bar/Test.java",
            """
            package com.google.errorprone.foo.bar;

            public class Test {
              // BUG: Diagnostic contains:
              public void myMethod() {}
            }
            """)
        .doTest();
  }

  @Test
  public void methodReturnsSubtype() {
    Matcher<MethodTree> matcher = methodReturns(isSubtypeOf("java.util.List"));
    CompilationTestHelper.newInstance(methodTreeCheckerSupplier(matcher), getClass())
        .addSourceLines(
            "test/MethodReturnsSubtypeTest.java",
            """
            package test;

            public class MethodReturnsSubtypeTest {
              // BUG: Diagnostic contains:
              public java.util.ArrayList<String> matches() {
                return null;
              }

              public java.util.HashSet<String> doesntMatch() {
                return null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void methodReturnsType() {
    Matcher<MethodTree> matcher = methodReturns(typeFromString("java.lang.Number"));
    CompilationTestHelper.newInstance(methodTreeCheckerSupplier(matcher), getClass())
        .addSourceLines(
            "test/MethodReturnsSubtypeTest.java",
            """
            package test;

            public class MethodReturnsSubtypeTest {
              public java.lang.Integer doesntMatch() {
                return 0;
              }

              // BUG: Diagnostic contains:
              public java.lang.Number matches() {
                return 0;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void methodReturnsPrimitiveType() {
    Matcher<MethodTree> matcher = methodReturns(isPrimitiveType());
    CompilationTestHelper.newInstance(methodTreeCheckerSupplier(matcher), getClass())
        .addSourceLines(
            "test/MethodReturnsPrimitiveTypeTest.java",
            """
            package test;

            public class MethodReturnsPrimitiveTypeTest {
              // BUG: Diagnostic contains:
              public boolean matches1() {
                return false;
              }

              public void doesntMatch() {}

              // BUG: Diagnostic contains:
              public int matches2() {
                return 42;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void methodReturnsVoidType() {
    Matcher<MethodTree> matcher =
        Matchers.allOf(methodReturns(typeFromString("void")), methodReturns(isVoidType()));
    CompilationTestHelper.newInstance(methodTreeCheckerSupplier(matcher), getClass())
        .addSourceLines(
            "test/MethodReturnsVoidTypeTest.java",
            """
            package test;

            public class MethodReturnsVoidTypeTest {
              public boolean doesntMatch1() {
                return true;
              }

              public Object doesntMatch2() {
                return Integer.valueOf(42);
              }

              // BUG: Diagnostic contains:
              public void matches() {
                System.out.println(42);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void methodReturnsPrimitiveOrVoidType() {
    Matcher<MethodTree> matcher = methodReturns(isPrimitiveOrVoidType());
    CompilationTestHelper.newInstance(methodTreeCheckerSupplier(matcher), getClass())
        .addSourceLines(
            "test/MethodReturnsPrimitiveOrVoidTypeTest.java",
            """
            package test;

            public class MethodReturnsPrimitiveOrVoidTypeTest {
              public Object doesntMatch() {
                return null;
              }

              // BUG: Diagnostic contains:
              public boolean doesMatch1() {
                return true;
              }

              // BUG: Diagnostic contains:
              public void doesMatch2() {}

              // BUG: Diagnostic contains:
              public double doesMatch3() {
                return 42.0;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void methodReturnsNonPrimitiveType() {
    Matcher<MethodTree> matcher = Matchers.methodReturnsNonPrimitiveType();
    CompilationTestHelper.newInstance(methodTreeCheckerSupplier(matcher), getClass())
        .addSourceLines(
            "test/MethodReturnsSubtypeTest.java",
            """
            package test;

            public class MethodReturnsSubtypeTest {
              public int doesntMatch() {
                return 0;
              }

              public void doesntMatch2() {}

              // BUG: Diagnostic contains:
              public String matches() {
                return "";
              }
            }
            """)
        .doTest();
  }

  @Test
  public void methodInvocationDoesntMatchAnnotation() {
    CompilationTestHelper.newInstance(NoAnnotatedCallsChecker.class, getClass())
        .addSourceLines(
            "test/MethodInvocationDoesntMatchAnnotation.java",
            """
            package test;

            public class MethodInvocationDoesntMatchAnnotation {
              @Deprecated
              public void matches() {}

              public void doesntMatch() {
                matches();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void methodInvocationHasDeclarationAnnotation() {
    CompilationTestHelper.newInstance(NoAnnotatedDeclarationCallsChecker.class, getClass())
        .addSourceLines(
            "test/MethodInvocationMatchesDeclAnnotation.java",
            """
            package test;

            public class MethodInvocationMatchesDeclAnnotation {
              @Deprecated
              public void matches() {}

              public void callsMatch() {
                // BUG: Diagnostic contains:
                matches();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void sameArgumentGoesOutOfBounds() {
    CompilationTestHelper compilationTestHelper =
        CompilationTestHelper.newInstance(SameArgumentChecker.class, getClass())
            .addSourceLines(
                "test/SameArgumentCheckerTest.java",
                """
                package test;

                public class SameArgumentCheckerTest {
                  public void matches(Object... args) {}

                  public void callsMatch() {
                    Object obj = new Object();
                    matches(obj, "some arg");
                  }
                }
                """);
    AssertionError thrown =
        assertThrows(AssertionError.class, () -> compilationTestHelper.doTest());
    assertThat(thrown).hasMessageThat().contains("IndexOutOfBoundsException");
  }

  @Test
  public void booleanConstantMatchesTrue() {
    CompilationTestHelper.newInstance(BooleanConstantTrueChecker.class, getClass())
        .addSourceLines(
            "test/BooleanConstantTrueCheckerTest.java",
            """
            package test;

            public class BooleanConstantTrueCheckerTest {
              public void function() {
                // BUG: Diagnostic contains:
                method(Boolean.TRUE);
                method(Boolean.FALSE);
              }

              void method(Object value) {}
            }
            """)
        .doTest();
  }

  @Test
  public void packageStartsWith() {
    assertThrows(NullPointerException.class, () -> Matchers.packageStartsWith(null));
    assertThrows(IllegalArgumentException.class, () -> Matchers.packageStartsWith(""));

    CompilationTestHelper.newInstance(PackageNameChecker.class, getClass())
        .addSourceLines(
            "test/foo/ClassName.java",
            """
            package test.foo;

            // BUG: Diagnostic contains:
            public class ClassName {}
            """)
        .doTest();

    CompilationTestHelper.newInstance(PackageNameChecker.class, getClass())
        .addSourceLines(
            "test/foo/ClassName.java",
            "package testyfoo;",
            // No match, the "." is escaped correctly in the regex "test.foo".
            "public class ClassName {}")
        .doTest();

    CompilationTestHelper.newInstance(PackageNameChecker.class, getClass())
        .addSourceLines(
            "test/foo/bar/ClassName.java",
            """
            package test.foo.bar;

            // BUG: Diagnostic contains:
            public class ClassName {}
            """)
        .doTest();

    CompilationTestHelper.newInstance(PackageNameChecker.class, getClass())
        .addSourceLines(
            "test/ClassName.java", // Do not wrap.
            "package test;",
            "public class ClassName {}")
        .doTest();

    CompilationTestHelper.newInstance(PackageNameChecker.class, getClass())
        .addSourceLines(
            "test/foobar/ClassName.java",
            """
            package test.foobar;

            // No match, the package is not "test.foo" and is not in a subpackage of "test.foo".
            public class ClassName {}
            """)
        .doTest();
  }

  @BugPattern(
      summary = "Checker that flags the given expression statement if the given matcher matches",
      severity = ERROR)
  public static class InLoopChecker extends MatcherChecker {
    public InLoopChecker() {
      super("System.out.println();", inLoop());
    }
  }

  private static ScannerSupplier methodTreeCheckerSupplier(Matcher<MethodTree> matcher) {
    return ScannerSupplier.fromScanner(new ErrorProneScanner(new MethodTreeChecker(matcher)));
  }

  @BugPattern(
      summary = "Checker that flags the given method declaration if the given matcher matches",
      severity = ERROR)
  static class MethodTreeChecker extends BugChecker implements MethodTreeMatcher {
    private final Matcher<MethodTree> matcher;

    public MethodTreeChecker(Matcher<MethodTree> matcher) {
      this.matcher = matcher;
    }

    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
      return matcher.matches(tree, state) ? describeMatch(tree) : Description.NO_MATCH;
    }
  }

  /** Simple checker to make sure hasAnnotation doesn't match on MethodInvocationTree. */
  @BugPattern(
      summary = "Checker that flags the given method invocation if the given matcher matches",
      severity = ERROR)
  public static class NoAnnotatedCallsChecker extends BugChecker
      implements MethodInvocationTreeMatcher {
    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      if (Matchers.hasAnnotation("java.lang.Deprecated").matches(tree, state)) {
        return describeMatch(tree);
      }
      return Description.NO_MATCH;
    }
  }

  /** Simple checker to make sure sameArgument doesn't throw IndexOutOfBoundsException. */
  @BugPattern(
      summary = "Checker that matches invocation if the first argument is repeated",
      severity = ERROR)
  public static class SameArgumentChecker extends BugChecker
      implements MethodInvocationTreeMatcher {
    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      // intentionally go above arg size.
      for (int i = 1; i <= tree.getArguments().size(); i++) {
        if (Matchers.sameArgument(0, i).matches(tree, state)) {
          return describeMatch(tree);
        }
      }
      return Description.NO_MATCH;
    }
  }

  /** Checker that makes sure symbolHasAnnotation matches on MethodInvocationTree. */
  @BugPattern(
      summary = "Checker that flags the given method invocation if the given matcher matches",
      severity = ERROR)
  public static class NoAnnotatedDeclarationCallsChecker extends BugChecker
      implements MethodInvocationTreeMatcher {
    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      if (Matchers.symbolHasAnnotation("java.lang.Deprecated").matches(tree, state)) {
        return describeMatch(tree);
      }
      return Description.NO_MATCH;
    }
  }

  /** Checker that checks if a class is in a particular package. */
  @BugPattern(summary = "Checks the name of the package", severity = ERROR)
  public static class PackageNameChecker extends BugChecker implements ClassTreeMatcher {
    private static final Matcher<Tree> MATCHER = Matchers.packageStartsWith("test.foo");

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      return MATCHER.matches(tree, state) ? describeMatch(tree) : Description.NO_MATCH;
    }
  }

  /** Checker that checks if an argument is 'Boolean.TRUE'. */
  @BugPattern(summary = "BooleanConstantTrueChecker", severity = ERROR)
  public static class BooleanConstantTrueChecker extends BugChecker
      implements MethodInvocationTreeMatcher {

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      if (Matchers.methodInvocation(
              MethodMatchers.anyMethod(),
              ChildMultiMatcher.MatchType.AT_LEAST_ONE,
              Matchers.booleanConstant(true))
          .matches(tree, state)) {
        return describeMatch(tree);
      }
      return Description.NO_MATCH;
    }
  }

  /** Checker that flags annotations matched by {@link Matchers#hasMetaAnnotation(String)}. */
  @BugPattern(summary = "Checker that flags meta-annotated annotations", severity = ERROR)
  public static class HasMetaAnnotationChecker extends BugChecker implements AnnotationTreeMatcher {
    @Override
    public Description matchAnnotation(AnnotationTree tree, VisitorState state) {
      return Matchers.hasMetaAnnotation("Meta").matches(tree, state)
          ? this.buildDescription(tree).setMessage("HasMetaAnnotation").build()
          : Description.NO_MATCH;
    }
  }

  @Test
  public void annotationTypeNullThrows() {
    assertThrows(NullPointerException.class, () -> new AnnotationType((String) null));
    assertThrows(NullPointerException.class, () -> new AnnotationType((ClassName) null));
  }

  @Test
  public void hasAnnotation_className() {
    assertMatcher(
            ClassTree.class,
            Matchers.anyOf(
                Matchers.hasAnnotation(ClassName.of("pkg.Outer.InheritedAnno")),
                Matchers.hasAnnotation(ClassName.of("com.example.Absent$Anno"))))
        .addSourceLines(
            "pkg/Outer.java",
            """
            package pkg;

            import java.lang.annotation.Inherited;

            public class Outer {
              @Inherited
              public @interface InheritedAnno {}
            }
            """)
        .addSourceLines(
            "pkg/Test.java",
            """
            package pkg;

            @Outer.InheritedAnno
            // BUG: Diagnostic contains:
            class Base {}

            // BUG: Diagnostic contains:
            class Sub extends Base {}

            class Unannotated {}
            """)
        .doTest();
  }

  @Test
  public void hasAnyAnnotation_className() {
    assertMatcher(
            MethodTree.class,
            Matchers.hasAnyAnnotation(
                ClassName.of("com.example.Absent$Anno"),
                ClassName.of("pkg.Outer$FirstAnno"),
                ClassName.of("pkg.Outer.SecondAnno")))
        .addSourceLines(
            "pkg/Outer.java",
            """
            package pkg;

            public class Outer {
              public @interface FirstAnno {}

              public @interface SecondAnno {}
            }
            """)
        .addSourceLines(
            "pkg/Test.java",
            """
            package pkg;

            class Test {
              @Outer.FirstAnno
              // BUG: Diagnostic contains:
              void first() {}

              @Outer.SecondAnno
              // BUG: Diagnostic contains:
              void second() {}

              void unannotated() {}
            }
            """)
        .doTest();
  }

  @Test
  public void symbolHasAnnotation_className() {
    assertMatcher(
            MethodInvocationTree.class,
            Matchers.anyOf(
                Matchers.symbolHasAnnotation(ClassName.of("pkg.Outer$NestedAnno")),
                Matchers.symbolHasAnnotation(ClassName.of("com.example.Absent$Anno"))))
        .addSourceLines(
            "pkg/Outer.java",
            """
            package pkg;

            public class Outer {
              public @interface NestedAnno {}
            }
            """)
        .addSourceLines(
            "pkg/Test.java",
            """
            package pkg;

            class Test {
              @Outer.NestedAnno
              void annotated() {}

              void unannotated() {}

              void calls() {
                // BUG: Diagnostic contains:
                annotated();
                unannotated();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void hasAnnotationOnAnyOverriddenMethod_className() {
    assertMatcher(
            MethodTree.class,
            Matchers.anyOf(
                Matchers.hasAnnotationOnAnyOverriddenMethod(ClassName.of("pkg.Outer$NestedAnno")),
                Matchers.hasAnnotationOnAnyOverriddenMethod(ClassName.of("com.example.Absent"))))
        .addSourceLines(
            "pkg/Outer.java",
            """
            package pkg;

            public class Outer {
              public @interface NestedAnno {}
            }
            """)
        .addSourceLines(
            "pkg/Test.java",
            """
            package pkg;

            class Base {
              @Outer.NestedAnno
              // BUG: Diagnostic contains:
              void overridden() {}
            }

            class Sub extends Base {
              @Override
              // BUG: Diagnostic contains:
              void overridden() {}

              void other() {}
            }
            """)
        .doTest();
  }

  @Test
  public void isTypeAndHasMetaAnnotation_className() {
    assertMatcher(
            AnnotationTree.class,
            Matchers.allOf(
                Matchers.isType(ClassName.of("pkg.Outer.Composed")),
                Matchers.hasMetaAnnotation(ClassName.of("pkg.Outer.Meta")),
                Matchers.not(Matchers.isType(ClassName.of("com.example.Absent"))),
                Matchers.not(Matchers.hasMetaAnnotation(ClassName.of("com.example.Absent")))))
        .addSourceLines(
            "pkg/Outer.java",
            """
            package pkg;

            public class Outer {
              public @interface Meta {}

              @Meta
              public @interface Composed {}

              public @interface Plain {}
            }
            """)
        .addSourceLines(
            "pkg/Test.java",
            """
            package pkg;

            class Test {
              // BUG: Diagnostic contains:
              @Outer.Composed
              void composed() {}

              @Outer.Plain
              void plain() {}
            }
            """)
        .doTest();
  }

  @Test
  public void classHierarchyMatchers_className() {
    ClassName nestedIface = ClassName.of("pkg.Outer.NestedIface");
    ClassName nestedBase = ClassName.of("pkg.Outer$NestedBase");
    ClassName absent = ClassName.of("com.example.Absent");
    assertMatcher(
            ClassTree.class,
            Matchers.allOf(
                Matchers.isDirectImplementationOf(nestedIface),
                Matchers.isExtensionOf(nestedBase),
                Matchers.isSubtypeOf(nestedIface),
                Matchers.not(Matchers.isDirectImplementationOf(absent)),
                Matchers.not(Matchers.isExtensionOf(absent)),
                Matchers.not(Matchers.isSubtypeOf(absent))))
        .addSourceLines(
            "pkg/Outer.java",
            """
            package pkg;

            public class Outer {
              public interface NestedIface {}

              public static class NestedBase {}

              // BUG: Diagnostic contains:
              public static class NestedImpl extends NestedBase implements NestedIface {}
            }
            """)
        .doTest();
  }

  @Test
  public void methodDeclarationMatchers_className() {
    ClassName nestedImpl = ClassName.of("pkg.Outer.NestedImpl");
    ClassName nestedIface = ClassName.of("pkg.Outer.NestedIface");
    ClassName absent = ClassName.of("com.example.Absent");
    assertMatcher(
            MethodTree.class,
            Matchers.anyOf(
                Matchers.constructorOfClass(nestedImpl),
                Matchers.constructorOfClass(absent),
                Matchers.allOf(
                    Matchers.methodWithClassAndName(nestedImpl, "targetMethod"),
                    Matchers.methodReturns(nestedIface),
                    Matchers.not(Matchers.methodWithClassAndName(absent, "targetMethod")),
                    Matchers.not(Matchers.methodReturns(absent)))))
        .addSourceLines(
            "pkg/Outer.java",
            """
            package pkg;

            public class Outer {
              public interface NestedIface {}

              public static class NestedImpl implements NestedIface {
                // BUG: Diagnostic contains:
                public NestedImpl() {}

                // BUG: Diagnostic contains:
                public NestedIface targetMethod() {
                  return this;
                }

                public String otherMethod() {
                  return "";
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void fieldMatchers_className() {
    ClassName innerCanonical = ClassName.of("pkg.Outer.Inner");
    ClassName innerBinary = ClassName.of("pkg.Outer$Inner");
    ClassName absent = ClassName.of("com.example.Absent");
    assertMatcher(
            MemberSelectTree.class,
            Matchers.anyOf(
                FieldMatchers.instanceField().onClass(innerCanonical).named("instField"),
                FieldMatchers.staticField().onClass(innerBinary).named("statField"),
                FieldMatchers.instanceField().onClass(absent).named("instField"),
                FieldMatchers.staticField().onClass(absent).named("statField")))
        .addSourceLines(
            "pkg/Outer.java",
            """
            package pkg;

            public class Outer {
              public static class Inner {
                public int instField;
                public static int statField;
              }

              void test(Inner inner) {
                // BUG: Diagnostic contains:
                int a = inner.instField;
                // BUG: Diagnostic contains:
                int b = Inner.statField;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void constructorForClass_className() {
    assertMatcher(
            NewClassTree.class,
            Matchers.anyOf(
                MethodMatchers.constructor().forClass(ClassName.of("pkg.Outer$Sub")),
                MethodMatchers.constructor().forClass(ClassName.of("com.example.Absent"))))
        .addSourceLines(
            "pkg/Outer.java",
            """
            package pkg;

            public class Outer {
              public static class Sub {}

              public static class Other {}

              void test() {
                // BUG: Diagnostic contains:
                Sub sub = new Sub();
                Other other = new Other();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void instanceMethodMatchers_className() {
    ClassName base = ClassName.of("pkg.Outer.Base");
    ClassName sub = ClassName.of("pkg.Outer$Sub");
    ClassName inner = ClassName.of("pkg.Outer.Inner");
    ClassName absent = ClassName.of("com.example.Absent");
    assertMatcher(
            MethodInvocationTree.class,
            Matchers.anyOf(
                Matchers.allOf(
                    MethodMatchers.instanceMethod().onExactClass(sub).withAnyName(),
                    MethodMatchers.instanceMethod().onExactClassAny(absent, sub).withAnyName(),
                    MethodMatchers.instanceMethod().onDescendantOf(base).withAnyName(),
                    MethodMatchers.instanceMethod().onDescendantOfAny(absent, base).withAnyName()),
                Matchers.allOf(
                    MethodMatchers.instanceMethod()
                        .onDescendantOf(base)
                        .named("baseMethod")
                        .withParametersOfType(inner),
                    Matchers.not(
                        MethodMatchers.instanceMethod()
                            .onDescendantOf(base)
                            .named("baseMethod")
                            .withParametersOfType(absent)))))
        .addSourceLines(
            "pkg/Outer.java",
            """
            package pkg;

            public class Outer {
              public static class Base {
                public void baseMethod(Inner arg) {}

                public void otherMethod() {}
              }

              public static class Sub extends Base {
                public void exactMethod() {}
              }

              public static class Inner {}

              void test(Base base, Sub sub, Inner inner) {
                // BUG: Diagnostic contains:
                sub.exactMethod();
                // BUG: Diagnostic contains:
                base.baseMethod(inner);
                base.otherMethod();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void typePredicatesAndIsSameType_className() {
    ClassName base = ClassName.of("pkg.Outer.Base");
    ClassName sub = ClassName.of("pkg.Outer$Sub");
    ClassName absent = ClassName.of("com.example.Absent");
    assertMatcher(
            VariableTree.class,
            Matchers.allOf(
                Matchers.isSameType(sub),
                Matchers.typePredicateMatcher(TypePredicates.isExactType(sub)),
                Matchers.typePredicateMatcher(TypePredicates.isExactTypeAny(absent, sub)),
                Matchers.typePredicateMatcher(TypePredicates.isDescendantOf(base)),
                Matchers.typePredicateMatcher(TypePredicates.isDescendantOfAny(absent, base)),
                Matchers.typePredicateMatcher(
                    TypePredicates.hasAnnotation(ClassName.of("pkg.Outer.InheritedAnno"))),
                Matchers.not(Matchers.typePredicateMatcher(TypePredicates.hasAnnotation(absent)))))
        .addSourceLines(
            "pkg/Outer.java",
            """
            package pkg;

            import java.lang.annotation.Inherited;

            public class Outer {
              @Inherited
              public @interface InheritedAnno {}

              @InheritedAnno
              public static class Base {}

              public static class Sub extends Base {}

              // BUG: Diagnostic contains:
              Sub matching;
              Base nonMatching;
            }
            """)
        .doTest();
  }

  @Test
  public void suppliersArrayOfClassName() {
    CompilationTestHelper.newInstance(ArrayOfClassNameChecker.class, getClass())
        .addSourceLines(
            "pkg/Test.java",
            """
            package pkg;

            class Test {
              // BUG: Diagnostic contains: [stringArray]
              String[] strings;
              int[] ints;
              String notArray;
            }
            """)
        .doTest();
  }

  private static <T extends Tree> CompilationTestHelper assertMatcher(
      Class<T> treeType, Matcher<? super T> matcher) {
    return CompilationTestHelper.newInstance(
        ScannerSupplier.fromScanner(new ErrorProneScanner(new TreeChecker<>(treeType, matcher))),
        MatchersTest.class);
  }

  @BugPattern(summary = "Flags trees matching the given matcher", severity = ERROR)
  private static class TreeChecker<T extends Tree> extends BugChecker
      implements AnnotationTreeMatcher,
          ClassTreeMatcher,
          MemberSelectTreeMatcher,
          MethodInvocationTreeMatcher,
          MethodTreeMatcher,
          NewClassTreeMatcher,
          VariableTreeMatcher {
    private final Class<T> treeType;
    private final Matcher<? super T> matcher;

    TreeChecker(Class<T> treeType, Matcher<? super T> matcher) {
      this.treeType = treeType;
      this.matcher = matcher;
    }

    private Description matchTree(Tree tree, VisitorState state) {
      if (treeType.isInstance(tree) && matcher.matches(treeType.cast(tree), state)) {
        return describeMatch(tree);
      }
      return Description.NO_MATCH;
    }

    @Override
    public Description matchAnnotation(AnnotationTree tree, VisitorState state) {
      return matchTree(tree, state);
    }

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      return matchTree(tree, state);
    }

    @Override
    public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
      return matchTree(tree, state);
    }

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      return matchTree(tree, state);
    }

    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
      return matchTree(tree, state);
    }

    @Override
    public Description matchNewClass(NewClassTree tree, VisitorState state) {
      return matchTree(tree, state);
    }

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      return matchTree(tree, state);
    }
  }

  /**
   * Checker exercising {@link Suppliers#arrayOf(ClassName)} for present and absent element types.
   */
  @BugPattern(summary = "ArrayOfClassNameChecker", severity = ERROR)
  public static class ArrayOfClassNameChecker extends BugChecker implements VariableTreeMatcher {
    private static final ClassName STRING = ClassName.of("java.lang.String");
    private static final ClassName ABSENT = ClassName.of("com.example.Absent");
    private static final Supplier<Type> STRING_ARRAY = Suppliers.arrayOf(STRING);
    private static final Supplier<Type> ABSENT_ARRAY = Suppliers.arrayOf(ABSENT);

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      assertThat(ABSENT_ARRAY.get(state)).isNull();
      assertThat(STRING_ARRAY.get(state)).isSameInstanceAs(STRING_ARRAY.get(state));
      Type varType = ASTHelpers.getType(tree);
      assertThat(ASTHelpers.isSameType(varType, ABSENT_ARRAY.get(state), state)).isFalse();
      assertThat(ASTHelpers.isSubtype(varType, ABSENT_ARRAY.get(state), state)).isFalse();
      if (ASTHelpers.isSameType(varType, STRING_ARRAY.get(state), state)) {
        return buildDescription(tree).setMessage("[stringArray]").build();
      }
      return Description.NO_MATCH;
    }
  }
}
