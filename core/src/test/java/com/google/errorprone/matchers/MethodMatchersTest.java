/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.matchers.method.MethodMatchers.MethodNameMatcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link MethodMatchers}Test */
@RunWith(JUnit4.class)
public class MethodMatchersTest {

  /** A bugchecker to test constructor matching. */
  @BugPattern(
      name = "ConstructorDeleter",
      category = JDK,
      summary = "Deletes constructors",
      severity = ERROR,
      providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
  public static class ConstructorDeleter extends BugChecker
      implements BugChecker.MethodInvocationTreeMatcher, BugChecker.NewClassTreeMatcher {

    static final Matcher<ExpressionTree> CONSTRUCTOR =
        constructor().forClass("test.Foo").withParameters("java.lang.String");

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      if (CONSTRUCTOR.matches(tree, state)) {
        return describeMatch(tree, SuggestedFix.delete(tree));
      }
      return NO_MATCH;
    }

    @Override
    public Description matchNewClass(NewClassTree tree, VisitorState state) {
      if (CONSTRUCTOR.matches(tree, state)) {
        return describeMatch(tree, SuggestedFix.delete(tree));
      }
      return NO_MATCH;
    }
  }

  @Test
  public void constructorMatcherTest_this() {
    CompilationTestHelper.newInstance(ConstructorDeleter.class, getClass())
        .addSourceLines(
            "test/Foo.java",
            "package test;",
            "public class Foo { ",
            "  public Foo(String s) {}",
            "  public Foo() {",
            "    // BUG: Diagnostic contains:",
            "    this(\"\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructorMatcherTest_super() {
    CompilationTestHelper.newInstance(ConstructorDeleter.class, getClass())
        .addSourceLines(
            "test/Foo.java",
            "package test;",
            "public class Foo { ",
            "  public Foo(String s) {}",
            "}")
        .addSourceLines(
            "test/Bar.java", //
            "package test;",
            "public class Bar extends Foo {",
            "  public Bar() {",
            "    // BUG: Diagnostic contains:",
            "    super(\"\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructorMatcherTest_regular() {
    CompilationTestHelper.newInstance(ConstructorDeleter.class, getClass())
        .addSourceLines(
            "test/Foo.java",
            "package test;",
            "public class Foo { ",
            "  public Foo(String s) {}",
            "}")
        .addSourceLines(
            "test/Test.java", //
            "package test;",
            "public class Test {",
            "  public void f() {",
            "    // BUG: Diagnostic contains:",
            "    new Foo(\"\");",
            "  }",
            "}")
        .doTest();
  }

  /** This is javadoc. */
  @BugPattern(
      name = "CrashyParameterMatcherTestChecker",
      category = JDK,
      summary = "",
      severity = ERROR)
  public static class CrashyerMatcherTestChecker extends BugChecker
      implements MethodInvocationTreeMatcher {

    public static final Matcher<ExpressionTree> MATCHER =
        instanceMethod().anyClass().withAnyName().withParameters("NOSUCH");

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      return MATCHER.matches(tree, state) ? describeMatch(tree) : NO_MATCH;
    }
  }

  @Test
  public void varargsParameterMatcher() {
    CompilationTestHelper.newInstance(CrashyerMatcherTestChecker.class, getClass())
        .addSourceLines("Lib.java", "abstract class Lib { ", "  void f(int x) {}", "}")
        .addSourceLines(
            "test/Test.java", //
            "class Test {",
            "  void f(Lib l) {",
            "    l.f(42);",
            "  }",
            "}")
        .doTest();
  }

  /** Test BugChecker for namedAnyOf(...) */
  @BugPattern(name = "FlagMethodNames", summary = "", severity = ERROR)
  public static class FlagMethodNamesChecker extends BugChecker
      implements MethodInvocationTreeMatcher {
    private static final MethodNameMatcher INSTANCE_MATCHER =
        instanceMethod().anyClass().namedAnyOf("foo", "bar");
    private static final MethodNameMatcher STATIC_MATCHER =
        staticMethod().anyClass().namedAnyOf("fizz", "buzz");

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      if (INSTANCE_MATCHER.matches(tree, state)) {
        return buildDescription(tree).setMessage("instance varargs").build();
      }
      if (STATIC_MATCHER.matches(tree, state)) {
        return buildDescription(tree).setMessage("static varargs").build();
      }
      return NO_MATCH;
    }
  }

  @Test
  public void namedAnyOfTest() {
    CompilationTestHelper.newInstance(FlagMethodNamesChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void foo() {}",
            "  void bar() {}",
            "  static void fizz() {}",
            "  static void buzz() {}",
            "  void anotherMethod() {}",
            "  static void yetAnother() {}",
            "  void f() {",
            "    // BUG: Diagnostic contains: instance varargs",
            "    this.foo();",
            "    // BUG: Diagnostic contains: instance varargs",
            "    this.bar();",
            "    // BUG: Diagnostic contains: static varargs",
            "    Test.fizz();",
            "    // BUG: Diagnostic contains: static varargs",
            "    Test.buzz();",
            "    // These ones are ok",
            "    this.anotherMethod();",
            "    Test.yetAnother();",
            "  }",
            "}")
        .doTest();
  }
}
