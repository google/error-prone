/*
 * Copyright 2015 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static javax.lang.model.element.ElementKind.TYPE_PARAMETER;

import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.tools.javac.code.Symbol;
import javax.inject.Inject;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Type parameter used as type qualifier",
    severity = ERROR,
    suppressionAnnotations = {})
public class TypeParameterQualifier extends BugChecker
    implements MemberSelectTreeMatcher, MemberReferenceTreeMatcher {
  private final boolean matchMethodReferences;

  @Inject
  TypeParameterQualifier(ErrorProneFlags flags) {
    this.matchMethodReferences =
        flags.getBoolean("TypeParameterQualifier:MatchMethodReferences").orElse(true);
  }

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    return match(tree, tree.getExpression(), state);
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    if (!matchMethodReferences) {
      return NO_MATCH;
    }
    return match(tree, tree.getQualifierExpression(), state);
  }

  private Description match(ExpressionTree tree, ExpressionTree qualifier, VisitorState state) {
    Symbol baseSym = getSymbol(qualifier);
    if (baseSym == null || baseSym.getKind() != TYPE_PARAMETER) {
      return NO_MATCH;
    }
    Symbol memberSym = getSymbol(tree);
    if (memberSym == null) {
      return NO_MATCH;
    }
    var fix = SuggestedFix.builder();
    if (tree instanceof MemberSelectTree) {
      fix.replace(tree, qualifyType(state, fix, memberSym));
    } else {
      fix.replace(qualifier, qualifyType(state, fix, memberSym.owner));
    }
    return describeMatch(tree, fix.build());
  }
}
