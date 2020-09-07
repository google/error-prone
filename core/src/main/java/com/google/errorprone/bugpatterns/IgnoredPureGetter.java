/*
 * Copyright 2020 The Error Prone Authors.
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
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static javax.lang.model.element.Modifier.ABSTRACT;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/** Flags ignored return values from pure getters. */
@BugPattern(
    name = "IgnoredPureGetter",
    severity = WARNING,
    summary =
        "Getters on AutoValue classes and protos are side-effect free, so there is no point in"
            + " calling them if the return value is ignored.")
public final class IgnoredPureGetter extends AbstractReturnValueIgnored {
  private static final String MESSAGE_LITE = "com.google.protobuf.MessageLite";

  private static final String MUTABLE_MESSAGE_LITE = "com.google.protobuf.MutableMessageLite";

  @Override
  protected Matcher<? super ExpressionTree> specializedMatcher() {
    return IgnoredPureGetter::isPureGetter;
  }

  @Override
  protected Description describeReturnValueIgnored(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    Description.Builder builder =
        buildDescription(methodInvocationTree)
            .addFix(
                SuggestedFix.builder()
                    .setShortDescription("Delete entire statement")
                    .delete(methodInvocationTree)
                    .build());
    ExpressionTree receiver = getReceiver(methodInvocationTree);
    if (receiver instanceof MethodInvocationTree) {
      builder.addFix(
          SuggestedFix.builder()
              .setShortDescription("Delete getter only")
              .replace(methodInvocationTree, state.getSourceForNode(receiver))
              .build());
    }
    return builder.build();
  }

  private static boolean isPureGetter(ExpressionTree tree, VisitorState state) {
    Symbol symbol = getSymbol(tree);
    if (!(symbol instanceof MethodSymbol)) {
      return false;
    }
    if (hasAnnotation(symbol.owner, "com.google.auto.value.AutoValue", state)
        && symbol.getModifiers().contains(ABSTRACT)) {
      return true;
    }
    if (isSubtype(symbol.owner.type, state.getTypeFromString(MESSAGE_LITE), state)
        && !isSubtype(symbol.owner.type, state.getTypeFromString(MUTABLE_MESSAGE_LITE), state)) {
      String name = symbol.getSimpleName().toString();
      return (name.startsWith("get") || name.startsWith("has"))
          && ((MethodSymbol) symbol).getParameters().isEmpty();
    }
    return false;
  }
}
