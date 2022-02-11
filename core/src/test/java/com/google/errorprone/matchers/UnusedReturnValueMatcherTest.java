/*
 * Copyright 2022 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link UnusedReturnValueMatcher}. */
@RunWith(JUnit4.class)
public final class UnusedReturnValueMatcherTest {

  /** Checker using {@link UnusedReturnValueMatcher}. */
  @BugPattern(severity = WARNING, summary = "bad")
  public static class Checker extends BugChecker
      implements MethodInvocationTreeMatcher, NewClassTreeMatcher, MemberReferenceTreeMatcher {

    protected boolean allowInExceptionTesting() {
      return true;
    }

    private Description match(ExpressionTree tree, VisitorState state) {
      if (!UnusedReturnValueMatcher.isReturnValueUnused(tree, state)) {
        return Description.NO_MATCH;
      }
      UnusedReturnValueMatcher matcher = UnusedReturnValueMatcher.get(allowInExceptionTesting());
      return matcher.isAllowed(tree, state)
          ? buildDescription(tree).setMessage("allowed").build()
          : describeMatch(tree);
    }

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      return match(tree, state);
    }

    @Override
    public Description matchNewClass(NewClassTree tree, VisitorState state) {
      return match(tree, state);
    }

    @Override
    public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
      return match(tree, state);
    }
  }

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(Checker.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "public class Test {",
            "  static interface Foo {",
            "    String bar();",
            "  }",
            "",
            "  static String staticMethod() {",
            "    return \"static\";",
            "  }",
            "  String instanceMethod() {",
            "    return \"instance\";",
            "  }",
            "",
            "  static void run(Runnable r) {}",
            "",
            "  void stuff(Foo foo) {",
            "    // BUG: Diagnostic contains: bad",
            "    foo.bar();",
            "    // BUG: Diagnostic contains: bad",
            "    run(foo::bar);",
            "    // BUG: Diagnostic contains: bad",
            "    run(() -> foo.bar());",
            "    // BUG: Diagnostic contains: bad",
            "    staticMethod();",
            "    // BUG: Diagnostic contains: bad",
            "    Test.staticMethod();",
            "    // BUG: Diagnostic contains: bad",
            "    instanceMethod();",
            "    // BUG: Diagnostic contains: bad",
            "    this.instanceMethod();",
            "    // BUG: Diagnostic contains: bad",
            "    run(Test::staticMethod);",
            "    // BUG: Diagnostic contains: bad",
            "    run(() -> Test.staticMethod());",
            "    // BUG: Diagnostic contains: bad",
            "    run(this::instanceMethod);",
            "    // BUG: Diagnostic contains: bad",
            "    run(() -> instanceMethod());",
            "    // BUG: Diagnostic contains: bad",
            "    run(() -> this.instanceMethod());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import java.util.function.Supplier;",
            "public class Test {",
            "  interface Foo {",
            "    String bar();",
            "    void voidMethod();",
            "  }",
            "",
            "  static String staticMethod() {",
            "    return \"static\";",
            "  }",
            "  String instanceMethod() {",
            "    return \"instance\";",
            "  }",
            "",
            "  static void accept(String s) {}",
            "  static void run(Supplier<String> s) {}",
            "",
            "  String stuff(Foo foo) {",
            "    String s = foo.bar();",
            "    s = staticMethod();",
            "    s = instanceMethod();",
            "    s = Test.staticMethod();",
            "    s = this.instanceMethod();",
            "    accept(foo.bar());",
            "    accept(staticMethod());",
            "    accept(instanceMethod());",
            "    accept(Test.staticMethod());",
            "    accept(this.instanceMethod());",
            "    run(foo::bar);",
            "    run(Test::staticMethod);",
            "    run(this::instanceMethod);",
            "    run(() -> foo.bar());",
            "    run(() -> staticMethod());",
            "    run(() -> instanceMethod());",
            "    run(() -> Test.staticMethod());",
            "    run(() -> this.instanceMethod());",
            "    Supplier<String> supplier = foo::bar;",
            "    supplier = Test::staticMethod;",
            "    supplier = this::instanceMethod;",
            "    supplier = () -> foo.bar();",
            "    supplier = () -> staticMethod();",
            "    supplier = () -> instanceMethod();",
            "    supplier = () -> Test.staticMethod();",
            "    supplier = () -> this.instanceMethod();",
            "    foo.voidMethod();",
            "    return foo.bar();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void allowed() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import static org.junit.Assert.fail;",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.verify;",
            "import java.util.function.Consumer;",
            "import org.junit.rules.ExpectedException;",
            "public class Test {",
            "  interface Foo<T> {",
            "    T bar();",
            "  }",
            "",
            "  private final ExpectedException expected = ExpectedException.none();",
            "",
            "  static void run(Runnable r) {}",
            "  static <T> void doSomething(T foo, Consumer<? super T> c) {}",
            "",
            "  interface SubFoo<T> extends Foo<T> {}",
            "  interface VoidFoo extends Foo<Void> {}",
            "  interface VoidSubFoo extends SubFoo<Void> {}",
            "",
            "  void javaLangVoid(",
            "      Foo<Void> foo, SubFoo<Void> subFoo, VoidFoo voidFoo, VoidSubFoo voidSubFoo) {",
            "    // BUG: Diagnostic contains: allowed",
            "    foo.bar();",
            "    // BUG: Diagnostic contains: allowed",
            "    run(foo::bar);",
            "    // BUG: Diagnostic contains: allowed",
            "    run(() -> foo.bar());",
            "",
            "    // BUG: Diagnostic contains: allowed",
            "    subFoo.bar();",
            "    // BUG: Diagnostic contains: allowed",
            "    run(subFoo::bar);",
            "    // BUG: Diagnostic contains: allowed",
            "    run(() -> subFoo.bar());",
            "",
            "    // BUG: Diagnostic contains: allowed",
            "    voidFoo.bar();",
            "    // BUG: Diagnostic contains: allowed",
            "    run(voidFoo::bar);",
            "    // BUG: Diagnostic contains: allowed",
            "    run(() -> voidFoo.bar());",
            "",
            "    // BUG: Diagnostic contains: allowed",
            "    voidSubFoo.bar();",
            "    // BUG: Diagnostic contains: allowed",
            "    run(voidSubFoo::bar);",
            "    // BUG: Diagnostic contains: allowed",
            "    run(() -> voidSubFoo.bar());",
            "",
            "    // BUG: Diagnostic contains: allowed",
            "    doSomething(voidFoo, VoidFoo::bar);",
            "    // BUG: Diagnostic contains: allowed",
            "    doSomething(voidSubFoo, VoidSubFoo::bar);",
            "  }",
            "",
            "  void exceptionTestingFail(Foo<String> foo) {",
            "    try {",
            "      // BUG: Diagnostic contains: allowed",
            "      foo.bar();",
            "      fail();",
            "    } catch (RuntimeException expected) {",
            "    }",
            "",
            "    expected.expect(RuntimeException.class);",
            "    // BUG: Diagnostic contains: allowed",
            "    foo.bar();",
            "  }",
            "",
            "  void mockito() {",
            "    Foo<?> foo = mock(Foo.class);",
            "    // BUG: Diagnostic contains: allowed",
            "    verify(foo).bar();",
            "  }",
            "}")
        .doTest();
  }

  /** {@link Checker} with {@code allowInExceptionTesting = false}. */
  @BugPattern(severity = WARNING, summary = "bad")
  public static class NotAllowedInExceptionTesting extends Checker {
    @Override
    protected boolean allowInExceptionTesting() {
      return false;
    }
  }

  @Test
  public void allowedInExceptionTestingFalse() {
    CompilationTestHelper notAllowedInExceptionTesting =
        CompilationTestHelper.newInstance(NotAllowedInExceptionTesting.class, getClass());
    notAllowedInExceptionTesting
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import static org.junit.Assert.fail;",
            "import org.junit.rules.ExpectedException;",
            "public class Test {",
            "  interface Foo<T> {",
            "    T bar();",
            "  }",
            "",
            "  private final ExpectedException expected = ExpectedException.none();",
            "",
            "  void exceptionTesting(Foo<String> foo) {",
            "    try {",
            "      // BUG: Diagnostic contains: bad",
            "      foo.bar();",
            "      fail();",
            "    } catch (RuntimeException expected) {",
            "    }",
            "",
            "    expected.expect(RuntimeException.class);",
            "    // BUG: Diagnostic contains: bad",
            "    foo.bar();",
            "  }",
            "}")
        .doTest();
  }
}
