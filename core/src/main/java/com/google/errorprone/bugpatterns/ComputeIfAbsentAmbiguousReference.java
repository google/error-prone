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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Flags ambiguous creations of objects in {@link java.util.Map#computeIfAbsent}.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    name = "ComputeIfAbsentAmbiguousReference",
    summary = "computeIfAbsent passes the map key to the provided class's constructor",
    severity = ERROR)
public final class ComputeIfAbsentAmbiguousReference extends BugChecker
    implements MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> COMPUTE_IF_ABSENT =
      instanceMethod().onDescendantOf("java.util.Map").named("computeIfAbsent");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!COMPUTE_IF_ABSENT.matches(tree, state)) {
      return NO_MATCH;
    }
    ExpressionTree mappingFunctionArg = tree.getArguments().get(1);
    if (!(mappingFunctionArg instanceof MemberReferenceTree)) {
      return NO_MATCH;
    }
    MemberReferenceTree memberReferenceTree = (MemberReferenceTree) mappingFunctionArg;
    if (memberReferenceTree.getMode() != ReferenceMode.NEW) {
      return NO_MATCH;
    }
    ExpressionTree expressionTree = memberReferenceTree.getQualifierExpression();
    Symbol symbol = ASTHelpers.getSymbol(expressionTree);
    if (!(symbol instanceof ClassSymbol)) {
      return NO_MATCH;
    }
    ClassSymbol classSymbol = (ClassSymbol) symbol;
    ImmutableList<MethodSymbol> constructors = ASTHelpers.getConstructors(classSymbol);
    List<MethodSymbol> zeroArgConstructors =
        constructors.stream()
            .filter(methodSymbol -> methodSymbol.type.getParameterTypes().isEmpty())
            .collect(Collectors.toList());
    if (zeroArgConstructors.size() != 1) {
      return NO_MATCH;
    }
    List<MethodSymbol> oneArgConstructors =
        constructors.stream()
            .filter(methodSymbol -> methodSymbol.type.getParameterTypes().size() == 1)
            .collect(Collectors.toList());
    if (oneArgConstructors.size() >= 1) {
      return describeMatch(memberReferenceTree);
    }
    return NO_MATCH;
  }
}
