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
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isEffectivelyPrivate;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.util.Collections;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Public types must have Javadoc comments.",
    severity = WARNING,
    linkType = CUSTOM,
    link = "https://google.github.io/styleguide/javaguide.html#s7.3-javadoc-where-required",
    documentSuppression = false)
public final class MissingJavadoc extends BugChecker implements ClassTreeMatcher {

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    return checkJavadoc(classTree, getSymbol(classTree), state);
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
