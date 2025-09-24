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
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.hasImplicitType;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static javax.lang.model.type.TypeKind.TYPEVAR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.nullness.NullnessUtils.NullCheck;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;

import java.util.Optional;

import javax.inject.Inject;
import javax.lang.model.element.ElementKind;

@BugPattern(
    summary =
        "Null check on an expression that is statically determined to be non-null according to "
            + "language semantics or nullness annotations.",
    severity = WARNING)
public class RedundantNullCheck extends BugChecker
    implements BinaryTreeMatcher, MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> OBJECTS_REQUIRE_NON_NULL =
      staticMethod().onClass("java.util.Objects").named("requireNonNull");

  private final boolean checkRequireNonNull;

  @Inject
  public RedundantNullCheck(ErrorProneFlags flags) {
    this.checkRequireNonNull =
        flags.getBoolean("RedundantNullCheck:CheckRequireNonNull").orElse(false);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!checkRequireNonNull) {
      return NO_MATCH;
    }
    if (!OBJECTS_REQUIRE_NON_NULL.matches(tree, state)) {
      return NO_MATCH;
    }
    if (tree.getArguments().isEmpty()) {
      return NO_MATCH;
    }
    ExpressionTree arg = tree.getArguments().get(0);
    Symbol symbol = ASTHelpers.getSymbol(arg);

    boolean isRedundant = false;
    if (symbol instanceof VarSymbol) {
      isRedundant = !isEffectivelyNullable((VarSymbol) symbol, state);
    } else if (symbol instanceof MethodSymbol) {
      isRedundant = !isEffectivelyNullable((MethodSymbol) symbol, state);
    }

    if (isRedundant) {
      return buildDescription(tree).build();
    }
    return NO_MATCH;
  }

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    NullCheck nullCheck = NullnessUtils.getNullCheck(tree);
    if (nullCheck == null) {
      return NO_MATCH;
    }

    boolean isRedundant = false;
    VarSymbol varSymbol = nullCheck.varSymbolButUsuallyPreferBareIdentifier();
    if (varSymbol != null) {
      isRedundant = !isEffectivelyNullable(varSymbol, state);
    } else {
      MethodSymbol methodSymbol = nullCheck.methodSymbol();
      if (methodSymbol != null) {
        isRedundant = !isEffectivelyNullable(methodSymbol, state);
      }
    }

    if (isRedundant) {
      return buildDescription(tree).build();
    }
    return NO_MATCH;
  }

  private static boolean isEffectivelyNullable(VarSymbol varSymbol, VisitorState state) {
    boolean isLocalOrResourceVariable =
        varSymbol.getKind() == ElementKind.LOCAL_VARIABLE
            || varSymbol.getKind() == ElementKind.RESOURCE_VARIABLE;

    if (!isLocalOrResourceVariable) {
      Optional<Nullness> varNullness = NullnessAnnotations.fromAnnotationsOn(varSymbol);
      if (varNullness.isPresent()) {
        return varNullness.get() == Nullness.NULLABLE;
      }
    }

    if (varSymbol.asType().getKind() == TYPEVAR) {
      return true;
    }

    VariableTree varDecl = NullnessUtils.findDeclaration(state, varSymbol);
    if (varSymbol.getKind() == ElementKind.PARAMETER && hasImplicitType(varDecl, state)) {
      return true;
    }

    if (isLocalOrResourceVariable && varDecl != null) {
      if (varDecl.getInitializer() == null) {
        return true;
      }

      if (!isConsideredFinal(varSymbol)) {
        return true;
      }

      Tree initializer = varDecl.getInitializer();

      if (initializer.getKind() == Tree.Kind.METHOD_INVOCATION) {
        MethodInvocationTree methodInvocation = (MethodInvocationTree) initializer;
        MethodSymbol methodSymbol = ASTHelpers.getSymbol(methodInvocation);
        return methodSymbol == null || isEffectivelyNullable(methodSymbol, state);
      } else if (initializer instanceof LiteralTree) {
        return initializer.getKind() == Tree.Kind.NULL_LITERAL;
      }

      return true;
    }

    return !NullnessUtils.isInNullMarkedScope(varSymbol, state);
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
