/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/** Removes {@code final} from non-field variables. */
@BugPattern(
    summary =
        "Since Java 8, it's been unnecessary to make local variables and parameters `final` for use"
            + " in lambdas or anonymous classes. Marking them as `final` is weakly discouraged, as"
            + " it adds a fair amount of noise for minimal benefit.",
    severity = WARNING)
public final class UnnecessaryFinal extends BugChecker implements VariableTreeMatcher {
  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    Symbol symbol = getSymbol(tree);
    if (symbol.getKind() == ElementKind.FIELD) {
      return NO_MATCH;
    }
    if (!tree.getModifiers().getFlags().contains(Modifier.FINAL)) {
      return NO_MATCH;
    }
    ImmutableList<ErrorProneToken> tokens =
        ErrorProneTokens.getTokens(state.getSourceForNode(tree.getModifiers()), state.context);
    for (ErrorProneToken token : tokens) {
      if (token.kind() == TokenKind.FINAL) {
        int startPos = getStartPosition(tree);
        return describeMatch(
            tree, SuggestedFix.replace(startPos + token.pos(), startPos + token.endPos(), ""));
      }
    }
    return NO_MATCH;
  }
}
