/*
 * Copyright 2012 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.ReplacementVariableFinder.fixesByReplacingExpressionWithLocallyDeclaredField;
import static com.google.errorprone.bugpatterns.ReplacementVariableFinder.fixesByReplacingExpressionWithMethodParameter;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.receiverSameAsArgument;
import static com.google.errorprone.matchers.Matchers.variableType;
import static com.sun.source.tree.Tree.Kind.IDENTIFIER;
import static com.sun.source.tree.Tree.Kind.MEMBER_SELECT;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import java.util.List;
import java.util.function.Predicate;
import javax.lang.model.element.ElementKind;

/** @author scottjohnson@google.com (Scott Johnson) */
@BugPattern(
  name = "ModifyingCollectionWithItself",
  summary = "Using a collection function with itself as the argument.",
  category = JDK,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class ModifyingCollectionWithItself extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<MethodInvocationTree> IS_COLLECTION_MODIFIED_WITH_ITSELF =
      buildMatcher();

  private static Matcher<MethodInvocationTree> buildMatcher() {
    return anyOf(
        allOf(
            anyOf(
                instanceMethod().onDescendantOf("java.util.Collection").named("addAll"),
                instanceMethod().onDescendantOf("java.util.Collection").named("removeAll"),
                instanceMethod().onDescendantOf("java.util.Collection").named("containsAll"),
                instanceMethod().onDescendantOf("java.util.Collection").named("retainAll")),
            receiverSameAsArgument(0)),
        allOf(
            instanceMethod().onDescendantOf("java.util.Collection").named("addAll"),
            receiverSameAsArgument(1)));
  }

  /** Matches calls to addAll, containsAll, removeAll, and retainAll on itself */
  @Override
  public Description matchMethodInvocation(MethodInvocationTree t, VisitorState state) {
    if (IS_COLLECTION_MODIFIED_WITH_ITSELF.matches(t, state)) {
      return describe(t, state);
    }
    return Description.NO_MATCH;
  }

  /**
   * We expect that the lhs is a field and the rhs is an identifier, specifically a parameter to the
   * method. We base our suggested fixes on this expectation.
   *
   * <p>Case 1: If lhs is a field and rhs is an identifier, find a method parameter of the same type
   * and similar name and suggest it as the rhs. (Guess that they have misspelled the identifier.)
   *
   * <p>Case 2: If lhs is a field and rhs is not an identifier, find a method parameter of the same
   * type and similar name and suggest it as the rhs.
   *
   * <p>Case 3: If lhs is not a field and rhs is an identifier, find a class field of the same type
   * and similar name and suggest it as the lhs.
   *
   * <p>Case 4: Otherwise replace with literal meaning of functionality
   */
  private Description describe(MethodInvocationTree methodInvocationTree, VisitorState state) {
    ExpressionTree receiver = ASTHelpers.getReceiver(methodInvocationTree);

    List<? extends ExpressionTree> arguments = methodInvocationTree.getArguments();
    ExpressionTree argument;
    // .addAll(int, Collection); for the true case
    argument = arguments.size() == 2 ? arguments.get(1) : arguments.get(0);

    Description.Builder builder = buildDescription(methodInvocationTree);
    for (Fix fix : buildFixes(methodInvocationTree, state, receiver, argument)) {
      builder.addFix(fix);
    }
    return builder.build();
  }

  private List<Fix> buildFixes(
      MethodInvocationTree methodInvocationTree,
      VisitorState state,
      ExpressionTree receiver,
      ExpressionTree argument) {
    List<Fix> fixes;
    // this.a.addAll(...);
    if (receiver.getKind() == MEMBER_SELECT) {
      // Only inspect method parameters, unlikely to want to this.a.addAll(b), where b is another
      // field.
      fixes =
          fixesByReplacingExpressionWithMethodParameter(
              argument, isCollectionVariable(state), state);
    } else {
      // a.addAll(...)
      Preconditions.checkState(receiver.getKind() == IDENTIFIER, "receiver.getKind is identifier");

      boolean lhsIsField = ASTHelpers.getSymbol(receiver).getKind() == ElementKind.FIELD;
      fixes =
          lhsIsField
              ? fixesByReplacingExpressionWithMethodParameter(
                  argument, isCollectionVariable(state), state)
              : fixesByReplacingExpressionWithLocallyDeclaredField(
                  receiver, isCollectionVariable(state), state);
    }

    if (fixes.isEmpty()) {
      fixes = literalReplacement(methodInvocationTree, state, receiver);
    }
    return fixes;
  }

  private static Predicate<JCVariableDecl> isCollectionVariable(final VisitorState state) {
    return var -> variableType(isSubtypeOf("java.util.Collection")).matches(var, state);
  }

  private static ImmutableList<Fix> literalReplacement(
      MethodInvocationTree methodInvocationTree, VisitorState state, ExpressionTree lhs) {

    Tree parent = state.getPath().getParentPath().getLeaf();

    // If the parent is an ExpressionStatement, the expression value is ignored, so we can delete
    // the call entirely (or replace removeAll with .clear()). Otherwise, we can't provide a good
    // replacement.
    if (parent instanceof ExpressionStatementTree) {
      Fix fix;
      if (instanceMethod().anyClass().named("removeAll").matches(methodInvocationTree, state)) {
        fix = SuggestedFix.replace(methodInvocationTree, lhs + ".clear()");
      } else {
        fix = SuggestedFix.delete(parent);
      }
      return ImmutableList.of(fix);
    }

    return ImmutableList.of();
  }
}
