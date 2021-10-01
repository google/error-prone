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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for the {@link Suggester}. */
@RunWith(JUnit4.class)
public class SuggesterTest {
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(Suggester.class, getClass());

  @Test
  public void testBuildAnnotation_withImports() {
    assertThat(
            InlineMeData.buildAnnotation(
                "REPLACEMENT",
                ImmutableSet.of("java.time.Duration", "java.time.Instant"),
                ImmutableSet.of()))
        .isEqualTo(
            "@InlineMe(replacement = \"REPLACEMENT\", "
                + "imports = {\"java.time.Duration\", \"java.time.Instant\"})\n");
  }

  @Test
  public void testBuildAnnotation_withSingleImport() {
    assertThat(
            InlineMeData.buildAnnotation(
                "REPLACEMENT", ImmutableSet.of("java.time.Duration"), ImmutableSet.of()))
        .isEqualTo(
            "@InlineMe(replacement = \"REPLACEMENT\", " + "imports = \"java.time.Duration\")\n");
  }

  @Test
  public void testInstanceMethodNewImport() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import java.time.Duration;",
            "public final class Client {",
            "  private Duration deadline = Duration.ofSeconds(5);",
            "  @Deprecated",
            "  public void setDeadline(long millis) {",
            "    setDeadline(Duration.ofMillis(millis));",
            "  }",
            "  public void setDeadline(Duration deadline) {",
            "    this.deadline = deadline;",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public final class Client {",
            "  private Duration deadline = Duration.ofSeconds(5);",
            "  @InlineMe(replacement = \"this.setDeadline(Duration.ofMillis(millis))\","
                + " imports = \"java.time.Duration\")",
            "  @Deprecated",
            "  public void setDeadline(long millis) {",
            "    setDeadline(Duration.ofMillis(millis));",
            "  }",
            "  public void setDeadline(Duration deadline) {",
            "    this.deadline = deadline;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testStaticMethodInNewClass() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import java.time.Duration;",
            "public final class Client {",
            "  @Deprecated",
            "  public Duration fromMillis(long millis) {",
            "    return Duration.ofMillis(millis);",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public final class Client {",
            "  @InlineMe(",
            "      replacement = \"Duration.ofMillis(millis)\", ",
            "      imports = \"java.time.Duration\")",
            "  @Deprecated",
            "  public Duration fromMillis(long millis) {",
            "    return Duration.ofMillis(millis);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testProtectedConstructor() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  @Deprecated",
            "  protected Client() {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testReturnField() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import java.time.Duration;",
            "public final class Client {",
            "  @Deprecated",
            "  public Duration getZero() {",
            "    return Duration.ZERO;",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public final class Client {",
            "  @InlineMe(replacement = \"Duration.ZERO\", imports = \"java.time.Duration\")",
            "  @Deprecated",
            "  public Duration getZero() {",
            "    return Duration.ZERO;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testImplementationSplitOverMultipleLines() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import java.time.Duration;",
            "import java.time.Instant;",
            "public final class Client {",
            "  @Deprecated",
            "  public Duration getElapsed() {",
            "    return Duration.between(",
            "        Instant.ofEpochMilli(42),",
            "        Instant.now());",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "import java.time.Instant;",
            "public final class Client {",
            "  @InlineMe(",
            "      replacement = \"Duration.between(Instant.ofEpochMilli(42), Instant.now())\", ",
            "      imports = {\"java.time.Duration\", \"java.time.Instant\"})",
            "  @Deprecated",
            "  public Duration getElapsed() {",
            "    return Duration.between(",
            "        Instant.ofEpochMilli(42),",
            "        Instant.now());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAnonymousClass() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  @Deprecated",
            "  public Object getUselessObject() {",
            "    return new Object() {",
            "      @Override",
            "      public int hashCode() {",
            "        return 42;",
            "      }",
            "    };",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testMethodReference() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import java.time.Duration;",
            "import java.util.Optional;",
            "public final class Client {",
            "  @Deprecated",
            "  public Optional<Duration> silly(Optional<Long> input) {",
            "    return input.map(Duration::ofMillis);",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "import java.util.Optional;",
            "public final class Client {",
            "  @InlineMe(replacement = \"input.map(Duration::ofMillis)\", ",
            "      imports = \"java.time.Duration\")",
            "  @Deprecated",
            "  public Optional<Duration> silly(Optional<Long> input) {",
            "    return input.map(Duration::ofMillis);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNewClass() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import org.joda.time.Instant;",
            "public final class Client {",
            "  @Deprecated",
            "  public Instant silly() {",
            "    return new Instant();",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import org.joda.time.Instant;",
            "public final class Client {",
            "  @InlineMe(replacement = \"new Instant()\", imports = \"org.joda.time.Instant\")",
            "  @Deprecated",
            "  public Instant silly() {",
            "    return new Instant();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNewArray() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import org.joda.time.Instant;",
            "public final class Client {",
            "  @Deprecated",
            "  public Instant[] silly() {",
            "    return new Instant[42];",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import org.joda.time.Instant;",
            "public final class Client {",
            "  @InlineMe(replacement = \"new Instant[42]\", imports = \"org.joda.time.Instant\")",
            "  @Deprecated",
            "  public Instant[] silly() {",
            "    return new Instant[42];",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNewNestedClass() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  @Deprecated",
            "  public NestedClass silly() {",
            "    return new NestedClass();",
            "  }",
            "  public static class NestedClass {}",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @InlineMe(replacement = \"new NestedClass()\", ",
            "      imports = \"com.google.frobber.Client.NestedClass\")",
            "  @Deprecated",
            "  public NestedClass silly() {",
            "    return new NestedClass();",
            "  }",
            "  public static class NestedClass {}",
            "}")
        .doTest();
  }

  @Test
  public void testReturnStringLiteral() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  @Deprecated",
            "  public String getName() {",
            "    return \"kurt\";",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @InlineMe(replacement = \"\\\"kurt\\\"\")",
            "  @Deprecated",
            "  public String getName() {",
            "    return \"kurt\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testCallMethodWithStringLiteral() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  @Deprecated",
            "  public String getName() {",
            "    return getName(\"kurt\");",
            "  }",
            "  public String getName(String defaultValue) {",
            "    return \"test\";",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @InlineMe(replacement = \"this.getName(\\\"kurt\\\")\")",
            "  @Deprecated",
            "  public String getName() {",
            "    return getName(\"kurt\");",
            "  }",
            "  public String getName(String defaultValue) {",
            "    return \"test\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testReturnPrivateVariable() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import java.time.Duration;",
            "public final class Client {",
            "  private final Duration myDuration = Duration.ZERO;",
            "  @Deprecated",
            "  public Duration getMyDuration() {",
            "    return myDuration;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testReturnPrivateVariable_qualifiedWithThis() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import java.time.Duration;",
            "public final class Client {",
            "  private final Duration myDuration = Duration.ZERO;",
            "  @Deprecated",
            "  public Duration getMyDuration() {",
            "    return this.myDuration;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testSettingPrivateVariable() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import java.time.Duration;",
            "public final class Client {",
            "  private Duration duration = Duration.ZERO;",
            "  @Deprecated",
            "  public void setDuration(Duration duration) {",
            "    this.duration = duration;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testDelegateToParentClass() {
    refactoringTestHelper
        .addInputLines(
            "Parent.java",
            "package com.google.frobber;",
            "import java.time.Duration;",
            "public class Parent {",
            "  private Duration duration = Duration.ZERO;",
            "  public final Duration after() {",
            "    return duration;",
            "  }",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import java.time.Duration;",
            "public final class Client extends Parent {",
            "  private Duration duration = Duration.ZERO;",
            "  @Deprecated",
            "  public final Duration before() {",
            "    return after();",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public final class Client extends Parent {",
            "  private Duration duration = Duration.ZERO;",
            "  @InlineMe(replacement = \"this.after()\")",
            "  @Deprecated",
            "  public final Duration before() {",
            "    return after();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testWithCast() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import java.time.Duration;",
            "public final class Client {",
            "  @Deprecated",
            "  public void setDuration(Object duration) {",
            "    foo((Duration) duration);",
            "  }",
            "  public void foo(Duration duration) {",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public final class Client {",
            "  @InlineMe(replacement = \"this.foo((Duration) duration)\", imports ="
                + " \"java.time.Duration\")",
            "  @Deprecated",
            "  public void setDuration(Object duration) {",
            "    foo((Duration) duration);",
            "  }",
            "  public void foo(Duration duration) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAccessPrivateVariable() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import java.time.Duration;",
            "public final class Client {",
            "  private final Duration myDuration = Duration.ZERO;",
            "  @Deprecated",
            "  public boolean silly() {",
            "    return myDuration.isZero();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testAccessPrivateMethod() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            "  @Deprecated",
            "  public boolean silly() {",
            "    return privateDelegate();",
            "  }",
            "  private boolean privateDelegate() {",
            "    return false;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testTryWithResources() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "import java.io.BufferedReader;",
            "import java.io.FileReader;",
            "import java.io.IOException;",
            "public class Client {",
            "  @Deprecated",
            "  public String readLine(String path) throws IOException {",
            "    try (BufferedReader br = new BufferedReader(new FileReader(path))) {",
            "      return br.readLine();",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testIfStatement() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "public class Client {",
            "  @Deprecated",
            "  public void foo(String input) {",
            "    if (input.equals(\"hi\")) {",
            "      return;",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testNestedBlock() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "public class Client {",
            "  @Deprecated",
            "  public String foo(String input) {",
            "    {",
            "      return input.toLowerCase();",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testTernaryOverMultipleLines() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import java.time.Duration;",
            "public final class Client {",
            "  @Deprecated",
            "  public Duration getDeadline(Duration deadline) {",
            "    return deadline.compareTo(Duration.ZERO) > 0",
            "        ? Duration.ofSeconds(42)",
            "        : Duration.ZERO;",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public final class Client {",
            "  @InlineMe(replacement = \"deadline.compareTo(Duration.ZERO) > 0 ?"
                + " Duration.ofSeconds(42) : Duration.ZERO\", ",
            "imports = \"java.time.Duration\")",
            "  @Deprecated",
            "  public Duration getDeadline(Duration deadline) {",
            "    return deadline.compareTo(Duration.ZERO) > 0",
            "        ? Duration.ofSeconds(42)",
            "        : Duration.ZERO;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testStaticCallingAnotherQualifiedStatic() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import java.time.Duration;",
            "public final class Client {",
            "  @Deprecated",
            "  public static Duration getDeadline() {",
            "    return Client.getDeadline2();",
            "  }",
            "  public static Duration getDeadline2() {",
            "    return Duration.ZERO;",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.time.Duration;",
            "public final class Client {",
            "  @InlineMe(replacement = \"Client.getDeadline2()\", ",
            "      imports = \"com.google.frobber.Client\")",
            "  @Deprecated",
            "  public static Duration getDeadline() {",
            "    return Client.getDeadline2();",
            "  }",
            "  public static Duration getDeadline2() {",
            "    return Duration.ZERO;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void staticReferenceToJavaLang() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import static java.lang.String.format;",
            "public final class Client {",
            "  @Deprecated",
            "  public static String myFormat(String template, String arg) {",
            "    return format(template, arg);",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import static java.lang.String.format;",
            "import com.google.errorprone.annotations.InlineMe;",
            "public final class Client {",
            "  @InlineMe(replacement = \"format(template, arg)\", staticImports ="
                + " \"java.lang.String.format\")",
            "  @Deprecated",
            "  public static String myFormat(String template, String arg) {",
            "    return format(template, arg);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void replacementContainsGenericInvocation() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "public final class Client {",
            "  @Deprecated",
            "  public static List<Void> newArrayList() {",
            "    return new ArrayList<Void>();",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "public final class Client {",
            "  @InlineMe(replacement = \"new ArrayList<Void>()\", imports ="
                + " \"java.util.ArrayList\")",
            "  @Deprecated",
            "  public static List<Void> newArrayList() {",
            "    return new ArrayList<Void>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suggestedFinalOnOtherwiseGoodMethod() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public class Client {",
            "  @Deprecated",
            "  public int method() {",
            "    return 42;",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "public class Client {",
            "  @InlineMe(replacement = \"42\")",
            "  @Deprecated",
            "  public final int method() {",
            "    return 42;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void dontSuggestOnDefaultMethods() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public interface Client {",
            "  @Deprecated",
            "  public default int method() {",
            "    return 42;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  // Since constructors can't be "overridden" in the same way as other non-final methods, it's
  // OK to inline them even if there could be a subclass of the surrounding class.
  @Test
  public void deprecatedConstructorInNonFinalClass() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public class Client {",
            "  @Deprecated",
            "  public Client() {",
            "    this(42);",
            "  }",
            "  public Client(int value) {}",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.InlineMe;",
            "public class Client {",
            "  @InlineMe(replacement = \"this(42)\")",
            "  @Deprecated",
            "  public Client() {",
            "    this(42);",
            "  }",
            "  public Client(int value) {}",
            "}")
        .doTest();
  }

  @Test
  public void publicStaticFactoryCallsPrivateConstructor() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public class Client {",
            "  @Deprecated",
            "  public static Client create() {",
            "    return new Client();",
            "  }",
            "  private Client() {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void deprecatedMethodWithDoNotCall() {
    refactoringTestHelper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.DoNotCall;",
            "public class Client {",
            "  @DoNotCall",
            "  @Deprecated",
            "  public void before() {",
            "    after();",
            "  }",
            "  public void after() {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testCustom() {
    refactoringTestHelper
        .addInputLines(
            "InlineMe.java", //
            "package bespoke;",
            "public @interface InlineMe {",
            "  String replacement();",
            "  String[] imports() default {};",
            "  String[] staticImports() default {};",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import java.time.Duration;",
            "import java.util.Optional;",
            "public final class Client {",
            "  @Deprecated",
            "  public Optional<Duration> silly(Optional<Long> input) {",
            "    return input.map(Duration::ofMillis);",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import bespoke.InlineMe;",
            "import java.time.Duration;",
            "import java.util.Optional;",
            "public final class Client {",
            "  @InlineMe(replacement = \"input.map(Duration::ofMillis)\", ",
            "      imports = \"java.time.Duration\")",
            "  @Deprecated",
            "  public Optional<Duration> silly(Optional<Long> input) {",
            "    return input.map(Duration::ofMillis);",
            "  }",
            "}")
        .setArgs("-XepOpt:InlineMe:annotation=bespoke.InlineMe")
        .doTest();
  }
}
