/*
 * Copyright 2018 The Error Prone Authors.
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

/** Tests for {@link NoFunctionalReturnType}. */
@RunWith(JUnit4.class)
public class NoFunctionalReturnTypeTest {

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(NoFunctionalReturnType.class, getClass());

  @Test
  public void positiveFunctionalReturnTypePredicate() {
    helper
        .addSourceLines(
            "Test.java", //
            "import java.util.List;",
            "import java.util.function.Predicate;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  // BUG: Diagnostic contains: NoFunctionalReturnType",
            "  private Predicate<String> matchesAnyRegex(List<Pattern> regexes) {",
            "    return (toMatch) -> regexes.stream().anyMatch(p -> p.matcher(toMatch).matches());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveFunctionalReturnTypeDoublePredicate() {
    helper
        .addSourceLines(
            "Test.java", //
            "import java.util.function.DoublePredicate;",
            "class Test {",
            "  // BUG: Diagnostic contains: NoFunctionalReturnType",
            "  DoublePredicate test(boolean val) { ",
            "    return (value) -> !true;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveFunctionalReturnTypeFunction() {
    helper
        .addSourceLines(
            "Test.java", //
            "import java.util.function.Function;",
            "class Test {",
            "  // BUG: Diagnostic contains: NoFunctionalReturnType",
            "  Function<String, Boolean> test(String s) { ",
            "    return (value) -> true;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeFunctionalParam() {
    helper
        .addSourceLines(
            "Test.java", //
            "import java.util.List;",
            "import java.util.function.Predicate;",
            "import java.util.regex.Pattern;",
            "class Test {",
            "  private boolean matchesAnyRegex(Predicate<String> regexes) {",
            "     return true;",
            "  }",
            "}")
        .doTest();
  }
}
