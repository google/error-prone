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
import static org.junit.Assert.fail;

import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.MatcherChecker;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.scanner.ErrorProneScanner;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
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
    try {
      Matchers.instanceMethod().onExactClass("java.lang.String").named("getBytes()");
      fail("Expected an IAE to be throw but wasn't");
    } catch (IllegalArgumentException expected) {
    }
    try {
      Matchers.instanceMethod().onExactClass("java.lang.String").named("getBytes)");
      fail("Expected an IAE to be throw but wasn't");
    } catch (IllegalArgumentException expected) {
    }
    try {
      Matchers.instanceMethod().onExactClass("java.lang.String").named("getBytes(");
      fail("Expected an IAE to be throw but wasn't");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void inLoopShouldMatchInWhileLoop() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  public void doIt() {",
            "    while (true) {",
            "      // BUG: Diagnostic contains:",
            "      System.out.println();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inLoopShouldMatchInDoLoop() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  public void doIt() {",
            "    do {",
            "      // BUG: Diagnostic contains:",
            "      System.out.println();",
            "    } while (true);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inLoopShouldMatchInForLoop() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  public void doIt() {",
            "    for (; true; ) {",
            "      // BUG: Diagnostic contains:",
            "      System.out.println();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inLoopShouldMatchInEnhancedForLoop() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "public class Test {",
            "  public void doIt(List<String> strings) {",
            "    for (String s : strings) {",
            "      // BUG: Diagnostic contains:",
            "      System.out.println();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inLoopShouldNotMatchInInitializerWithoutLoop() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "public class Test {",
            "  {",
            "    System.out.println();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inLoopShouldMatchInInitializerInLoop() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "public class Test {",
            "  {",
            "    int count = 0;",
            "    while (count < 10) {",
            "      // BUG: Diagnostic contains:",
            "      System.out.println();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inLoopShouldNotMatchInAnonymousInnerClassDefinedInLoop() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.*;",
            "public class Test {",
            "  public void sort(List<List<String>> stringLists) {",
            "    for (List<String> stringList : stringLists) {",
            "      Collections.sort(stringList, new Comparator<String>() {",
            "          {",
            "            System.out.println();",
            "          }",
            "          public int compare(String s1, String s2) {",
            "            return 0;",
            "          }",
            "          public boolean equals(Object obj) {",
            "            return false;",
            "          }",
            "      });",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodWithClassAndName() {
    Matcher<MethodTree> matcher =
        Matchers.methodWithClassAndName("com.google.errorprone.foo.bar.Test", "myMethod");
    CompilationTestHelper.newInstance(methodTreeCheckerSupplier(matcher), getClass())
        .addSourceLines(
            "com/google/errorprone/foo/bar/Test.java",
            "package com.google.errorprone.foo.bar;",
            "public class Test {",
            "  // BUG: Diagnostic contains:",
            "  public void myMethod() {}",
            "}")
        .doTest();
  }

  @Test
  public void methodReturnsSubtype() {
    Matcher<MethodTree> matcher = methodReturns(isSubtypeOf("java.util.List"));
    CompilationTestHelper.newInstance(methodTreeCheckerSupplier(matcher), getClass())
        .addSourceLines(
            "test/MethodReturnsSubtypeTest.java",
            "package test;",
            "public class MethodReturnsSubtypeTest {",
            "  // BUG: Diagnostic contains:",
            "  public java.util.ArrayList<String> matches() {",
            "    return null;",
            "  }",
            "  public java.util.HashSet<String> doesntMatch() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodReturnsType() {
    Matcher<MethodTree> matcher = methodReturns(typeFromString("java.lang.Number"));
    CompilationTestHelper.newInstance(methodTreeCheckerSupplier(matcher), getClass())
        .addSourceLines(
            "test/MethodReturnsSubtypeTest.java",
            "package test;",
            "public class MethodReturnsSubtypeTest {",
            "  public java.lang.Integer doesntMatch() {",
            "    return 0;",
            "  }",
            "  // BUG: Diagnostic contains:",
            "  public java.lang.Number matches() {",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodReturnsPrimitiveType() {
    Matcher<MethodTree> matcher = methodReturns(isPrimitiveType());
    CompilationTestHelper.newInstance(methodTreeCheckerSupplier(matcher), getClass())
        .addSourceLines(
            "test/MethodReturnsPrimitiveTypeTest.java",
            "package test;",
            "public class MethodReturnsPrimitiveTypeTest {",
            "  // BUG: Diagnostic contains:",
            "  public boolean matches1() {",
            "    return false;",
            "  }",
            "  public void doesntMatch() {",
            "  }",
            "  // BUG: Diagnostic contains:",
            "  public int matches2() {",
            "    return 42;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodReturnsVoidType() {
    Matcher<MethodTree> matcher =
        Matchers.allOf(methodReturns(typeFromString("void")), methodReturns(isVoidType()));
    CompilationTestHelper.newInstance(methodTreeCheckerSupplier(matcher), getClass())
        .addSourceLines(
            "test/MethodReturnsVoidTypeTest.java",
            "package test;",
            "public class MethodReturnsVoidTypeTest {",
            "  public boolean doesntMatch1() {",
            "    return true;",
            "  }",
            "  public Object doesntMatch2() {",
            "    return Integer.valueOf(42);",
            "  }",
            "  // BUG: Diagnostic contains:",
            "  public void matches() {",
            "    System.out.println(42);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodReturnsPrimitiveOrVoidType() {
    Matcher<MethodTree> matcher = methodReturns(isPrimitiveOrVoidType());
    CompilationTestHelper.newInstance(methodTreeCheckerSupplier(matcher), getClass())
        .addSourceLines(
            "test/MethodReturnsPrimitiveOrVoidTypeTest.java",
            "package test;",
            "public class MethodReturnsPrimitiveOrVoidTypeTest {",
            "  public Object doesntMatch() {",
            "    return null;",
            "  }",
            "  // BUG: Diagnostic contains:",
            "  public boolean doesMatch1() {",
            "    return true;",
            "  }",
            "  // BUG: Diagnostic contains:",
            "  public void doesMatch2() {",
            "  }",
            "  // BUG: Diagnostic contains:",
            "  public double doesMatch3() {",
            "    return 42.0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodReturnsNonPrimitiveType() {
    Matcher<MethodTree> matcher = Matchers.methodReturnsNonPrimitiveType();
    CompilationTestHelper.newInstance(methodTreeCheckerSupplier(matcher), getClass())
        .addSourceLines(
            "test/MethodReturnsSubtypeTest.java",
            "package test;",
            "public class MethodReturnsSubtypeTest {",
            "  public int doesntMatch() {",
            "    return 0;",
            "  }",
            "  public void doesntMatch2() {",
            "  }",
            "  // BUG: Diagnostic contains:",
            "  public String matches() {",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodInvocationDoesntMatchAnnotation() {
    CompilationTestHelper.newInstance(NoAnnotatedCallsChecker.class, getClass())
        .addSourceLines(
            "test/MethodInvocationDoesntMatchAnnotation.java",
            "package test;",
            "public class MethodInvocationDoesntMatchAnnotation {",
            "  @Deprecated",
            "  public void matches() {",
            "  }",
            "  public void doesntMatch() {",
            "    matches();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodInvocationHasDeclarationAnnotation() {
    CompilationTestHelper.newInstance(NoAnnotatedDeclarationCallsChecker.class, getClass())
        .addSourceLines(
            "test/MethodInvocationMatchesDeclAnnotation.java",
            "package test;",
            "public class MethodInvocationMatchesDeclAnnotation {",
            "  @Deprecated",
            "  public void matches() {",
            "  }",
            "  public void callsMatch() {",
            "    // BUG: Diagnostic contains:",
            "    matches();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void sameArgumentGoesOutOfBounds() {
    AssertionError thrown =
        assertThrows(
            AssertionError.class,
            () ->
                CompilationTestHelper.newInstance(SameArgumentChecker.class, getClass())
                    .addSourceLines(
                        "test/SameArgumentCheckerTest.java",
                        "package test;",
                        "public class SameArgumentCheckerTest {",
                        "  public void matches(Object... args) {",
                        "  }",
                        "  public void callsMatch() {",
                        "    Object obj = new Object();",
                        "    matches(obj, \"some arg\");",
                        "  }",
                        "}")
                    .doTest());
    assertThat(thrown).hasMessageThat().contains("IndexOutOfBoundsException");
  }

  @Test
  public void booleanConstantMatchesTrue() {
    CompilationTestHelper.newInstance(BooleanConstantTrueChecker.class, getClass())
        .addSourceLines(
            "test/BooleanConstantTrueCheckerTest.java",
            "package test;",
            "public class BooleanConstantTrueCheckerTest {",
            "  public void function() {",
            "    // BUG: Diagnostic contains:",
            "    method(Boolean.TRUE);",
            "    method(Boolean.FALSE);",
            "  }",
            "  void method(Object value) {}",
            "}")
        .doTest();
  }

  @Test
  public void packageNameChecker() {
    CompilationTestHelper.newInstance(PackageNameChecker.class, getClass())
        .addSourceLines(
            "test/foo/ClassName.java",
            "package test.foo;",
            "// BUG: Diagnostic contains:",
            "public class ClassName {}")
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
            "package test.foo.bar;",
            "// BUG: Diagnostic contains:",
            "public class ClassName {}")
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
            "package test.foobar;",
            "// BUG: Diagnostic contains:",
            "public class ClassName {}")
        .doTest();
  }

  @BugPattern(
      name = "InLoopChecker",
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
      name = "MethodTreeChecker",
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
      name = "MethodInvocationTreeChecker",
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
      name = "SameArgumentChecker",
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
      name = "NoAnnotatedDeclarationCallsChecker",
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
  @BugPattern(
      name = "PackageNameChecker",
      summary = "Checks the name of the package",
      severity = ERROR)
  public static class PackageNameChecker extends BugChecker implements ClassTreeMatcher {
    private static final Matcher<Tree> MATCHER = Matchers.packageStartsWith("test.foo");

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      return MATCHER.matches(tree, state) ? describeMatch(tree) : Description.NO_MATCH;
    }
  }

  /** Checker that checks if an argument is 'Boolean.TRUE'. */
  @BugPattern(
      name = "BooleanConstantTrueChecker",
      summary = "BooleanConstantTrueChecker",
      severity = ERROR)
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
}
