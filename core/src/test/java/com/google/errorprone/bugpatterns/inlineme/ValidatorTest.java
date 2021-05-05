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

import com.google.errorprone.CompilationTestHelper;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for the {@code @InlineMe} {@link Validator}. */
@RunWith(JUnit4.class)
public class ValidatorTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(Validator.class, getClass());

  @Test
  public void testStaticFactoryToConstructor() {
    helper
        .addSourceLines(
            "Client.java",
            "package com.google.foo;",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @InlineMe(replacement = \"new Client();\", imports ={\"com.google.foo.Client\"})",
            "  @Deprecated",
            "  public static Client create() {",
            "    return new Client();",
            "  }",
            "  public Client() {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void annotationWithImportsAsASingleMember() {
    helper
        .addSourceLines(
            "Client.java",
            "package com.google.foo;",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @InlineMe(replacement = \"new Client();\", imports = \"com.google.foo.Client\")",
            "  @Deprecated",
            "  public static Client create() {",
            "    return new Client();",
            "  }",
            "  public Client() {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void arrayConstructor_fromInstanceMethod() {
    helper
        .addSourceLines(
            "Foo.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public final class Foo {",
            "  @InlineMe(replacement = \"new Duration[size]\", ",
            "      imports = {\"java.time.Duration\"})",
            "  @Deprecated",
            "  public Duration[] someDurations(int size) {",
            "    return new Duration[size];",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void arrayConstructor_fromStaticMethod() {
    helper
        .addSourceLines(
            "Foo.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public final class Foo {",
            "  @InlineMe(replacement = \"new Duration[size]\", ",
            "      imports = {\"java.time.Duration\"})",
            "  @Deprecated",
            "  public static Duration[] someDurations(int size) {",
            "    return new Duration[size];",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInstanceMethod_withInlineComment() {
    helper
        .addSourceLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.util.function.Supplier;",
            "public final class Client {",
            "  @InlineMe(replacement = \"this.after(string);\")",
            "  @Deprecated",
            "  public void before(String string) {",
            "    after(/* string= */ string);",
            "  }",
            "  public void after(String string) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInstanceMethod_withTrailingComment() {
    helper
        .addSourceLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.util.function.Supplier;",
            "public final class Client {",
            "  @InlineMe(replacement = \"this.after(string);\")",
            "  @Deprecated",
            "  public void before(String string) {",
            "    after( // string",
            "        string);",
            "  }",
            "  public void after(String string) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInstanceMethod_withLambda() {
    helper
        .addSourceLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.util.function.Supplier;",
            "public final class Client {",
            "  @InlineMe(replacement = \"this.after(() -> string);\")",
            "  @Deprecated",
            "  // BUG: Diagnostic contains: evaluation timing",
            "  public void before(String string) {",
            "    after(() -> string);",
            "  }",
            "  public void after(Supplier<String> supplier) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInstanceMethod_withLambdaAndVariable() {
    helper
        .addSourceLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.util.function.Function;",
            "public final class Client {",
            "  @InlineMe(replacement = \"this.after(str -> Integer.parseInt(str));\")",
            "  @Deprecated",
            "  public void before() {",
            "    after(str -> Integer.parseInt(str));",
            "  }",
            "  public void after(Function<String, Integer> function) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInstanceMethod_privateVariable() {
    helper
        .addSourceLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  private final String str = null;",
            "  @InlineMe(replacement = \"str;\")",
            "  @Deprecated",
            "  // BUG: Diagnostic contains: references deprecated or non-public APIs",
            "  public String before() {",
            "    return str;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInstanceMethod_publicVariable() {
    helper
        .addSourceLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  public final String str = null;",
            "  @InlineMe(replacement = \"this.str;\")",
            "  @Deprecated",
            "  public String before() {",
            "    return str;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInstanceMethod_privateMethod() {
    helper
        .addSourceLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @InlineMe(replacement = \"this.privateMethod();\")",
            "  @Deprecated",
            "  // BUG: Diagnostic contains: references deprecated or non-public APIs",
            "  public String before() {",
            "    return privateMethod();",
            "  }",
            "  private String privateMethod() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInstanceMethod_splitOverMultipleLines_withLambda() {
    helper
        .addSourceLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.util.function.Supplier;",
            "public final class Client {",
            "  @InlineMe(replacement = \"this.after(() -> string);\")",
            "  @Deprecated",
            "  // BUG: Diagnostic contains: evaluation timing",
            "  public void before(String string) {",
            "    after(() ->",
            "      string);",
            "  }",
            "  public void after(Supplier<String> supplier) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testConstructor() {
    helper
        .addSourceLines(
            "ProfileTimer.java",
            "import com.google.common.base.Ticker;",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class ProfileTimer {",
            "  @InlineMe(",
            "      replacement = \"this(Ticker.systemTicker(), name)\", ",
            "      imports = {\"com.google.common.base.Ticker\"})",
            "  @Deprecated",
            "  public ProfileTimer(String name) {",
            "    this(Ticker.systemTicker(), name);",
            "  }",
            "  public ProfileTimer(Ticker ticker, String name) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMissingImport() {
    helper
        .addSourceLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public final class Client {",
            "  @InlineMe(replacement = \"this.setDeadline(Duration.ofMillis(millis))\")",
            "  @Deprecated",
            "  // BUG: Diagnostic matches: BAR",
            "  public void setDeadlineInMillis(long millis) {",
            "    this.setDeadline(Duration.ofMillis(millis));",
            "  }",
            "  public void setDeadline(Duration deadline) {",
            "  }",
            "}")
        .expectErrorMessage(
            "BAR",
            str ->
                str.contains("InferredFromBody: [java.time.Duration")
                    && str.contains("FromAnnotation: []"))
        .doTest();
  }

  @Test
  public void testFullQualifiedReplacementType() {
    helper
        .addSourceLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @InlineMe(replacement = \"this.setDeadline(java.time.Duration.ofMillis(millis))\")",
            "  @Deprecated",
            "  public void setDeadlineInMillis(long millis) {",
            "    this.setDeadline(java.time.Duration.ofMillis(millis));",
            "  }",
            "  public void setDeadline(java.time.Duration deadline) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testReplacementWithJavaLangClass() {
    helper
        .addSourceLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this.after(String.valueOf(value))\")",
            "  public void before(long value) {",
            "    after(String.valueOf(value));",
            "  }",
            "  public void after(String string) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testStringLiteralThatLooksLikeAMethodCall() {
    helper
        .addSourceLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @InlineMe(replacement = \"this.after(\\\"not actually() a method call\\\")\")",
            "  @Deprecated",
            "  public void before() {",
            "    after(\"not actually() a method call\");",
            "  }",
            "  public void after(String string) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testTryStatement() {
    helper
        .addSourceLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @InlineMe(replacement = \"try { int x = 0; } catch (RuntimeException e) {}\")",
            "  @Deprecated",
            "  // BUG: Diagnostic contains: InlineMe cannot inline complex statements",
            "  public void before() {",
            "    try { int x = 0; } catch (RuntimeException e) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNoOpMethod() {
    helper
        .addSourceLines(
            "RpcClient.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public final class RpcClient {",
            "  @InlineMe(replacement = \"\")",
            "  @Deprecated",
            "  // BUG: Diagnostic contains: cannot inline methods with more than 1 statement",
            "  public void setDeadline(org.joda.time.Duration deadline) {}",
            "  public void setDeadline(Duration deadline) {}",
            "}")
        .doTest();
  }

  @Test
  public void testInstanceMethod_withConstant() {
    helper
        .addSourceLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public final class Client {",
            "  @InlineMe(replacement = \"this.after(Duration.ZERO);\", ",
            "      imports = {\"java.time.Duration\"})",
            "  @Deprecated",
            "  public void before() {",
            "    after(Duration.ZERO);",
            "  }",
            "  public void after(Duration duration) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInstanceMethod_withConstantStaticallyImported() {
    helper
        .addSourceLines(
            "Client.java",
            "import static java.time.Duration.ZERO;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public final class Client {",
            "  @InlineMe(replacement = \"this.after(ZERO);\", ",
            "      staticImports = {\"java.time.Duration.ZERO\"})",
            "  @Deprecated",
            "  public void before() {",
            "    after(ZERO);",
            "  }",
            "  public void after(Duration duration) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testStaticMethod_typeParameter() {
    helper
        .addSourceLines(
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
        .doTest();
  }

  @Test
  public void testAllowingNestedClassImport() {
    helper
        .addSourceLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public final class Client {",
            "  public static final class Builder {",
            "    @InlineMe(",
            "        replacement = \"this.setDeadline(Client.Builder.parseDeadline(deadline));\",",
            "        imports = {\"com.google.frobber.Client\"})",
            "    @Deprecated",
            // TODO(b/176094331): we shouldn't need to import `Builder`
            "    // BUG: Diagnostic matches: BAR",
            "    public void setDeadline(String deadline) {",
            "      setDeadline(parseDuration(deadline));",
            "    }",
            "    public void setDeadline(Duration deadline) {",
            "    }",
            "    public static Duration parseDuration(String string) {",
            "      return Duration.parse(string);",
            "    }",
            "  }",
            "}")
        .expectErrorMessage(
            "BAR", Pattern.compile("InferredFromBody: .*\\.Builder]").asPredicate()::test)
        .doTest();
  }

  @Test
  public void testNestedClassWithInstanceMethodCallingStatic_implementationQualified() {
    helper
        .addSourceLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public final class Client {",
            "  public static final class Builder {",
            "    @InlineMe(",
            "        replacement = \"this.setDeadline(Client.Builder.parseDuration(deadline));\",",
            "        imports = {\"com.google.frobber.Client\"})",
            "    @Deprecated",
            "    public void setDeadline(String deadline) {",
            "      setDeadline(Client.Builder.parseDuration(deadline));",
            "    }",
            "    public void setDeadline(Duration deadline) {",
            "    }",
            "    public static Duration parseDuration(String string) {",
            "      return Duration.parse(string);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAssignmentToPrivateField() {
    helper
        .addSourceLines(
            "RpcClient.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class RpcClient {",
            "  private String name;",
            "  @InlineMe(replacement = \"this.name = name;\")",
            "  @Deprecated",
            "  // BUG: Diagnostic contains: references deprecated or non-public APIs",
            "  public void setName(String name) {",
            "    this.name = name;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAssignmentToPublicField() {
    helper
        .addSourceLines(
            "RpcClient.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class RpcClient {",
            "  public String name;",
            "  @InlineMe(replacement = \"this.name = name;\")",
            "  @Deprecated",
            "  public void setName(String name) {",
            "    this.name = name;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppressionWithSuppressWarningsDoesntWork() {
    helper
        .addSourceLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @SuppressWarnings(\"InlineMeValidator\")",
            "  @Deprecated",
            "  @InlineMe(replacement = \"Client.create()\", imports = \"foo.Client\")",
            "  // BUG: Diagnostic contains: cannot be applied",
            "  public Client() {}",
            "  ",
            "  public static Client create() { return new Client(); }",
            "}")
        .doTest();
  }

  @Test
  public void suppressionWithCustomAnnotationWorks() {
    helper
        .addSourceLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "import com.google.errorprone.annotations." + "InlineMeValidationDisabled;",
            "public final class Client {",
            "  @InlineMeValidationDisabled(\"Migrating to factory method\")",
            "  @Deprecated",
            "  @InlineMe(replacement = \"Client.create()\", imports = \"foo.Client\")",
            "  public Client() {}",
            "  ",
            "  public static Client create() { return new Client(); }",
            "}")
        .doTest();
  }

  @Test
  public void varargsPositive() {
    helper
        .addSourceLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this.after(inputs)\")",
            "  public void before(int... inputs) {",
            "    after(inputs);",
            "  }",
            "",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this.after(inputs)\")",
            "  public void extraBefore(int first, int... inputs) {",
            "    after(inputs);",
            "  }",
            "",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this.after(first)\")",
            "  public void ignoringVarargs(int first, int... inputs) {",
            "    after(first);", // Sneaky!
            "  }",
            "  public void after(int... inputs) {}",
            "}")
        .doTest();
  }

  @Test
  public void varargs_toNonVarargsMethod() {
    helper
        .addSourceLines(
            "Client.java",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @Deprecated",
            "  @InlineMe(replacement = \"this.after(inputs)\")",
            "    // BUG: Diagnostic contains: varargs",
            "  public void before(int... inputs) {",
            "    after(inputs);",
            "  }",
            "  public void after(int[] inputs) {}",
            "}")
        .doTest();
  }
}
