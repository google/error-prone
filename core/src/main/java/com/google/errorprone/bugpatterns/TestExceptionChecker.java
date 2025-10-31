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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.JUnitMatchers;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.Tag;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Using @Test(expected=...) is discouraged, since the test will pass if *any* statement in"
            + " the test method throws the expected exception",
    severity = ERROR)
public class TestExceptionChecker extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (tree.getBody() == null) {
      return NO_MATCH;
    }
    SuggestedFix.Builder baseFixBuilder = SuggestedFix.builder();
    JCExpression expectedException =
        deleteExpectedException(
            baseFixBuilder, ((JCMethodDecl) tree).getModifiers().getAnnotations(), state);
    SuggestedFix baseFix = baseFixBuilder.build();
    if (expectedException == null) {
      return NO_MATCH;
    }
    return handleStatements(tree, state, expectedException, baseFix);
  }

  /**
   * Handle a method annotated with {@code @Test(expected=...}.
   *
   * @param tree the method
   * @param state the visitor state
   * @param expectedException the type of expected exception
   * @param baseFix the base fix
   */
  private Description handleStatements(
      MethodTree tree, VisitorState state, JCExpression expectedException, SuggestedFix baseFix) {
    return describeMatch(
        tree,
        buildFix(state, baseFix.toBuilder(), expectedException, tree.getBody().getStatements()));
  }

  private static SuggestedFix buildFix(
      VisitorState state,
      SuggestedFix.Builder fix,
      JCExpression expectedException,
      Collection<? extends StatementTree> statements) {
    if (statements.isEmpty()) {
      return fix.build();
    }
    fix.addStaticImport("org.junit.Assert.assertThrows");
    StringBuilder prefix = new StringBuilder();
    prefix.append(
        String.format("assertThrows(%s, () -> ", state.getSourceForNode(expectedException)));
    StatementTree last = getLast(statements);
    if (last instanceof ExpressionStatementTree expressionStatementTree) {
      ExpressionTree expression = expressionStatementTree.getExpression();
      fix.prefixWith(expression, prefix.toString());
      fix.postfixWith(expression, ")");
    } else {
      prefix.append(" {");
      fix.prefixWith(last, prefix.toString());
      fix.postfixWith(last, "});");
    }
    return fix.build();
  }

  /**
   * Searches the annotation list for {@code @Test(expected=...)}. If found, deletes the exception
   * attribute from the annotation, and returns its value.
   */
  private static @Nullable JCExpression deleteExpectedException(
      SuggestedFix.Builder fix, List<JCAnnotation> annotations, VisitorState state) {
    Type testAnnotation = ORG_JUNIT_TEST.get(state);
    for (JCAnnotation annotationTree : annotations) {
      if (!ASTHelpers.isSameType(testAnnotation, annotationTree.type, state)) {
        continue;
      }
      com.sun.tools.javac.util.List<JCExpression> arguments = annotationTree.getArguments();
      for (JCExpression arg : arguments) {
        if (!arg.hasTag(Tag.ASSIGN)) {
          continue;
        }
        JCAssign assign = (JCAssign) arg;
        if (assign.lhs.hasTag(Tag.IDENT)
            && ((JCIdent) assign.lhs).getName().contentEquals("expected")) {
          if (arguments.size() == 1) {
            fix.replace(
                annotationTree,
                "@" + qualifyType(state, fix, JUnitMatchers.JUNIT4_TEST_ANNOTATION));
          } else {
            removeFromList(fix, state, arguments, assign);
          }
          return assign.rhs;
        }
      }
    }
    return null;
  }

  /** Deletes an entry and its delimiter from a list. */
  private static void removeFromList(
      SuggestedFix.Builder fix, VisitorState state, List<? extends Tree> arguments, Tree tree) {
    int idx = arguments.indexOf(tree);
    if (idx == arguments.size() - 1) {
      fix.replace(
          state.getEndPosition(arguments.get(arguments.size() - 1)),
          state.getEndPosition(tree),
          "");
    } else {
      fix.replace(getStartPosition(tree), getStartPosition(arguments.get(idx + 1)), "");
    }
  }

  private static final Supplier<Type> ORG_JUNIT_TEST =
      VisitorState.memoize(state -> state.getTypeFromString(JUnitMatchers.JUNIT4_TEST_ANNOTATION));
}
