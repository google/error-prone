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
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.equalsMethodDeclaration;
import static com.google.errorprone.matchers.Matchers.instanceEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.staticEqualsInvocation;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import java.util.List;

/**
 * Matches use of {@code BigDecimal#equals}, which compares scale as well (which is not likely to be
 * intended).
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "BigDecimalEquals",
    summary = "BigDecimal#equals has surprising behavior: it also compares scale.",
    category = JDK,
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class BigDecimalEquals extends BugChecker implements MethodInvocationTreeMatcher {
  private static final String BIG_DECIMAL = "java.math.BigDecimal";

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    Tree receiver;
    Tree argument;
    List<? extends ExpressionTree> arguments = tree.getArguments();
    Type bigDecimal = state.getTypeFromString(BIG_DECIMAL);
    boolean handleNulls;

    if (staticEqualsInvocation().matches(tree, state)) {
      handleNulls = true;
      receiver = arguments.get(arguments.size() - 2);
      argument = getLast(arguments);
    } else if (instanceEqualsInvocation().matches(tree, state)) {
      handleNulls = false;
      receiver = getReceiver(tree);
      argument = arguments.get(0);
    } else {
      return NO_MATCH;
    }
    MethodTree enclosingMethod = state.findEnclosing(MethodTree.class);
    if (enclosingMethod != null && equalsMethodDeclaration().matches(enclosingMethod, state)) {
      return NO_MATCH;
    }

    boolean isReceiverBigDecimal = isSameType(getType(receiver), bigDecimal, state);
    boolean isTargetBigDecimal = isSameType(getType(argument), bigDecimal, state);

    if (!isReceiverBigDecimal && !isTargetBigDecimal) {
      return NO_MATCH;
    }

    // One is BigDecimal but the other isn't: report a finding without a fix.
    if (isReceiverBigDecimal != isTargetBigDecimal) {
      return describeMatch(tree);
    }
    return describe(tree, state, receiver, argument, handleNulls);
  }

  private Description describe(
      MethodInvocationTree tree,
      VisitorState state,
      Tree receiver,
      Tree argument,
      boolean handleNulls) {
    return describeMatch(tree);
  }
}
