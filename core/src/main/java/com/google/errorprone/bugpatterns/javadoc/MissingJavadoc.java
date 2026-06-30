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

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.findSuperMethods;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isEffectivelyPrivate;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Public types and public/protected members must have Javadoc comments.",
    severity = WARNING,
    linkType = CUSTOM,
    link = "https://google.github.io/styleguide/javaguide.html#s7.3-javadoc-where-required",
    documentSuppression = false)
public final class MissingJavadoc extends BugChecker
    implements ClassTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    return checkJavadoc(classTree, getSymbol(classTree), state);
  }

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    MethodSymbol symbol = getSymbol(methodTree);
    if (symbol == null) {
      return NO_MATCH;
    }
    if (symbol.isConstructor()) {
      // Constructors have a better than average change of being "self-explanatory":
      // https://google.github.io/styleguide/javaguide.html#s7.3.1-javadoc-exception-self-explanatory
      return NO_MATCH;
    }
    if (!findSuperMethods(symbol, state.getTypes()).isEmpty()) {
      return NO_MATCH;
    }
    if (isSimpleGetterOrSetter(methodTree)) {
      return NO_MATCH;
    }
    return checkJavadoc(methodTree, symbol, state);
  }

  @Override
  public Description matchVariable(VariableTree variableTree, VisitorState state) {
    Symbol symbol = getSymbol(variableTree);
    if (symbol == null || symbol.getKind() != ElementKind.FIELD) {
      return NO_MATCH;
    }
    return checkJavadoc(variableTree, symbol, state);
  }

  private Description checkJavadoc(Tree tree, Symbol symbol, VisitorState state) {
    if (state.errorProneOptions().isTestOnlyTarget()) {
      return NO_MATCH;
    }
    if (isEffectivelyPrivate(symbol)) {
      return NO_MATCH;
    }
    if (!isEffectivelyPublicOrProtected(symbol)) {
      return NO_MATCH;
    }
    DocCommentTree docCommentTree =
        JavacTrees.instance(state.context).getDocCommentTree(state.getPath());
    if (docCommentTree != null) {
      return NO_MATCH;
    }
    Description.Builder description = buildDescription(tree);
    if (tree instanceof ClassTree classTree
        && classTree.getSimpleName().toString().endsWith("Builder")) {
      ClassSymbol enclosing = enclosingClass(symbol);
      if (enclosing != null) {
        String suggestedJavadoc =
            String.format("/** A builder for {@link %s}. */\n", enclosing.getSimpleName());
        description
            .setMessage(
                "Builder classes require Javadoc comments. Consider adding: %s",
                suggestedJavadoc.trim())
            .addFix(SuggestedFix.prefixWith(tree, suggestedJavadoc));
      }
    }
    return description.build();
  }

  private static boolean isSimpleGetterOrSetter(MethodTree methodTree) {
    if (methodTree.getBody() == null) {
      return false;
    }
    List<? extends StatementTree> statements = methodTree.getBody().getStatements();
    if (statements.size() != 1) {
      return false;
    }
    StatementTree stmt = statements.get(0);
    return switch (stmt) {
      case ReturnTree returnTree -> true;
      case ExpressionStatementTree expressionStatementTree ->
          expressionStatementTree.getExpression() instanceof AssignmentTree;
      default -> false;
    };
  }

  private static final ImmutableSet<Modifier> PUBLIC_OR_PROTECTED =
      ImmutableSet.of(Modifier.PUBLIC, Modifier.PROTECTED);

  private static boolean isEffectivelyPublicOrProtected(Symbol symbol) {
    Symbol current = symbol;
    while (current != null) {
      if (current.getKind() == ElementKind.PACKAGE) {
        break;
      }
      if (Collections.disjoint(current.getModifiers(), PUBLIC_OR_PROTECTED)) {
        return false;
      }
      current = current.owner;
    }
    return true;
  }
}
