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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Flags.Flag;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "UnsafeFinalization",
    summary = "Finalizer may run before native code finishes execution",
    severity = WARNING)
public class UnsafeFinalization extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> FENCE_MATCHER =
      staticMethod().onClass("java.lang.ref.Reference").named("reachabilityFence");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MethodSymbol sym = ASTHelpers.getSymbol(tree);
    // Match invocations of static native methods.
    if (sym == null || !sym.isStatic() || !Flags.asFlagSet(sym.flags()).contains(Flag.NATIVE)) {
      return NO_MATCH;
    }
    // Find the enclosing method declaration where the invocation occurs.
    MethodTree method = enclosingMethod(state);
    if (method == null) {
      return NO_MATCH;
    }
    // Don't check native methods called from static methods and constructors:
    // static methods don't have an instance to finalize, and we shouldn't need to worry about
    // finalization during construction.
    MethodSymbol enclosing = ASTHelpers.getSymbol(method);
    if (enclosing == null || enclosing.isStatic() || enclosing.isConstructor()) {
      return NO_MATCH;
    }
    // Check if any arguments of the static native method are members (e.g. fields) of the enclosing
    // class. We're only looking for cases where the static native uses state of the enclosing class
    // that may become invalid after finalization.
    ImmutableList<Symbol> arguments =
        tree.getArguments().stream()
            .map(ASTHelpers::getSymbol)
            .filter(x -> x != null)
            .collect(toImmutableList());
    if (arguments.stream()
        .filter(
            x ->
                EnumSet.of(TypeKind.INT, TypeKind.LONG)
                    .contains(state.getTypes().unboxedTypeOrType(x.asType()).getKind()))
        .noneMatch(arg -> arg.isMemberOf(enclosing.enclClass(), state.getTypes()))) {
      // no instance state is passed to the native method
      return NO_MATCH;
    }
    if (arguments.stream()
        .anyMatch(
            arg ->
                arg.getSimpleName().contentEquals("this")
                    && arg.isMemberOf(enclosing.enclClass(), state.getTypes()))) {
      // the instance is passed to the native method
      return NO_MATCH;
    }
    Symbol finalizeSym = getFinalizer(state, enclosing.enclClass());
    if (finalizeSym.equals(enclosing)) {
      // Don't check native methods called from within the implementation of finalize.
      return NO_MATCH;
    }
    if (finalizeSym.enclClass().equals(state.getSymtab().objectType.asElement())) {
      // Inheriting finalize from Object doesn't count.
      return NO_MATCH;
    }
    boolean[] sawFence = {false};
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        if (FENCE_MATCHER.matches(tree, state)) {
          sawFence[0] = true;
        }
        return null;
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    if (sawFence[0]) {
      // Ignore methods that contain a use of reachabilityFence.
      return NO_MATCH;
    }
    return describeMatch(tree);
  }

  private static Symbol getFinalizer(VisitorState state, ClassSymbol enclosing) {
    Type finalizerType = state.getTypeFromString("com.google.common.labs.base.Finalizer");
    Optional<VarSymbol> finalizerField =
        state.getTypes().closure(enclosing.asType()).stream()
            .flatMap(s -> getFields(s.asElement()))
            .filter(s -> ASTHelpers.isSameType(finalizerType, s.asType(), state))
            .findFirst();
    if (finalizerField.isPresent()) {
      return finalizerField.get();
    }
    return ASTHelpers.resolveExistingMethod(
        state,
        enclosing.enclClass(),
        state.getName("finalize"),
        /* argTypes= */ ImmutableList.of(),
        /* tyargTypes= */ ImmutableList.of());
  }

  private static Stream<VarSymbol> getFields(TypeSymbol s) {
    return Streams.stream(s.members().getSymbols(m -> m.getKind() == ElementKind.FIELD))
        .map(VarSymbol.class::cast);
  }

  private static MethodTree enclosingMethod(VisitorState state) {
    for (Tree parent : state.getPath()) {
      switch (parent.getKind()) {
        case METHOD:
          return (MethodTree) parent;
        case CLASS:
        case LAMBDA_EXPRESSION:
          return null;
        default: // fall out
      }
    }
    return null;
  }
}
