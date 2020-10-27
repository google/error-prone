/*
 * Copyright 2012 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author alexeagle@google.com (Alex Eagle) */
@RunWith(JUnit4.class)
public class ReturnValueIgnoredTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ReturnValueIgnored.class, getClass());

  @Test
  public void testPositiveCases() {
    compilationHelper.addSourceFile("ReturnValueIgnoredPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("ReturnValueIgnoredNegativeCases.java").doTest();
  }

  @Test
  public void function() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  void f(Function<Integer, Integer> f) {",
            "    // BUG: Diagnostic contains:",
            "    f.apply(0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void consumer() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Consumer;",
            "class Test {",
            "  void f(Consumer<Integer> f) {",
            "    f.accept(0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void functionVoid() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  void f(Function<Integer, Void> f) {",
            "    f.apply(0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreInTests() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static org.junit.Assert.fail;",
            "import java.util.function.Function;",
            "class Test {",
            "  void f(Function<Integer, Integer> f) {",
            "    try {",
            "      f.apply(0);",
            "      fail();",
            "    } catch (Exception expected) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void stream() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains:",
            "    \"\".codePoints().count();",
            "    \"\".codePoints().forEach(i -> {});",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void javaTime() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.time.Duration;",
            "import java.time.LocalDate;",
            "import java.time.ZoneId;",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Duration.ZERO.plusDays(2);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Duration.ZERO.toDays();",
            // We ignore parse() methods on java.time types
            "    Duration.parse(\"PT20.345S\");",
            "    LocalDate.parse(\"2007-12-03\");",
            // We ignore of() methods on java.time types
            "    LocalDate.of(1985, 5, 31);",
            // We ignore ZoneId.of() -- it's effectively a parse() method
            "    ZoneId.of(\"America/New_York\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void optionalStaticMethods() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  void optional() {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Optional.empty();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Optional.of(42);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    Optional.ofNullable(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void optionalInstanceMethods() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  void optional() {",
            "    Optional<Integer> optional = Optional.of(42);",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    optional.isPresent();",
            "    optional.filter(v -> v > 40);",
            "    optional.map(v -> Integer.toString(v));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void issue1565_enumDeclaration() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "enum Test {",
            "  A;",
            "  void f(Function<Integer, Integer> f) {",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    f.apply(0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void issue1363_dateTimeFormatterBuilder() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.time.format.DateTimeFormatterBuilder;",
            "class Test {",
            "  void f() {",
            "    DateTimeFormatterBuilder formatter = new DateTimeFormatterBuilder();",
            "    formatter.appendZoneId();",
            "    formatter.optionalEnd();",
            "    formatter.padNext(5);",
            "    formatter.parseCaseSensitive();",
            "    // BUG: Diagnostic contains: ReturnValueIgnored",
            "    formatter.toFormatter();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void issue876() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.nio.file.Path;",
            "abstract class Test {",
            "  void test(Path p) {",
            "    // BUG: Diagnostic contains:",
            "    E e = p::toRealPath;",
            "  }",
            "  abstract <T> void a(T t);",
            "  public interface E {",
            "    void run() throws Exception;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void collectionContains() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  void test(java.util.List p) {",
            "    // BUG: Diagnostic contains:",
            "    p.contains(null);",
            "  }",
            "  void test2(java.util.Map p) {",
            "    // BUG: Diagnostic contains:",
            "    p.containsKey(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodReferenceToObject() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "abstract class Test {",
            "  void test(Function<Integer, Long> fn) {",
            "    foo(fn::apply);",
            "  }",
            "  void foo(Function<Integer, Object> fn) {",
            "  }",
            "}")
        .doTest();
  }
}
