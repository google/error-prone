/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.ONE_OFF;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.inLoop;
import static com.google.errorprone.matchers.Matchers.isPrimitiveOrVoidType;
import static com.google.errorprone.matchers.Matchers.isPrimitiveType;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.isVoidType;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;

import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.MatcherChecker;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.scanner.ErrorProneScanner;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link Matchers}. */
@RunWith(JUnit4.class)
public class MatchersTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(InLoopChecker.class, getClass());
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
            "    return new Integer(42);",
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

  @BugPattern(
    name = "InLoopChecker",
    summary = "Checker that flags the given expression statement if the given matcher matches",
    category = ONE_OFF,
    severity = ERROR
  )
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
    category = ONE_OFF,
    severity = ERROR
  )
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
    category = ONE_OFF,
    severity = ERROR
  )
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

  /** Checker that makes sure symbolHasAnnotation matches on MethodInvocationTree. */
  @BugPattern(
    name = "NoAnnotatedDeclarationCallsChecker",
    summary = "Checker that flags the given method invocation if the given matcher matches",
    category = ONE_OFF,
    severity = ERROR
  )
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
}
