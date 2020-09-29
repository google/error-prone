/*
 * Copyright 2016 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.android;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anything;
import static com.google.errorprone.matchers.Matchers.classLiteral;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.receiverOfInvocation;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.sun.source.tree.Tree.Kind.IDENTIFIER;
import static javax.lang.model.element.Modifier.FINAL;

import com.google.common.base.Utf8;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;

/** @author epmjohnston@google.com (Emily P.M. Johnston) */
@BugPattern(
    name = "IsLoggableTagLength",
    summary = "Log tag too long, cannot exceed 23 characters.",
    severity = ERROR)
public class IsLoggableTagLength extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> IS_LOGGABLE_CALL =
      staticMethod().onClass("android.util.Log").named("isLoggable");
  private static final Matcher<ExpressionTree> GET_SIMPLE_NAME_CALL =
      instanceMethod().onExactClass("java.lang.Class").named("getSimpleName");
  private static final Matcher<MethodInvocationTree> RECEIVER_IS_CLASS_LITERAL =
      receiverOfInvocation(classLiteral(anything()));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!state.isAndroidCompatible()) {
      return Description.NO_MATCH;
    }
    if (!IS_LOGGABLE_CALL.matches(tree, state)) {
      return NO_MATCH;
    }

    ExpressionTree tagArg = tree.getArguments().get(0);

    // Check for constant value.
    String tagConstantValue = ASTHelpers.constValue(tagArg, String.class);
    if (tagConstantValue != null) {
      return isValidTag(tagConstantValue) ? NO_MATCH : describeMatch(tagArg);
    }

    // Check for class literal simple name (e.g. MyClass.class.getSimpleName().
    ExpressionTree tagExpr = tagArg;
    // If the tag argument is a final field, retrieve the initializer.
    if (kindIs(IDENTIFIER).matches(tagArg, state)) {
      VariableTree declaredField = findEnclosingIdentifier((IdentifierTree) tagArg, state);
      if (declaredField == null || !hasModifier(FINAL).matches(declaredField, state)) {
        return NO_MATCH;
      }
      tagExpr = declaredField.getInitializer();
    }

    if (GET_SIMPLE_NAME_CALL.matches(tagExpr, state)
        && RECEIVER_IS_CLASS_LITERAL.matches((MethodInvocationTree) tagExpr, state)) {
      String tagName = getSymbol(getReceiver(getReceiver(tagExpr))).getSimpleName().toString();
      return isValidTag(tagName) ? NO_MATCH : describeMatch(tagArg);
    }
    return NO_MATCH;
  }

  private boolean isValidTag(String tag) {
    return Utf8.encodedLength(tag) <= 23;
  }

  private VariableTree findEnclosingIdentifier(IdentifierTree originalNode, VisitorState state) {
    Symbol identifierSymbol = getSymbol(originalNode);
    if (!(identifierSymbol instanceof VarSymbol)) {
      return null;
    }
    return state
        .findEnclosing(ClassTree.class)
        .accept(
            new TreeScanner<VariableTree, Void>() {
              @Override
              public VariableTree visitVariable(VariableTree node, Void p) {
                return getSymbol(node).equals(identifierSymbol) ? node : null;
              }

              @Override
              public VariableTree reduce(VariableTree r1, VariableTree r2) {
                return r1 != null ? r1 : r2;
              }
            },
            null);
  }
}
