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

package com.google.errorprone.bugpatterns.argumentselectiondefects;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.argumentselectiondefects.NamedParameterComment.ParsedParameterComment;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for NameInCommentHeuristic
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
@RunWith(JUnit4.class)
public class NameInCommentHeuristicTest {

  /** A {@link BugChecker} which runs the NameInCommentHeuristic and prints the result */
  @BugPattern(
      severity = SeverityLevel.ERROR,
      summary = "Runs NameInCommentHeuristic and prints the result")
  public static class NameInCommentHeuristicChecker extends BugChecker
      implements MethodInvocationTreeMatcher {

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      ImmutableList<Parameter> formal =
          Parameter.createListFromVarSymbols(ASTHelpers.getSymbol(tree).getParameters());
      Stream<Parameter> actual =
          Parameter.createListFromExpressionTrees(tree.getArguments()).stream();

      Changes changes =
          Changes.create(
              formal.stream().map(f -> 1.0).collect(toImmutableList()),
              formal.stream().map(f -> 0.0).collect(toImmutableList()),
              Streams.zip(formal.stream(), actual, ParameterPair::create)
                  .collect(toImmutableList()));

      boolean result =
          !new NameInCommentHeuristic()
              .isAcceptableChange(changes, tree, ASTHelpers.getSymbol(tree), state);
      return buildDescription(tree).setMessage(String.valueOf(result)).build();
    }
  }

  @Test
  public void nameInCommentHeuristic_returnsTrue_whereCommentMatchesFormalParameter() {
    CompilationTestHelper.newInstance(NameInCommentHeuristicChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            abstract class Test {
              abstract void target(Object first);

              void test(Object first) {
                // BUG: Diagnostic contains: true
                target(first /* first */);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void
      nameInCommentHeuristic_returnsTrue_wherePreceedingCommentWithEqualsMatchesFormalParameter() {
    CompilationTestHelper.newInstance(NameInCommentHeuristicChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            abstract class Test {
              abstract void target(Object first);

              void test(Object first) {
                // BUG: Diagnostic contains: true
                target(/* first= */ first);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void
      nameInCommentHeuristic_returnsTrue_wherePreceedingCommentHasEqualsSpacesAndExtraText() {
    CompilationTestHelper.newInstance(NameInCommentHeuristicChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            abstract class Test {
              abstract void target(Object first);

              void test(Object first) {
                // BUG: Diagnostic contains: true
                target(/*note first = */ first);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nameInCommentHeuristic_returnsFalse_withNoComments() {
    CompilationTestHelper.newInstance(NameInCommentHeuristicChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            abstract class Test {
              abstract void target(Object first);

              void test(Object first) {
                // BUG: Diagnostic contains: false
                target(first);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nameInCommentHeuristic_returnsFalse_withCommentNotMatchingFormalParameter() {
    CompilationTestHelper.newInstance(NameInCommentHeuristicChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            abstract class Test {
              abstract void target(Object first);

              void test(Object first) {
                // BUG: Diagnostic contains: false
                target(first /* other */);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nameInCommentHeuristic_returnsTrue_whereLineCommentMatchesFormalParameter() {
    CompilationTestHelper.newInstance(NameInCommentHeuristicChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            abstract class Test {
              abstract void target(Object first, Object second);

              void test(Object first, Object second) {
                // BUG: Diagnostic contains: true
                target(
                    first, // first
                    second);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void parseParameterComment_validInputs() {
    assertThat(NamedParameterComment.parseParameterComment("foo="))
        .hasValue(new ParsedParameterComment("foo", false));
    assertThat(NamedParameterComment.parseParameterComment(" foo= "))
        .hasValue(new ParsedParameterComment("foo", false));
    assertThat(
            NamedParameterComment.parseParameterComment(
                " \t\n\u000B\f\rfoo \t\n\u000B\f\r= \t\n\u000B\f\r"))
        .hasValue(new ParsedParameterComment("foo", false));
    assertThat(NamedParameterComment.parseParameterComment(" $foo_1$ = "))
        .hasValue(new ParsedParameterComment("$foo_1$", false));
    assertThat(NamedParameterComment.parseParameterComment(" λ_α1 = "))
        .hasValue(new ParsedParameterComment("λ_α1", false));
    assertThat(NamedParameterComment.parseParameterComment(" \uD835\uDCB0\uD835\uDFCE = "))
        .hasValue(new ParsedParameterComment("\uD835\uDCB0\uD835\uDFCE", false));
    assertThat(NamedParameterComment.parseParameterComment(" int = "))
        .hasValue(new ParsedParameterComment("int", false));
    assertThat(NamedParameterComment.parseParameterComment(" default = "))
        .hasValue(new ParsedParameterComment("default", false));
    assertThat(NamedParameterComment.parseParameterComment(" args...= "))
        .hasValue(new ParsedParameterComment("args", true));
    assertThat(NamedParameterComment.parseParameterComment("args...\t = \n"))
        .hasValue(new ParsedParameterComment("args", true));
  }

  @Test
  public void parseParameterComment_invalidInputs() {
    assertThat(NamedParameterComment.parseParameterComment("")).isEmpty();
    assertThat(NamedParameterComment.parseParameterComment("   ")).isEmpty();
    assertThat(NamedParameterComment.parseParameterComment("=")).isEmpty();
    assertThat(NamedParameterComment.parseParameterComment("...=")).isEmpty();
    assertThat(NamedParameterComment.parseParameterComment("foo")).isEmpty();
    assertThat(NamedParameterComment.parseParameterComment("1foo=")).isEmpty();
    assertThat(NamedParameterComment.parseParameterComment("\uD835\uDFCE=")).isEmpty();
    assertThat(NamedParameterComment.parseParameterComment("a.b=")).isEmpty();
    assertThat(NamedParameterComment.parseParameterComment("a..b=")).isEmpty();
    assertThat(NamedParameterComment.parseParameterComment(".a=")).isEmpty();
    assertThat(NamedParameterComment.parseParameterComment("a.=")).isEmpty();
    assertThat(NamedParameterComment.parseParameterComment("args..=")).isEmpty();
    assertThat(NamedParameterComment.parseParameterComment("args....=")).isEmpty();
    assertThat(NamedParameterComment.parseParameterComment("args ...=")).isEmpty();
    assertThat(NamedParameterComment.parseParameterComment("foo==")).isEmpty();
    assertThat(NamedParameterComment.parseParameterComment("foo=bar")).isEmpty();
    // Non-ASCII whitespace (e.g. U+00A0 NO-BREAK SPACE and U+2000 EN QUAD, which is
    // Character.isWhitespace) is not matched by regex \s.
    assertThat(NamedParameterComment.parseParameterComment("\u00A0foo=")).isEmpty();
    assertThat(NamedParameterComment.parseParameterComment("\u2000foo=")).isEmpty();
    assertThat(NamedParameterComment.parseParameterComment("foo\u2000=")).isEmpty();
    assertThat(NamedParameterComment.parseParameterComment("foo=\u2000")).isEmpty();
  }
}
