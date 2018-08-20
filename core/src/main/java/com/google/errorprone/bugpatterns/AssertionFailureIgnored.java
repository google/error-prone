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

import static com.google.common.base.MoreObjects.firstNonNull;
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
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.predicates.TypePredicates;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.UnionClassType;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCTry;
import java.util.Objects;
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
    category = JDK)
public class AssertionFailureIgnored extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> ASSERTION =
      staticMethod()
          .onClassAny("org.junit.Assert", "junit.framework.Assert", "junit.framework.TestCase")
          .withNameMatching(Pattern.compile("fail|assert.*"));

  private static final Matcher<ExpressionTree> NEW_THROWABLE =
      MethodMatchers.constructor().forClass(TypePredicates.isDescendantOf("java.lang.Throwable"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!ASSERTION.matches(tree, state)) {
      return NO_MATCH;
    }
    JCTry tryStatement = enclosingTry(state);
    if (tryStatement == null) {
      return NO_MATCH;
    }
    Optional<JCCatch> maybeCatchTree =
        catchesType(tryStatement, state.getSymtab().assertionErrorType, state);
    if (!maybeCatchTree.isPresent()) {
      return NO_MATCH;
    }
    JCCatch catchTree = maybeCatchTree.get();
    VarSymbol parameter = ASTHelpers.getSymbol(catchTree.getParameter());
    boolean rethrows =
        firstNonNull(
            new TreeScanner<Boolean, Void>() {
              @Override
              public Boolean visitThrow(ThrowTree tree, Void unused) {
                if (Objects.equals(parameter, ASTHelpers.getSymbol(tree.getExpression()))) {
                  return true;
                }
                if (NEW_THROWABLE.matches(tree.getExpression(), state)
                    && ((NewClassTree) tree.getExpression())
                        .getArguments().stream()
                            .anyMatch(
                                arg -> Objects.equals(parameter, ASTHelpers.getSymbol(arg)))) {
                  return true;
                }
                return super.visitThrow(tree, null);
              }

              @Override
              public Boolean reduce(Boolean a, Boolean b) {
                return firstNonNull(a, false) || firstNonNull(b, false);
              }
            }.scan(catchTree.getBlock(), null),
            false);
    if (rethrows) {
      return NO_MATCH;
    }
    Description.Builder description = buildDescription(tree);
    buildFix(tryStatement, tree, state).ifPresent(description::addFix);
    return description.build();
  }

  // Provide a fix for one of the classic blunders:
  // rewrite `try { ..., fail(); } catch (AssertionError e) { ... }`
  // to `AssertionError e = assertThrows(AssertionError.class, () -> ...); ...`.
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
      fix.addStaticImport("org.junit.Assert.assertThrows")
          .prefixWith(tryStatement, state.getSourceForNode(catchTree.getParameter()))
          .replace(
              tryStatement.getStartPosition(),
              startPosition,
              String.format(
                  " = assertThrows(%s.class, () -> ",
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

  private static Optional<JCCatch> catchesType(
      JCTry tryStatement, Type assertionErrorType, VisitorState state) {
    return tryStatement.getCatches().stream()
        .filter(
            catchTree -> {
              Type type = ASTHelpers.getType(catchTree.getParameter());
              return (type.isUnion()
                      ? Streams.stream(((UnionClassType) type).getAlternativeTypes())
                      : Stream.of(type))
                  .anyMatch(caught -> isSubtype(assertionErrorType, caught, state));
            })
        .findFirst();
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
