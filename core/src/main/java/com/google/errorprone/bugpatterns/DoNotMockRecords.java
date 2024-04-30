/*
 * Copyright 2024 The Error Prone Authors.
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

import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.collect.Lists;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;

import javax.lang.model.element.ElementKind;

@BugPattern(
    name = "DoNotMockRecords",
    summary = "Records are intended to model plain data so mocking them should not be necessary. "
        + "Construct a real instance of the class instead.",
    severity = SeverityLevel.SUGGESTION)
public final class DoNotMockRecords extends AbstractMockChecker {

  public DoNotMockRecords() {
    super(MOCKING_ANNOTATION, MOCKING_METHOD);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return methodExtractor
        .extract(tree, state)
        .flatMap(type -> argFromClass(type, state))
        .map(type -> checkMockedType(type, tree, state))
        .orElse(NO_MATCH);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    return varExtractor
        .extract(tree, state)
        .map(type -> checkMockedType(type, tree, state))
        .orElse(NO_MATCH);
  }

  @Override
  protected Description checkMockedType(Type mockedClass, Tree tree, VisitorState state) {
    if (ASTHelpers.isSameType(Type.noType, mockedClass, state)) {
      return NO_MATCH;
    }

    for (final Type currentType : Lists.reverse(state.getTypes().closure(mockedClass))) {
      final TypeSymbol currentSymbol = currentType.asElement();
      if (ElementKind.RECORD == currentSymbol.getKind()) {
        return buildDescription(tree)
            .setMessage(buildMessage(mockedClass, currentSymbol))
            .build();
      }
    }

    return NO_MATCH;
  }
}