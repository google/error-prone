/*
 * Copyright 2020 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.findSuperMethodInType;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ASTHelpers.targetType;
import static javax.lang.model.element.Modifier.ABSTRACT;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers.TargetType;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Scope.WriteableScope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;

/** Matches unnecessary uses of method references. */
@BugPattern(
    name = "UnnecessaryMethodReference",
    severity = SeverityLevel.WARNING,
    summary = "This method reference is unnecessary, and can be replaced with the variable itself.")
public final class UnnecessaryMethodReference extends BugChecker
    implements MemberReferenceTreeMatcher {

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    if (!tree.getMode().equals(ReferenceMode.INVOKE)) {
      return NO_MATCH;
    }
    if (!(state.getPath().getParentPath().getLeaf() instanceof MethodInvocationTree)) {
      return NO_MATCH;
    }
    TargetType targetType = targetType(state);
    if (targetType == null) {
      return NO_MATCH;
    }
    ExpressionTree receiver = getReceiver(tree);
    if (receiver == null) {
      return NO_MATCH;
    }
    if (!isSubtype(getType(receiver), targetType.type(), state)) {
      return NO_MATCH;
    }
    MethodSymbol symbol = getSymbol(tree);
    WriteableScope members = targetType.type().tsym.members();
    if (!members.anyMatch(
        sym -> isFunctionalInterfaceInvocation(symbol, targetType.type(), sym, state))) {
      return NO_MATCH;
    }
    return describeMatch(
        tree, replace(state.getEndPosition(receiver), state.getEndPosition(tree), ""));
  }

  private static boolean isFunctionalInterfaceInvocation(
      MethodSymbol memberReferenceTarget, Type type, Symbol symbol, VisitorState state) {
    return (memberReferenceTarget.equals(symbol)
            || findSuperMethodInType(memberReferenceTarget, type, state.getTypes()) != null)
        && symbol.getModifiers().contains(ABSTRACT);
  }
}
