/*
 * Copyright 2025 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.hasImplicitType;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static javax.lang.model.type.TypeKind.TYPEVAR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.bugpatterns.nullness.NullnessUtils.NullCheck;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;

import java.util.Optional;

import javax.lang.model.element.ElementKind;

@BugPattern(
    summary = "Explicit null check on a variable or method call that is not @Nullable within a @NullMarked scope.",
    severity = WARNING)
public class RedundantNullCheck extends BugChecker implements BinaryTreeMatcher {

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    NullCheck nullCheck = NullnessUtils.getNullCheck(tree);
    if (nullCheck == null) {
      return NO_MATCH;
    }

    VarSymbol varSymbol = nullCheck.varSymbolButUsuallyPreferBareIdentifier();
    if (varSymbol != null) {
      if (!NullnessUtils.isInNullMarkedScope(varSymbol, state)) {
        return NO_MATCH;
      }
      if (NullnessUtils.isAlreadyAnnotatedNullable(varSymbol)) {
        return NO_MATCH;
      }
      if (varSymbol.asType().getKind() == TYPEVAR) {
        return NO_MATCH;
      }

      VariableTree varDecl = NullnessUtils.findDeclaration(state, varSymbol);
      if (varSymbol.getKind() == ElementKind.PARAMETER && hasImplicitType(varDecl, state)) {
        return NO_MATCH;
      }

      if ((varSymbol.getKind() == ElementKind.LOCAL_VARIABLE
          || varSymbol.getKind() == ElementKind.RESOURCE_VARIABLE) && varDecl != null) {

        if (varDecl.getInitializer() == null) {
          return NO_MATCH;
        }

        if (!isConsideredFinal(varSymbol)) {
          return NO_MATCH;
        }

        Tree initializer = varDecl.getInitializer();

        if (initializer.getKind() == Tree.Kind.METHOD_INVOCATION) {
          MethodInvocationTree methodInvocation = (MethodInvocationTree) initializer;
          MethodSymbol methodSymbol = ASTHelpers.getSymbol(methodInvocation);
          if (methodSymbol != null && isEffectivelyNullable(methodSymbol, state)) {
            return NO_MATCH;
          }
        } else if (initializer instanceof LiteralTree) {
          if (initializer.getKind() == Tree.Kind.NULL_LITERAL) {
            return NO_MATCH;
          }
        } else {
          return NO_MATCH;
        }
      }
    } else {
      MethodSymbol methodSymbol = nullCheck.methodSymbol();
      if (methodSymbol == null) {
        return NO_MATCH;
      }
      if (isEffectivelyNullable(methodSymbol, state)) {
        return NO_MATCH;
      }
    }
    return buildDescription(tree).build();
  }

  private static boolean isEffectivelyNullable(MethodSymbol methodSymbol, VisitorState state) {
    Optional<Nullness> returnTypeNullness = NullnessAnnotations.fromAnnotationsOn(methodSymbol);
    if (returnTypeNullness.isPresent()) {
      // Explicit @Nullable or @NonNull on the return type
      return returnTypeNullness.get() == Nullness.NULLABLE;
    }
    if (methodSymbol.isConstructor()) {
      return false;
    }
    if (methodSymbol.getReturnType().getKind() == TYPEVAR) {
      return true;
    }
    // No explicit annotation on return type.
    // Default based on the null-marked status of the method's defining scope.
    // If the method's defining scope is NOT @NullMarked (or is @NullUnmarked),
    // its unannotated return type is effectively nullable.
    return !NullnessUtils.isInNullMarkedScope(methodSymbol, state);
  }
}
