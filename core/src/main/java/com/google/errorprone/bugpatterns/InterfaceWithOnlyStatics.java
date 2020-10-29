/*
 * Copyright 2018 The Error Prone Authors.
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
import static com.google.errorprone.bugpatterns.inject.dagger.DaggerAnnotations.isAnyModule;
import static com.google.errorprone.util.ASTHelpers.createPrivateConstructor;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ErrorProneToken;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import java.util.List;
import javax.lang.model.element.Modifier;

/**
 * Bugpattern to detect interfaces used only to store static fields/methods.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "InterfaceWithOnlyStatics",
    summary =
        "This interface only contains static fields and methods; consider making it a final class "
            + "instead to prevent subclassing.",
    severity = SeverityLevel.WARNING)
public final class InterfaceWithOnlyStatics extends BugChecker implements ClassTreeMatcher {
  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (!tree.getImplementsClause().isEmpty()) {
      return Description.NO_MATCH;
    }
    if (isAnyModule().matches(tree, state)) {
      return Description.NO_MATCH;
    }
    List<? extends Tree> members = tree.getMembers();
    ClassSymbol symbol = getSymbol(tree);
    if (symbol == null || !symbol.isInterface() || symbol.isAnnotationType()) {
      return Description.NO_MATCH;
    }
    int staticMembers = 0;
    int nonStaticMembers = 0;
    for (Tree member : members) {
      Symbol memberSymbol = getSymbol(member);
      if (memberSymbol == null) {
        return Description.NO_MATCH;
      }
      if (memberSymbol.isStatic()) {
        staticMembers++;
      } else {
        nonStaticMembers++;
      }
    }
    if (nonStaticMembers > 0 || staticMembers == 0) {
      return Description.NO_MATCH;
    }
    SuggestedFix.Builder suggestedFix = SuggestedFix.builder();
    for (Tree member : members) {
      if (member instanceof VariableTree) {
        VariableTree variableTree = (VariableTree) member;
        SuggestedFixes.addModifiers(
                variableTree, state, Modifier.FINAL, Modifier.STATIC, Modifier.PUBLIC)
            .ifPresent(suggestedFix::merge);
      }
      if (member instanceof MethodTree) {
        MethodTree methodTree = (MethodTree) member;
        SuggestedFixes.addModifiers(methodTree, state, Modifier.PUBLIC)
            .ifPresent(suggestedFix::merge);
      }
    }
    suggestedFix
        .merge(fixClass(tree, state))
        .postfixWith(getLast(members), "\n" + createPrivateConstructor(tree));
    return describeMatch(tree, suggestedFix.build());
  }

  private static SuggestedFix fixClass(ClassTree classTree, VisitorState state) {
    int startPos = getStartPosition(classTree);
    int endPos = getStartPosition(classTree.getMembers().get(0));
    List<ErrorProneToken> tokens = state.getOffsetTokens(startPos, endPos);
    String modifiers =
        getSymbol(classTree).owner.enclClass() == null ? "final class" : "static final class";
    SuggestedFix.Builder fix = SuggestedFix.builder();
    for (ErrorProneToken token : tokens) {
      if (token.kind() == TokenKind.INTERFACE) {
        fix.replace(token.pos(), token.endPos(), modifiers);
      }
    }
    return fix.build();
  }
}
