/*
 * Copyright 2026 The Error Prone Authors.
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.matchers.RegexMatchers.PATTERN_COMPILE_METHODS;
import static com.google.errorprone.matchers.RegexMatchers.PATTERN_MATCHER_METHODS;
import static com.google.errorprone.matchers.RegexMatchers.PATTERN_MATCHES_METHODS;
import static com.google.errorprone.matchers.RegexMatchers.PATTERN_QUOTE_METHODS;
import static com.google.errorprone.matchers.RegexMatchers.PREDICATES_CONTAINS_PATTERN_METHOD;
import static com.google.errorprone.matchers.RegexMatchers.SPLITTER_ON_PATTERN_METHOD;
import static com.google.errorprone.matchers.RegexMatchers.STRING_MATCHES_METHOD;
import static com.google.errorprone.matchers.RegexMatchers.STRING_REPLACE_METHODS;
import static com.google.errorprone.matchers.RegexMatchers.STRING_SPLIT_METHODS;
import static com.google.errorprone.matchers.RegexMatchers.isStringSplitFastPath;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.sun.source.tree.MethodInvocationTree;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Tests for {@link RegexMatchers}. */
public final class RegexMatchersTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(TestRegexMethodChecker.class, getClass());

  @Test
  public void methodMatchers() {
    compilationHelper
        .addSourceLines(
            "com/google/re2j/Matcher.java",
            """
            package com.google.re2j;

            public final class Matcher {}
            """)
        .addSourceLines(
            "com/google/re2j/Pattern.java",
            """
            package com.google.re2j;

            public final class Pattern {
              public static Pattern compile(String regex) {
                return new Pattern();
              }

              public static boolean matches(String regex, CharSequence input) {
                return false;
              }

              public static String quote(String s) {
                return s;
              }

              public Matcher matcher(CharSequence input) {
                return new Matcher();
              }
            }
            """)
        .addSourceLines(
            "TestRegex.java",
            """
            import com.google.common.base.Predicates;
            import com.google.common.base.Splitter;
            import java.util.regex.Pattern;

            class TestRegex {
              void test(String s) {
                // BUG: Diagnostic contains: [COMPILE]
                Pattern p1 = Pattern.compile("a+");
                // BUG: Diagnostic contains: [MATCHER]
                var m1 = p1.matcher(s);
                // BUG: Diagnostic contains: [PATTERN_MATCHES]
                boolean b1 = Pattern.matches("a+", s);
                // BUG: Diagnostic contains: [QUOTE]
                String q1 = Pattern.quote(s);

                // BUG: Diagnostic contains: [COMPILE]
                var p2 = com.google.re2j.Pattern.compile("a+");
                // BUG: Diagnostic contains: [MATCHER]
                var m2 = p2.matcher(s);

                // BUG: Diagnostic contains: [COMPILE]
                var p3 = org.safere.Pattern.compile("a+");
                // BUG: Diagnostic contains: [MATCHER]
                var m3 = p3.matcher(s);

                // BUG: Diagnostic contains: [STRING_MATCHES]
                boolean b2 = s.matches("a+");
                // BUG: Diagnostic contains: [STRING_REPLACE]
                String r1 = s.replaceFirst("a+", "b");
                // BUG: Diagnostic contains: [STRING_REPLACE]
                String r2 = s.replaceAll("a+", "b");
                // BUG: Diagnostic contains: [STRING_SPLIT]
                String[] sp1 = s.split("a+");
                // BUG: Diagnostic contains: [STRING_SPLIT]
                String[] sp2 = s.splitWithDelimiters("a+", -1);
                // BUG: Diagnostic contains: [SPLITTER]
                var sp3 = Splitter.onPattern("a+");
                // BUG: Diagnostic contains: [PREDICATES]
                var pred = Predicates.containsPattern("a+");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void stringSplitFastPath() {
    assertThat(isStringSplitFastPath(",")).isTrue();
    assertThat(isStringSplitFastPath(":")).isTrue();
    assertThat(isStringSplitFastPath("\\.")).isTrue();
    assertThat(isStringSplitFastPath("\\|")).isTrue();
    assertThat(isStringSplitFastPath(".")).isFalse();
    assertThat(isStringSplitFastPath("|")).isFalse();
    assertThat(isStringSplitFastPath("\\0")).isFalse();
    assertThat(isStringSplitFastPath("\\9")).isFalse();
    assertThat(isStringSplitFastPath("\\d")).isFalse();
    assertThat(isStringSplitFastPath("\\w")).isFalse();
    assertThat(isStringSplitFastPath("\\D")).isFalse();
    assertThat(isStringSplitFastPath("\\W")).isFalse();
    assertThat(isStringSplitFastPath("\\A")).isFalse();
    assertThat(isStringSplitFastPath("\\Z")).isFalse();
    assertThat(isStringSplitFastPath(String.valueOf(Character.MIN_HIGH_SURROGATE))).isFalse();
    assertThat(isStringSplitFastPath(String.valueOf(Character.MAX_HIGH_SURROGATE))).isFalse();
    assertThat(isStringSplitFastPath(String.valueOf(Character.MIN_LOW_SURROGATE))).isFalse();
    assertThat(isStringSplitFastPath(String.valueOf(Character.MAX_LOW_SURROGATE))).isFalse();
    assertThat(isStringSplitFastPath("\\" + Character.MIN_HIGH_SURROGATE)).isFalse();
    assertThat(isStringSplitFastPath("\\" + Character.MAX_HIGH_SURROGATE)).isFalse();
    assertThat(isStringSplitFastPath("\\" + Character.MIN_LOW_SURROGATE)).isFalse();
    assertThat(isStringSplitFastPath("\\" + Character.MAX_LOW_SURROGATE)).isFalse();
    assertThat(isStringSplitFastPath("")).isFalse();
    assertThat(isStringSplitFastPath(",,")).isFalse();
  }

  /** BugChecker for testing {@link RegexMatchers}. */
  @BugPattern(summary = "Reports matched regex methods", severity = SeverityLevel.WARNING)
  public static final class TestRegexMethodChecker extends BugChecker
      implements MethodInvocationTreeMatcher {
    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      List<String> matched = new ArrayList<>();
      if (PATTERN_COMPILE_METHODS.matches(tree, state)) {
        matched.add("COMPILE");
      }
      if (PATTERN_MATCHES_METHODS.matches(tree, state)) {
        matched.add("PATTERN_MATCHES");
      }
      if (PATTERN_QUOTE_METHODS.matches(tree, state)) {
        matched.add("QUOTE");
      }
      if (PATTERN_MATCHER_METHODS.matches(tree, state)) {
        matched.add("MATCHER");
      }
      if (STRING_MATCHES_METHOD.matches(tree, state)) {
        matched.add("STRING_MATCHES");
      }
      if (STRING_REPLACE_METHODS.matches(tree, state)) {
        matched.add("STRING_REPLACE");
      }
      if (STRING_SPLIT_METHODS.matches(tree, state)) {
        matched.add("STRING_SPLIT");
      }
      if (SPLITTER_ON_PATTERN_METHOD.matches(tree, state)) {
        matched.add("SPLITTER");
      }
      if (PREDICATES_CONTAINS_PATTERN_METHOD.matches(tree, state)) {
        matched.add("PREDICATES");
      }
      if (matched.isEmpty()) {
        return Description.NO_MATCH;
      }
      return buildDescription(tree).setMessage(matched.toString()).build();
    }
  }
}
