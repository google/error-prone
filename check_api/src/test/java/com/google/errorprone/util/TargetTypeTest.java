/*
 * Copyright 2025 The Error Prone Authors.
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

package com.google.errorprone.util;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.IdentifierTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import java.util.regex.Pattern;
import javax.lang.model.element.ElementKind;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class TargetTypeTest {
  /** A {@link BugChecker} that prints the target type of matched method invocations. */
  @BugPattern(severity = SeverityLevel.ERROR, summary = "Prints the target type")
  public static class TargetTypeChecker extends BugChecker
      implements MethodInvocationTreeMatcher, IdentifierTreeMatcher {
    private static final Matcher<ExpressionTree> METHOD_MATCHER =
        MethodMatchers.staticMethod().anyClass().withNameMatching(Pattern.compile("^detect.*"));

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      if (!METHOD_MATCHER.matches(tree, state)) {
        return Description.NO_MATCH;
      }
      TargetType targetType = TargetType.targetType(state);
      return buildDescription(tree)
          .setMessage(String.valueOf(targetType != null ? targetType.type() + "$" : null))
          .build();
    }

    @Override
    public Description matchIdentifier(IdentifierTree tree, VisitorState state) {
      Symbol symbol = ASTHelpers.getSymbol(tree);
      if (symbol == null
          || symbol.getKind() != ElementKind.LOCAL_VARIABLE
          || !tree.getName().toString().matches("detect.*")) {
        return Description.NO_MATCH;
      }
      TargetType targetType = TargetType.targetType(state);
      return buildDescription(tree)
          .setMessage(String.valueOf(targetType != null ? targetType.type() + "$" : null))
          .build();
    }
  }

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(TargetTypeChecker.class, getClass());

  @Test
  public void targetType() {
    helper
        .addSourceFile("testdata/TargetTypeTest.java")
        .setArgs(ImmutableList.of("-Xmaxerrs", "200", "-Xmaxwarns", "200"))
        .doTest();
  }
}
