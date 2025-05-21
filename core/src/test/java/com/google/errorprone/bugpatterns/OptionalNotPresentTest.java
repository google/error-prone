/*
 * Copyright 2017 The Error Prone Authors.
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

/**
 * @author mariasam@google.com (Maria Sam)
 */
@RunWith(JUnit4.class)
public class OptionalNotPresentTest {

  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(OptionalNotPresent.class, getClass());

  @Test
  public void negativeCases() {
    compilationTestHelper
        .addSourceLines(
            "OptionalNotPresentNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import java.util.Optional;
            import java.util.function.Predicate;

            /** Includes true-negative cases and false-positive cases. */
            public class OptionalNotPresentNegativeCases {

              // Test this doesn't trigger NullPointerException
              private final Predicate<Optional<?>> asField = o -> !o.isPresent();

              // False-positive
              public String getWhenTestedSafe_referenceEquality(Optional<String> optional) {
                if (!optional.isPresent()) {
                  if (optional == Optional.of("OK")) { // always false
                    // BUG: Diagnostic contains:
                    return optional.get();
                  }
                }
                return "";
              }

              // False-positive
              public String getWhenTestedSafe_equals(Optional<String> optional) {
                if (!optional.isPresent()) {
                  if (optional.equals(Optional.of("OK"))) { // always false
                    // BUG: Diagnostic contains:
                    return optional.get();
                  }
                }
                return "";
              }

              public String getWhenPresent_blockReassigned(Optional<String> optional) {
                if (!optional.isPresent()) {
                  optional = Optional.of("value");
                  return optional.get();
                }
                return "";
              }

              public String getWhenPresent_localReassigned(Optional<String> optional) {
                if (!optional.isPresent()) {
                  optional = Optional.of("value");
                }
                return optional.get();
              }

              public String getWhenPresent_nestedCheck(Optional<String> optional) {
                if (!optional.isPresent() || true) {
                  return optional.isPresent() ? optional.get() : "";
                }
                return "";
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void positiveCases() {
    compilationTestHelper
        .addSourceLines(
            "OptionalNotPresentPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import java.util.Optional;

            /** Includes true-positive and false-negative cases. */
            public class OptionalNotPresentPositiveCases {

              // False-negative
              public String getWhenUnknown(Optional<String> optional) {
                return optional.get();
              }

              // False-negative
              public String getWhenUnknown_testNull(Optional<String> optional) {
                if (optional.get() != null) {
                  return optional.get();
                }
                return "";
              }

              // False-negative
              public String getWhenAbsent_testAndNestUnrelated(Optional<String> optional) {
                if (true) {
                  String str = optional.get();
                  if (!optional.isPresent()) {
                    return "";
                  }
                  return str;
                }
                return "";
              }

              public String getWhenAbsent(Optional<String> testStr) {
                if (!testStr.isPresent()) {
                  // BUG: Diagnostic contains:
                  return testStr.get();
                }
                return "";
              }

              public String getWhenAbsent_multipleStatements(Optional<String> optional) {
                if (!optional.isPresent()) {
                  String test = "test";
                  // BUG: Diagnostic contains:
                  return test + optional.get();
                }
                return "";
              }

              public String getWhenAbsent_nestedCheck(Optional<String> optional) {
                if (!optional.isPresent() || true) {
                  // BUG: Diagnostic contains:
                  return !optional.isPresent() ? optional.get() : "";
                }
                return "";
              }

              public String getWhenAbsent_compoundIf_false(Optional<String> optional) {
                if (!optional.isPresent() && true) {
                  // BUG: Diagnostic contains:
                  return optional.get();
                }
                return "";
              }

              // False-negative
              public String getWhenAbsent_compoundIf_true(Optional<String> optional) {
                if (!optional.isPresent() || true) {
                  return optional.get();
                }
                return "";
              }

              public String getWhenAbsent_elseClause(Optional<String> optional) {
                if (optional.isPresent()) {
                  return optional.get();
                } else {
                  // BUG: Diagnostic contains:
                  return optional.get();
                }
              }

              // False-negative
              public String getWhenAbsent_localReassigned(Optional<String> optional) {
                if (!optional.isPresent()) {
                  optional = Optional.empty();
                }
                return optional.get();
              }

              // False-negative
              public String getWhenAbsent_methodScoped(Optional<String> optional) {
                if (optional.isPresent()) {
                  return "";
                }
                return optional.get();
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void b80065837() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Optional;
            import java.util.Map;

            class Test {
              <T> Optional<T> f(T t) {
                return Optional.ofNullable(t);
              }

              int g(Map<String, Optional<Integer>> m) {
                if (!m.get("one").isPresent()) {
                  return m.get("two").get();
                }
                return -1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negation_butNotNegatingOptionalCheck() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Optional;

            class Test {
              int g(Optional<Integer> o) {
                if (!equals(this) && o.isPresent()) {
                  return o.orElseThrow();
                }
                return -1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void isEmpty() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Optional;

            class Test {
              int g(Optional<Integer> o) {
                if (o.isEmpty()) {
                  // BUG: Diagnostic contains:
                  return o.get();
                }
                return -1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void orElseThrow() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Optional;

            class Test {
              int g(Optional<Integer> o) {
                if (o.isEmpty()) {
                  // BUG: Diagnostic contains:
                  return o.orElseThrow();
                }
                return -1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void ternary_good() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Optional;

            class Test {
              int g(Optional<Integer> o) {
                return o.isEmpty() ? 0 : o.get();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void ternary_bad() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Optional;

            class Test {
              int g(Optional<Integer> o) {
                // BUG: Diagnostic contains:
                return o.isEmpty() ? o.get() : 0;
              }
            }
            """)
        .doTest();
  }
}
