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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingMethod;
import static com.google.errorprone.matchers.Matchers.equalsMethodDeclaration;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.util.TreeScanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Discourages implementing {@code equals} using {@code hashCode}.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "EqualsUsingHashCode",
    summary =
        "Implementing #equals by just comparing hashCodes is fragile. Hashes collide "
            + "frequently, and this will lead to false positives in #equals.",
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class EqualsUsingHashCode extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      allOf(
          instanceMethod().anyClass().named("hashCode"),
          enclosingMethod(equalsMethodDeclaration()));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    ReturnTree returnTree = state.findEnclosing(ReturnTree.class);
    if (returnTree == null) {
      return NO_MATCH;
    }
    AtomicBoolean isTerminalCondition = new AtomicBoolean(false);
    returnTree.accept(
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitMethodInvocation(MethodInvocationTree methodTree, Void unused) {
            if (methodTree.equals(tree)) {
              isTerminalCondition.set(true);
            }
            return super.visitMethodInvocation(methodTree, null);
          }

          @Override
          public Void visitBinary(BinaryTree binaryTree, Void unused) {
            return scan(binaryTree.getRightOperand(), null);
          }
        },
        null);
    return isTerminalCondition.get() ? describeMatch(tree) : NO_MATCH;
  }
}
