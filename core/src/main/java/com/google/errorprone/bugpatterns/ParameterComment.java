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

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Streams.forEachPair;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Commented;
import com.google.errorprone.util.Comments;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.parser.Tokens.Comment;
import java.util.stream.Stream;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "ParameterComment",
    category = JDK,
    summary = "Non-standard parameter comment; prefer `/* paramName= */ arg`",
    severity = SUGGESTION,
    tags = StandardTags.STYLE,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class ParameterComment extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return matchNewClassOrMethodInvocation(
        ASTHelpers.getSymbol(tree), Comments.findCommentsForArguments(tree, state), tree);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    return matchNewClassOrMethodInvocation(
        ASTHelpers.getSymbol(tree), Comments.findCommentsForArguments(tree, state), tree);
  }

  private Description matchNewClassOrMethodInvocation(
      MethodSymbol symbol, ImmutableList<Commented<ExpressionTree>> arguments, Tree tree) {
    if (symbol.getParameters().isEmpty()) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    forEachPair(
        arguments.stream(),
        Stream.concat(
            symbol.getParameters().stream(),
            Stream.iterate(getLast(symbol.getParameters()), x -> x)),
        (commented, param) -> {
          ImmutableList<Comment> comments =
              commented.afterComments().isEmpty()
                  ? commented.beforeComments()
                  : commented.afterComments();
          boolean matchStandardForm = !commented.afterComments().isEmpty();
          comments.stream()
              .filter(c -> matchingParamComment(c, param, matchStandardForm))
              .findFirst()
              .ifPresent(c -> fixParamComment(fix, commented, param, c));
        });
    return fix.isEmpty() ? NO_MATCH : describeMatch(tree, fix.build());
  }

  private static boolean matchingParamComment(
      Comment c, VarSymbol param, boolean matchStandardForm) {
    String text = Comments.getTextFromComment(c).trim();
    if (text.endsWith("=")) {
      if (!matchStandardForm) {
        return false;
      }
      text = text.substring(0, text.length() - "=".length()).trim();
    }
    return param.getSimpleName().contentEquals(text);
  }

  private static void fixParamComment(
      SuggestedFix.Builder fix, Commented<ExpressionTree> commented, VarSymbol param, Comment c) {
    fix.prefixWith(commented.tree(), String.format("/* %s= */ ", param.getSimpleName()))
        .replace(c.getSourcePos(0), c.getSourcePos(0) + c.getText().length(), "");
  }
}
