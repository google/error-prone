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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link OptionalOfRedundantMethod}. */
@RunWith(JUnit4.class)
public class OptionalOfRedundantMethodTest {

  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(OptionalOfRedundantMethod.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(OptionalOfRedundantMethod.class, getClass());

  @Test
  public void positive_ifPresent() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " void f() {",
            "    // BUG: Diagnostic contains: ifPresent",
            "    Optional.of(\"test\").ifPresent(String::length);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_ifPresent_refactoring_ofNullableFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " void f() {",
            "    Optional.of(\"test\").ifPresent(String::length);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " void f() {",
            "    Optional.ofNullable(\"test\").ifPresent(String::length);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_orElse() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " void f() {",
            "    // BUG: Diagnostic contains: orElse",
            "    Optional.of(\"test\").orElse(\"test2\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_orElse_refactoring_ofNullableFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " String f() {",
            "    return Optional.of(\"test\").orElse(\"test2\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " String f() {",
            "    return Optional.ofNullable(\"test\").orElse(\"test2\");",
            "  }",
            "}")
        .setFixChooser(FixChoosers.FIRST)
        .doTest();
  }

  @Test
  public void positive_orElse_refactoring_simplifyExpressionFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " String f() {",
            "    return Optional.of(\"test\").orElse(\"test2\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " String f() {",
            "    return \"test\";",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void positive_orElseGet() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " void f() {",
            "    // BUG: Diagnostic contains: orElseGet",
            "    Optional.of(\"test\").orElseGet(() -> \"test2\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_orElseGet_refactoring_ofNullableFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " String f() {",
            "    return Optional.of(\"test\").orElseGet(() -> \"test2\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " String f() {",
            "    return Optional.ofNullable(\"test\").orElseGet(() -> \"test2\");",
            "  }",
            "}")
        .setFixChooser(FixChoosers.FIRST)
        .doTest();
  }

  @Test
  public void positive_orElseGet_refactoring_simplifyExpressionFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " String f() {",
            "    return Optional.of(\"test\").orElseGet(() -> \"test2\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " String f() {",
            "    return \"test\";",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void positive_orElseThrow() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "import com.google.common.collect.ImmutableMap;",
            "class Test {",
            " static String f(ImmutableMap<String,String> map){",
            "    // BUG: Diagnostic contains: orElseThrow",
            "    return Optional.of(map.get(\"test\")).orElseThrow(IllegalArgumentException::new);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_orElseThrow_refactoring_ofNullableFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "import com.google.common.collect.ImmutableMap;",
            "class Test {",
            " static String f(ImmutableMap<String,String> map){",
            "    return Optional.of(map.get(\"test\")).orElseThrow(IllegalArgumentException::new);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Optional;",
            "import com.google.common.collect.ImmutableMap;",
            "class Test {",
            " static String f(ImmutableMap<String,String> map){",
            "    return"
                + " Optional.ofNullable(map.get(\"test\")).orElseThrow(IllegalArgumentException::new);",
            "  }",
            "}")
        .setFixChooser(FixChoosers.FIRST)
        .doTest();
  }

  @Test
  public void positive_orElseThrow_refactoring_simplifyExpressionFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "import com.google.common.collect.ImmutableMap;",
            "class Test {",
            " static String f(ImmutableMap<String,String> map){",
            "    return Optional.of(map.get(\"test\")).orElseThrow(IllegalArgumentException::new);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Optional;",
            "import com.google.common.collect.ImmutableMap;",
            "class Test {",
            " static String f(ImmutableMap<String,String> map){",
            "    return map.get(\"test\");",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void positive_isPresent() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " void f() {",
            "    // BUG: Diagnostic contains: isPresent",
            "    if(Optional.of(\"test\").isPresent()) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_isPresent_refactoring_ofNullableFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " void f() {",
            "    if(Optional.of(\"test\").isPresent()) {}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " void f() {",
            "    if(Optional.ofNullable(\"test\").isPresent()) {}",
            "  }",
            "}")
        .setFixChooser(FixChoosers.FIRST)
        .doTest();
  }

  @Test
  public void positive_isPresent_refactoring_simplifyExpressionFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " void f() {",
            "    if(Optional.of(\"test\").isPresent()) {}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " void f() {",
            "    if(true) {}",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void positive_guavaIsPresent() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            " void f() {",
            "    // BUG: Diagnostic contains: isPresent",
            "    Optional.of(\"test\").isPresent();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_guavaIsPresent_refactoring_fromNullableFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            " void f() {",
            "    Optional.of(\"test\").isPresent();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            " void f() {",
            "    Optional.fromNullable(\"test\").isPresent();",
            "  }",
            "}")
        .setFixChooser(FixChoosers.FIRST)
        .doTest();
  }

  @Test
  public void positive_guavaIsPresent_refactoring_simplifyExpressionFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            " void f() {",
            "    if(Optional.of(\"test\").isPresent()){}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            " void f() {",
            "    if(true){}",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void positive_guavaOr() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            " void f() {",
            "    // BUG: Diagnostic contains: or",
            "    Optional.of(\"test\").or(\"\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_guavaOr_refactoring_fromNullableFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            " void f() {",
            "    Optional.of(\"test\").or(\"\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            " void f() {",
            "    Optional.fromNullable(\"test\").or(\"\");",
            "  }",
            "}")
        .setFixChooser(FixChoosers.FIRST)
        .doTest();
  }

  @Test
  public void positive_guavaOr_refactoring_simplifyExpressionFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            " String f() {",
            "    return Optional.of(\"test\").or(\"\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            " String f() {",
            "    return \"test\";",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void positive_guavaOrNull() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            " void f() {",
            "    // BUG: Diagnostic contains: orNull",
            "    Optional.of(\"test\").orNull();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_guavaOrNull_refactoring_fromNullableFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            " void f() {",
            "    Optional.of(\"test\").orNull();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            " void f() {",
            "    Optional.fromNullable(\"test\").orNull();",
            "  }",
            "}")
        .setFixChooser(FixChoosers.FIRST)
        .doTest();
  }

  @Test
  public void positive_guavaOrNull_refactoring_simplifyExpressionFix() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            " String f() {",
            "    return Optional.of(\"test\").orNull();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            " String f() {",
            "    return \"test\";",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void negative_ifPresent() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " void f(Optional<String> maybeString) {",
            "    maybeString.ifPresent(String::length);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_orElse() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            " void f(String value) {",
            "    Optional.of(value).filter(x -> x.length() < 5).orElse(\"test\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_guavaIsPresent() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "class Test {",
            " boolean f(Optional<String> maybeString) {",
            "    return maybeString.isPresent();",
            "  }",
            "}")
        .doTest();
  }
}
