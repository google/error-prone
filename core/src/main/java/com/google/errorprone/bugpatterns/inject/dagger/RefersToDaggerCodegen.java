/*
 * Copyright 2019 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.inject.dagger;

import static com.google.errorprone.util.ASTHelpers.enclosingPackage;
import static com.google.errorprone.util.ASTHelpers.getGeneratedBy;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;

/**
 * Checks that the only code that refers to Dagger generated code is other Dagger generated code.
 */
@BugPattern(
    summary = "Don't refer to Dagger's internal or generated code",
    severity = SeverityLevel.ERROR)
public final class RefersToDaggerCodegen extends BugChecker implements MethodInvocationTreeMatcher {
  private static final ImmutableSet<String> DAGGER_INTERNAL_PACKAGES =
      ImmutableSet.of(
          "dagger.internal",
          "dagger.producers.internal",
          "dagger.producers.monitoring.internal",
          "dagger.android.internal");
  private static final ImmutableSet<String> GENERATED_BASE_TYPES =
      ImmutableSet.of("dagger.internal.Factory", "dagger.producers.internal.AbstractProducer");

  /**
   * Dagger 1 does not add an @Generated annotation, but it's code should still be able to refer to
   * {@code dagger.internal} APIs.
   */
  private static final ImmutableSet<String> DAGGER_1_GENERATED_BASE_TYPES =
      ImmutableSet.of("dagger.internal.Binding", "dagger.internal.ModuleAdapter");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MethodSymbol method = getSymbol(tree);
    ClassSymbol rootClassOfMethod = ASTHelpers.outermostClass(method);

    if (!isGeneratedFactoryType(rootClassOfMethod, state)
        && !isMembersInjectionInvocation(method, state)
        && !isDaggerInternalClass(rootClassOfMethod)) {
      return Description.NO_MATCH;
    }

    if (isAllowedToReferenceDaggerInternals(state)) {
      return Description.NO_MATCH;
    }

    return describeMatch(tree);
  }

  private static boolean isMembersInjectionInvocation(MethodSymbol method, VisitorState state) {
    if (method.getSimpleName().contentEquals("injectMembers")) {
      return false;
    }
    return isGeneratedBaseType(ASTHelpers.outermostClass(method), state, "dagger.MembersInjector");
  }

  // TODO(ronshapiro): if we ever start emitting an annotation that has class retention, use that
  // instead of checking for subtypes of generated code
  private static boolean isGeneratedFactoryType(ClassSymbol symbol, VisitorState state) {
    // TODO(ronshapiro): check annotation creators, inaccessible map key proxies, or inaccessible
    // module constructor proxies?
    return GENERATED_BASE_TYPES.stream()
        .anyMatch(baseType -> isGeneratedBaseType(symbol, state, baseType));
  }

  private static boolean isGeneratedBaseType(
      ClassSymbol symbol, VisitorState state, String baseTypeName) {
    Type baseType = state.getTypeFromString(baseTypeName);
    return ASTHelpers.isSubtype(symbol.asType(), baseType, state);
  }

  private static boolean isDaggerInternalClass(ClassSymbol symbol) {
    return DAGGER_INTERNAL_PACKAGES.contains(
        enclosingPackage(symbol).getQualifiedName().toString());
  }

  private static boolean isAllowedToReferenceDaggerInternals(VisitorState state) {
    ClassSymbol rootCallingClass =
        ASTHelpers.outermostClass(getSymbol(state.findEnclosing(ClassTree.class)));
    if (rootCallingClass.getQualifiedName().toString().startsWith("dagger.")) {
      return true;
    }

    ImmutableSet<String> generatedBy = getGeneratedBy(rootCallingClass, state);
    if (!generatedBy.isEmpty()) {
      return generatedBy.contains("dagger.internal.codegen.ComponentProcessor");
    }

    if (DAGGER_1_GENERATED_BASE_TYPES.stream()
        .anyMatch(dagger1Type -> isGeneratedBaseType(rootCallingClass, state, dagger1Type))) {
      return true;
    } else if (isGeneratedBaseType(rootCallingClass, state, "dagger.MembersInjector")
        && rootCallingClass.getSimpleName().toString().contains("$$ParentAdapter")) {
      return true;
    }
    return false;
  }
}
