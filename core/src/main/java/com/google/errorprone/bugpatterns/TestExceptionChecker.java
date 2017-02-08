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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.Category.JUNIT;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.collect.Lists;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.JUnitMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.Tag;
import java.util.List;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "TestExceptionChecker",
  category = JUNIT,
  summary =
      "Using @Test(expected=...) is discouraged, since the test will pass if *any* statement in"
          + " the test method throws the expected exception",
  severity = WARNING
)
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
    List<? extends StatementTree> statements = tree.getBody().getStatements();
    if (statements.size() == 1) {
      return NO_MATCH;
    }

    Description.Builder description = buildDescription(tree);
    List<SuggestedFix> fixes =
        Lists.reverse(statements)
            .stream()
            .filter(t -> !JUnitMatchers.containsTestMethod(t))
            .map(s -> buildFix(state, SuggestedFix.builder().merge(baseFix), expectedException, s))
            .collect(toImmutableList());
    if (!fixes.isEmpty()) {
      description.addAllFixes(fixes);
    } else {
      description.addFix(
          buildFix(
              state,
              SuggestedFix.builder().merge(baseFix),
              expectedException,
              getLast(statements)));
    }
    return description.build();
  }

  // TODO(cushon): extracting one statement into a lambda may not compile if the statement has
  // side effects (e.g. it references a variable in the method that isn't effectively final).
  // If this is a problem, consider trying to detect and avoid that case.
  private static SuggestedFix buildFix(
      VisitorState state,
      SuggestedFix.Builder fix,
      JCExpression expectedException,
      StatementTree statement) {
    fix.addStaticImport("org.junit.Assert.assertThrows");
    StringBuilder prefix = new StringBuilder();
    prefix.append(
        String.format("assertThrows(%s, () -> ", state.getSourceForNode(expectedException)));
    if (statement instanceof ExpressionStatementTree) {
      ExpressionTree expression = ((ExpressionStatementTree) statement).getExpression();
      fix.prefixWith(statement, prefix.toString());
      fix.postfixWith(expression, ")");
    } else {
      prefix.append(" {");
      fix.prefixWith(statement, prefix.toString());
      fix.postfixWith(statement, "});");
    }
    return fix.build();
  }

  /**
   * Searches the annotation list for {@code @Test(expected=...)}. If found, deletes the exception
   * attribute from the annotation, and returns its value.
   */
  private static JCExpression deleteExpectedException(
      SuggestedFix.Builder fix, List<JCAnnotation> annotations, VisitorState state) {
    Type testAnnotation = state.getTypeFromString(JUnitMatchers.JUNIT4_TEST_ANNOTATION);
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
            fix.replace(annotationTree, "@Test");
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
      fix.replace(
          ((JCTree) tree).getStartPosition(),
          ((JCTree) arguments.get(idx + 1)).getStartPosition(),
          "");
    }
  }
}
