/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.LIKELY_ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.expressionStatement;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.UnionClassType;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCTry;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "AssertionFailureIgnored",
  summary =
      "This assertion throws an AssertionError if it fails, which will be caught by an enclosing"
          + " try block.",
  // TODO(cushon): promote this to an error and turn down TryFailThrowable
  severity = WARNING,
  tags = LIKELY_ERROR,
  category = JDK
)
public class AssertionFailureIgnored extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> ASSERTION =
      staticMethod()
          .onClassAny("org.junit.Assert", "junit.framework.Assert", "junit.framework.TestCase")
          .withNameMatching(Pattern.compile("fail|assert.*"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!ASSERTION.matches(tree, state)) {
      return NO_MATCH;
    }
    JCTry tryStatement = enclosingTry(state);
    if (tryStatement == null) {
      return NO_MATCH;
    }
    if (!catchesType(tryStatement, state.getSymtab().assertionErrorType, state)) {
      return NO_MATCH;
    }
    Description.Builder description = buildDescription(tree);
    buildFix(tryStatement, tree, state).ifPresent(description::addFix);
    return description.build();
  }

  // Provide a fix for one of the classic blunders:
  // rewrite `try { ..., fail(); } catch (AssertionError e) { ... }`
  // to `AssertionError e = expectThrows(AssertionError.class, () -> ...); ...`.
  private static Optional<Fix> buildFix(
      JCTry tryStatement, MethodInvocationTree tree, VisitorState state) {
    if (!ASTHelpers.getSymbol(tree).getSimpleName().contentEquals("fail")) {
      // ignore non-failure asserts
      return Optional.empty();
    }
    JCBlock block = tryStatement.getBlock();
    if (!expressionStatement((t, s) -> t.equals(tree))
        .matches(getLast(block.getStatements()), state)) {
      // the `fail()` should be the last expression statement in the try block
      return Optional.empty();
    }
    if (tryStatement.getCatches().size() != 1) {
      // the fix is less clear for multiple catch clauses
      return Optional.empty();
    }
    JCCatch catchTree = Iterables.getOnlyElement(tryStatement.getCatches());
    if (catchTree.getParameter().getType().getKind() == Kind.UNION_TYPE) {
      // variables can't have union types
      return Optional.empty();
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    boolean expression =
        block.getStatements().size() == 2
            && block.getStatements().get(0).getKind() == Kind.EXPRESSION_STATEMENT;
    int startPosition;
    int endPosition;
    if (expression) {
      JCExpressionStatement expressionTree = (JCExpressionStatement) block.getStatements().get(0);
      startPosition = expressionTree.getStartPosition();
      endPosition = state.getEndPosition(expressionTree.getExpression());
    } else {
      startPosition = block.getStartPosition();
      endPosition = getLast(tryStatement.getBlock().getStatements()).getStartPosition();
    }
    if (catchTree.getBlock().getStatements().isEmpty()) {
      fix.addStaticImport("org.junit.Assert.assertThrows");
      fix.replace(
              tryStatement.getStartPosition(),
              startPosition,
              String.format(
                  "assertThrows(%s.class, () -> ",
                  state.getSourceForNode(catchTree.getParameter().getType())))
          .replace(endPosition, state.getEndPosition(catchTree), (expression ? "" : "}") + ");\n");
    } else {
      fix.addStaticImport("org.junit.Assert.expectThrows")
          .prefixWith(tryStatement, state.getSourceForNode(catchTree.getParameter()))
          .replace(
              tryStatement.getStartPosition(),
              startPosition,
              String.format(
                  " = expectThrows(%s.class, () -> ",
                  state.getSourceForNode(catchTree.getParameter().getType())))
          .replace(
              /* startPos= */ endPosition,
              /* endPos= */ catchTree.getBlock().getStatements().get(0).getStartPosition(),
              (expression ? "" : "}") + ");\n")
          .replace(
              state.getEndPosition(getLast(catchTree.getBlock().getStatements())),
              state.getEndPosition(catchTree),
              "");
    }
    return Optional.of(fix.build());
  }

  private static boolean catchesType(
      JCTry tryStatement, Type assertionErrorType, VisitorState state) {
    return tryStatement
        .getCatches()
        .stream()
        .map(catchTree -> ASTHelpers.getType(catchTree.getParameter()))
        .flatMap(
            type ->
                type.isUnion()
                    ? Streams.stream(((UnionClassType) type).getAlternativeTypes())
                    : Stream.of(type))
        .anyMatch(caught -> isSubtype(assertionErrorType, caught, state));
  }

  private static JCTry enclosingTry(VisitorState state) {
    Tree prev = null;
    for (Tree parent : state.getPath()) {
      switch (parent.getKind()) {
        case METHOD:
        case LAMBDA_EXPRESSION:
          return null;
        case TRY:
          JCTry tryStatement = (JCTry) parent;
          return tryStatement.getBlock().equals(prev) ? tryStatement : null;
        default: // fall out
      }
      prev = parent;
    }
    return null;
  }
}
