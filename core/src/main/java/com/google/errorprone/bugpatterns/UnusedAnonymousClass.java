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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.TypeSymbol;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "UnusedAnonymousClass",
    summary = "Instance created but never used",
    severity = ERROR)
public class UnusedAnonymousClass extends BugChecker implements NewClassTreeMatcher {

  // An anonymous class creation cannot have side-effects if:
  // (1) it is of an interface type (no super-class constructor side-effects)
  // (2) and has no instance initializer blocks or field initializers
  @Override
  public Description matchNewClass(NewClassTree newClassTree, VisitorState state) {
    if (state.getPath().getParentPath().getLeaf().getKind() != Kind.EXPRESSION_STATEMENT) {
      return Description.NO_MATCH;
    }
    if (newClassTree.getClassBody() == null) {
      return Description.NO_MATCH;
    }
    if (!newClassTree.getArguments().isEmpty()) {
      return Description.NO_MATCH;
    }
    for (Tree def : newClassTree.getClassBody().getMembers()) {
      switch (def.getKind()) {
        case VARIABLE:
          {
            VariableTree variableTree = (VariableTree) def;
            if (variableTree.getInitializer() != null) {
              return Description.NO_MATCH;
            }
            break;
          }
        case BLOCK:
          return Description.NO_MATCH;
        default:
          break;
      }
    }
    if (!sideEffectFreeConstructor(ASTHelpers.getType(newClassTree.getIdentifier()).tsym, state)) {
      return Description.NO_MATCH;
    }
    return describeMatch(newClassTree);
  }

  private static final ImmutableList<String> TYPES_WITH_SIDE_EFFECT_FREE_CONSTRUCTORS =
      ImmutableList.of(Thread.class.getName());

  private boolean sideEffectFreeConstructor(TypeSymbol classType, VisitorState state) {
    if (classType.isInterface()) {
      return true;
    }
    for (String typeName : TYPES_WITH_SIDE_EFFECT_FREE_CONSTRUCTORS) {
      if (ASTHelpers.isSameType(classType.type, state.getTypeFromString(typeName), state)) {
        return true;
      }
    }
    return false;
  }
}
