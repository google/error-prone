/*
 * Copyright 2017 The Error Prone Authors.
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
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import java.util.function.Function;
import java.util.stream.Collectors;

/** @author sulku@google.com (Marsela Sulku) */
@BugPattern(
    name = "MultipleUnaryOperatorsInMethodCall",
    summary = "Avoid having multiple unary operators acting on the same variable in a method call",
    category = JDK,
    severity = SUGGESTION)
public class MultipleUnaryOperatorsInMethodCall extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final ImmutableSet<Kind> UNARY_OPERATORS =
      ImmutableSet.of(
          Kind.POSTFIX_DECREMENT,
          Kind.POSTFIX_INCREMENT,
          Kind.PREFIX_DECREMENT,
          Kind.PREFIX_INCREMENT);

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState visitorState) {

    if (methodInvocationTree.getArguments().stream()
        .filter(arg -> UNARY_OPERATORS.contains(arg.getKind()))
        .map(arg -> ASTHelpers.getSymbol(((UnaryTree) arg).getExpression()))
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
        .entrySet()
        .stream()
        .anyMatch(e -> e.getValue() > 1)) {
      return describeMatch(methodInvocationTree);
    }
    return Description.NO_MATCH;
  }
}
