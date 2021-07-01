/*
 * Copyright 2021 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.inlineme;

import static com.google.errorprone.bugpatterns.inlineme.Inliner.PREFIX_FLAG;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.scanner.ScannerSupplier;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for the {@link Inliner}. */
@RunWith(JUnit4.class)
public class InlinerTest {
  /* We expect that all @InlineMe annotations we try to use as inlineable targets are valid,
   so we run both checkers here. If the Validator trips on a method, we'll suggest some
   replacement which should trip up the checker.
  */
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(
          ScannerSupplier.fromBugCheckerClasses(Inliner.class, Validator.class), getClass());

  @Test
  public void testInstanceMethod_withThisLiteral() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this.foo2(value)\")",
            "  public void foo1(String value) {",
            "    foo2(value);",
            "  }",
            "  public void foo2(String value) {",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    client.foo1(\"frobber!\");",
            "    client.foo1(\"don't change this!\");",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    client.foo2(\"frobber!\");",
            "    client.foo2(\"don't change this!\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nestedQuotes() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this.after(foo)\")",
            "  public String before(String foo) {",
            "    return after(foo);",
            "  }",
            "  public String after(String foo) {",
            "    return \"frobber\";",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    String result = client.before(\"\\\"\");", // "\"" - a single quote character
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    String result = client.after(\"\\\"\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMethod_withParamSwap() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this.after(paramB, paramA)\")",
            "  public void before(String paramA, String paramB) {",
            "    after(paramB, paramA);",
            "  }",
            "  public void after(String paramB, String paramA) {",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "import java.time.Duration;",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    String a = \"a\";",
            "    String b = \"b\";",
            "    client.before(a, b);",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "import java.time.Duration;",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    String a = \"a\";",
            "    String b = \"b\";",
            "    client.after(b, a);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMethod_withReturnStatement() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this.after()\")",
            "  public String before() {",
            "    return after();",
            "  }",
            "  public String after() {",
            "    return \"frobber\";",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    String result = client.before();",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    String result = client.after();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testStaticMethod_explicitTypeParam() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.foo;",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(",
            "      replacement = \"Client.after()\",",
            "      imports = {\"com.google.foo.Client\"})",
            "  public static <T> T before() {",
            "    return after();",
            "  }",
            "  public static <T> T after() {",
            "    return (T) null;",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "package com.google.foo;",
            "public final class Caller {",
            "  public void doTest() {",
            "    String str = Client.<String>before();",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "package com.google.foo;",
            "public final class Caller {",
            "  public void doTest() {",
            // TODO(b/166285406): Client.<String>after();
            "    String str = Client.after();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInstanceMethod_withConflictingImport() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public final class Client {",
            "  private Duration deadline = Duration.ofSeconds(5);",
            "  @Deprecated",
            "  @InlineMe(",
            "      replacement = \"this.setDeadline(Duration.ofMillis(millis))\",",
            "      imports = {\"java.time.Duration\"})",
            "  public void setDeadline(long millis) {",
            "    setDeadline(Duration.ofMillis(millis));",
            "  }",
            "  public void setDeadline(Duration deadline) {",
            "    this.deadline = deadline;",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "import org.joda.time.Duration;",
            "public final class Caller {",
            "  public void doTest() {",
            "    Duration jodaDuration = Duration.millis(42);",
            "    Client client = new Client();",
            "    client.setDeadline(42);",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "import org.joda.time.Duration;",
            "public final class Caller {",
            "  public void doTest() {",
            "    Duration jodaDuration = Duration.millis(42);",
            "    Client client = new Client();",
            "    client.setDeadline(java.time.Duration.ofMillis(42));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInstanceMethod_withPartiallyQualifiedInnerType() {
    refactoringTestHelper
        .addInputLines(
            "A.java",
            "package com.google;",
            "public class A {",
            "  public static class Inner {",
            "    public static void foo() {",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Client.java",
            "import com.google.A;",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"A.Inner.foo()\", imports = \"com.google.A\")",
            "  public void something() {",
            "    A.Inner.foo();",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    client.something();",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "import com.google.A;",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    A.Inner.foo();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInstanceMethod_withConflictingMethodNameAndParameterName() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  private long deadline = 5000;",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this.millis(millis)\")",
            "  public void setDeadline(long millis) {",
            "    millis(millis);",
            "  }",
            "  public void millis(long millis) {",
            "    this.deadline = millis;",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    client.setDeadline(42);",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    client.millis(42);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testStaticMethod_withStaticImport_withImport() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.test;",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(",
            "      replacement = \"Client.after(value)\", ",
            "      imports = {\"com.google.test.Client\"})",
            "  public static void before(int value) {",
            "    after(value);",
            "  }",
            "  public static void after(int value) {",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "import static com.google.test.Client.before;",
            "public final class Caller {",
            "  public void doTest() {",
            "    before(42);",
            "  }",
            "}")
        .addOutputLines(
            "Caller.java",
            "import static com.google.test.Client.before;",
            "import com.google.test.Client;",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client.after(42);",
            "  }",
            "}")
        .doTest();
  }

  // With the new suggester implementation, we always import the surrounding class, so the suggested
  // replacement here isn't considered valid.
  @Ignore("b/176439392")
  @Test
  public void testStaticMethod_withStaticImport_withStaticImportReplacement() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.test;",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(",
            "      replacement = \"after(value)\", ",
            "      staticImports = {\"com.google.test.Client.after\"})",
            "  public static void before(int value) {",
            "    after(value);",
            "  }",
            "  public static void after(int value) {",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "import static com.google.test.Client.before;",
            "public final class Caller {",
            "  public void doTest() {",
            "    before(42);",
            "  }",
            "}")
        .addOutputLines(
            "Caller.java",
            "import static com.google.test.Client.after;",
            "import static com.google.test.Client.before;",
            "public final class Caller {",
            "  public void doTest() {",
            "    after(42);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInstanceMethodCalledBySubtype() {
    refactoringTestHelper
        .addInputLines(
            "Parent.java",
            "package com.google.test;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public class Parent {",
            "  @Deprecated",
            "  @InlineMe(",
            "      replacement = \"this.after(Duration.ofMillis(value))\", ",
            "      imports = {\"java.time.Duration\"})",
            "  protected final void before(int value) {",
            "    after(Duration.ofMillis(value));",
            "  }",
            // TODO(b/187169365): Validator currently doesn't like inlining non-public members.
            //   Consider allowing protected members if the method being inlined is also protected?
            "  public void after(Duration value) {",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Child.java",
            "package com.google.test;",
            "public final class Child extends Parent {",
            "  public void doTest() {",
            "    before(42);",
            "  }",
            "}")
        .addOutputLines(
            "Child.java",
            "package com.google.test;",
            "import java.time.Duration;",
            "public final class Child extends Parent {",
            "  public void doTest() {",
            "    after(Duration.ofMillis(42));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testConstructorCalledBySubtype() {
    refactoringTestHelper
        .addInputLines(
            "Parent.java",
            "package com.google.test;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public class Parent {",
            "  @Deprecated",
            "  @InlineMe(",
            "      replacement = \"this(Duration.ofMillis(value))\", ",
            "      imports = {\"java.time.Duration\"})",
            "  protected Parent(int value) {",
            "    this(Duration.ofMillis(value));",
            "  }",
            // TODO(b/187169365): Validator currently doesn't like inlining non-public members.
            //   Consider allowing protected members if the method being inlined is also protected?
            "  public Parent(Duration value) {",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Child.java",
            "package com.google.test;",
            "public final class Child extends Parent {",
            "  public Child() {",
            "    super(42);",
            "  }",
            "}")
        .addOutputLines(
            "Child.java",
            "package com.google.test;",
            "import java.time.Duration;",
            "public final class Child extends Parent {",
            "  public Child() {",
            "    super(Duration.ofMillis(42));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFluentMethodChain() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this.baz()\")",
            "  public Client foo() {",
            "    return baz();",
            "  }",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this.baz()\")",
            "  public Client bar() {",
            "    return baz();",
            "  }",
            "  public Client baz() {",
            "    return this;",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client().foo().bar();",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client().baz().baz();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInliningWithField() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this.setTimeout(Duration.ZERO)\", imports ="
                + " {\"java.time.Duration\"})",
            "  public void clearTimeout() {",
            "    setTimeout(Duration.ZERO);",
            "  }",
            "  public void setTimeout(Duration timeout) {",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    new Client().clearTimeout();",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "import java.time.Duration;",
            "public final class Caller {",
            "  public void doTest() {",
            "    new Client().setTimeout(Duration.ZERO);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testReturnThis() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this\")",
            "  public Client noOp() {",
            "    return this;",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    client = client.noOp();",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    client = client;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testReturnThis_preChained() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this\")",
            "  public Client noOp() {",
            "    return this;",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client().noOp();",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testReturnThis_postChained() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this\")",
            "  public Client noOp() {",
            "    return this;",
            "  }",
            "  public void bar() {",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    new Client().noOp().bar();",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    new Client().bar();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testReturnThis_alone() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this\")",
            "  public Client noOp() {",
            "    return this;",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    client.noOp();",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inlineUnvalidatedInline() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package foo;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import com.google.errorprone.annotations.InlineMeValidationDisabled;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMeValidationDisabled(\"Migrating to factory method\")",
            "  @InlineMe(replacement = \"Client.create()\", imports = \"foo.Client\")",
            "  public Client() {}",
            "  ",
            // The Inliner wants to inline the body of this factory method to the factory method :)
            "  @SuppressWarnings(\"InlineMeInliner\")",
            "  public static Client create() { return new Client(); }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "import foo.Client;",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "import foo.Client;",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = Client.create();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inlineUnvalidatedInlineMessage() {
    CompilationTestHelper.newInstance(Inliner.class, getClass())
        .addSourceLines(
            "Client.java",
            "package foo;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import com.google.errorprone.annotations.InlineMeValidationDisabled;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMeValidationDisabled(\"Migrating to factory method\")",
            "  @InlineMe(replacement = \"Client.create()\", imports = \"foo.Client\")",
            "  public Client() {}",
            "  ",
            // The Inliner wants to inline the body of this factory method to the factory method :)
            "  @SuppressWarnings(\"InlineMeInliner\")",
            "  public static Client create() { return new Client(); }",
            "}")
        .addSourceLines(
            "Caller.java",
            "import foo.Client;",
            "public final class Caller {",
            "  public void doTest() {",
            "    // BUG: Diagnostic contains: NOTE: this is an unvalidated inlining!"
                + " Reasoning: Migrating to factory method",
            "    Client client = new Client();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testVarargs() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this.after(inputs)\")",
            "  public void before(int... inputs) {",
            "    after(inputs);",
            "  }",
            "  public void after(int... inputs) {}",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this.after(inputs)\")",
            "  public void extraBefore(int first, int... inputs) {",
            "    after(inputs);",
            "  }",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this.after(first)\")",
            "  public void ignoreVarargs(int first, int... inputs) {",
            "    after(first);",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    client.before(1);",
            "    client.before();",
            "    client.before(1, 2, 3);",
            "    client.extraBefore(42, 1);",
            "    client.extraBefore(42);",
            "    client.extraBefore(42, 1, 2, 3);",
            "    client.ignoreVarargs(42, 1);",
            "    client.ignoreVarargs(42);",
            "    client.ignoreVarargs(42, 1, 2, 3);",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    client.after(1);",
            "    client.after();",
            "    client.after(1, 2, 3);",
            "    client.after(1);",
            "    client.after();",
            "    client.after(1, 2, 3);",
            "    client.after(42);",
            "    client.after(42);",
            "    client.after(42);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testVarargsWithPrecedingElements() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this.after(first, inputs)\")",
            "  public void before(int first, int... inputs) {",
            "    after(first, inputs);",
            "  }",
            "  public void after(int first, int... inputs) {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    client.before(1);",
            "    client.before(1, 2, 3);",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    client.after(1);",
            "    client.after(1, 2, 3);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testReplaceWithJustParameter() {
    refactoringTestHelper
        .allowBreakingChanges()
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"x\")",
            "  public final int identity(int x) {",
            "    return x;",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    int x = client.identity(42);",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            // TODO(b/180976346): replacements of the form that terminate in a parameter by itself
            //  don't work with the new replacement tool, but this is uncommon enough
            "    int x = client.identity(42);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testOrderOfOperations() {
    refactoringTestHelper
        .allowBreakingChanges()
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"x * y\")",
            "  public int multiply(int x, int y) {",
            "    return x * y;",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    int x = client.multiply(5, 10);",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            // TODO(kak): hmm, why don't we inline this?
            "    int x = client.multiply(5, 10);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testOrderOfOperationsWithParamAddition() {
    refactoringTestHelper
        .allowBreakingChanges()
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"x * y\")",
            "  public int multiply(int x, int y) {",
            "    return x * y;",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    int x = client.multiply(5 + 3, 10);",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            // TODO(kak): hmm, why don't we inline this?
            "    int x = client.multiply(5 + 3, 10);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testOrderOfOperationsWithTrailingOperand() {
    refactoringTestHelper
        .allowBreakingChanges()
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"x * y\")",
            "  public int multiply(int x, int y) {",
            "    return x * y;",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    int x = client.multiply(5 + 3, 10) * 5;",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            // TODO(kak): hmm, why don't we inline this?
            "    int x = client.multiply(5 + 3, 10) * 5;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testSublist() {
    refactoringTestHelper
        .setArgs("-XepOpt:" + InlinabilityResult.DISALLOW_ARGUMENT_REUSE + "=false")
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.util.List;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"list.subList(list.size() - n, list.size())\")",
            "  public List<String> last(List<String> list, int n) {",
            "    return list.subList(list.size() - n, list.size());",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "public final class Caller {",
            "  public void doTest() {",
            "    List<String> list = new ArrayList<>();",
            "    list.add(\"hi\");",
            "    Client client = new Client();",
            "    List<String> result = client.last(list, 1);",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "public final class Caller {",
            "  public void doTest() {",
            "    List<String> list = new ArrayList<>();",
            "    list.add(\"hi\");",
            "    Client client = new Client();",
            "    List<String> result = list.subList(list.size() - 1, list.size());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testSublistPassingMethod() {
    refactoringTestHelper
        .setArgs("-XepOpt:" + InlinabilityResult.DISALLOW_ARGUMENT_REUSE + "=false")
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.util.List;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"list.subList(list.size() - n, list.size())\")",
            "  public List<String> last(List<String> list, int n) {",
            "    return list.subList(list.size() - n, list.size());",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    List<String> result = client.last(getList(), 1);",
            "  }",
            "  public List<String> getList() {",
            "    List<String> list = new ArrayList<>();",
            "    list.add(\"hi\");",
            "    return list;",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            // TODO(b/188184784): this is a bug, as there's no guarantee that getList() returns the
            // same list every time (or it may be expensive!)
            "    List<String> result = getList().subList(getList().size() - 1, getList().size());",
            "  }",
            "  public List<String> getList() {",
            "    List<String> list = new ArrayList<>();",
            "    list.add(\"hi\");",
            "    return list;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testBooleanParameterWithInlineComment() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @InlineMe(replacement = \"this.after(/* isAdmin = */ isAdmin)\")",
            "  @Deprecated",
            "  public void before(boolean isAdmin) {",
            "    after(/* isAdmin= */ isAdmin);",
            "  }",
            "  public void after(boolean isAdmin) {",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    client.before(false);",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            // TODO(b/189535612): this is a bug!
            "    client.after(/* false = */ false);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testTrailingSemicolon() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @InlineMe(replacement = \"this.after(/* foo= */ isAdmin);;;;\")",
            "  @Deprecated",
            "  public boolean before(boolean isAdmin) {",
            "    return after(/* foo= */ isAdmin);",
            "  }",
            "  public boolean after(boolean isAdmin) { return isAdmin; }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    boolean x = (client.before(false) || true);",
            "  }",
            "}")
        .addOutputLines(
            "out/Caller.java",
            "public final class Caller {",
            "  public void doTest() {",
            "    Client client = new Client();",
            "    boolean x = (client.after(/* false = */ false) || true);",
            "  }",
            "}")
        .doTest();
  }

  private BugCheckerRefactoringTestHelper buildBugCheckerWithPrefixFlag(String prefix) {
    return BugCheckerRefactoringTestHelper.newInstance(Inliner.class, getClass())
        .setArgs("-XepOpt:" + PREFIX_FLAG + "=" + prefix);
  }
}
