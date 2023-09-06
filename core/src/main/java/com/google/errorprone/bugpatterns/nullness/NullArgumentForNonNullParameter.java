/*
 * Copyright 2022 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Streams.forEachPair;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.VisitorState.memoize;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.hasDefinitelyNullBranch;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.hasExtraParameterForEnclosingInstance;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.nullnessChecksShouldBeConservative;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.enclosingPackage;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static java.util.Arrays.stream;
import static javax.lang.model.type.TypeKind.TYPEVAR;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Name;
import java.util.List;
import javax.inject.Inject;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "Null is not permitted for this parameter.", severity = ERROR)
public final class NullArgumentForNonNullParameter extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {
  private static final Supplier<Type> JAVA_OPTIONAL_TYPE = typeFromString("java.util.Optional");
  private static final Supplier<Type> GUAVA_OPTIONAL_TYPE =
      typeFromString("com.google.common.base.Optional");
  private static final Supplier<Type> ARGUMENT_CAPTOR_CLASS =
      typeFromString("org.mockito.ArgumentCaptor");
  private static final Supplier<Name> OF_NAME = memoize(state -> state.getName("of"));
  private static final Supplier<Name> FOR_CLASS_NAME = memoize(state -> state.getName("forClass"));
  private static final Supplier<Name> BUILDER_NAME = memoize(state -> state.getName("Builder"));
  private static final Supplier<Name> GUAVA_COLLECT_IMMUTABLE_PREFIX =
      memoize(state -> state.getName("com.google.common.collect.Immutable"));
  private static final Supplier<Name> GUAVA_GRAPH_IMMUTABLE_PREFIX =
      memoize(state -> state.getName("com.google.common.graph.Immutable"));
  private static final Supplier<ImmutableSet<Name>> NULL_MARKED_PACKAGES_WE_TRUST =
      memoize(
          state ->
              stream(
                      new String[] {
                        "com.google.common",
                      })
                  .map(state::getName)
                  .collect(toImmutableSet()));

  private final boolean beingConservative;

  @Inject
  NullArgumentForNonNullParameter(ErrorProneFlags flags) {
    this.beingConservative = nullnessChecksShouldBeConservative(flags);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return match(getSymbol(tree), tree.getArguments(), state);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    return match(getSymbol(tree), tree.getArguments(), state);
  }

  private Description match(
      MethodSymbol methodSymbol, List<? extends ExpressionTree> args, VisitorState state) {
    if (hasExtraParameterForEnclosingInstance(methodSymbol)) {
      // TODO(b/232103314): Figure out the right way to handle the implicit outer `this` parameter.
      return NO_MATCH;
    }

    if (methodSymbol.isVarArgs()) {
      /*
       * TODO(b/232103314): Figure out the right way to handle this, or at least handle all
       * parameters but the last.
       */
      return NO_MATCH;
    }

    forEachPair(
        args.stream(),
        methodSymbol.getParameters().stream(),
        (argTree, paramSymbol) -> {
          if (!hasDefinitelyNullBranch(
              argTree,
              /*
               * TODO(cpovirk): Precompute sets of definitelyNullVars and varsProvenNullByParentIf
               * instead of passing empty sets.
               */
              ImmutableSet.of(),
              ImmutableSet.of(),
              state)) {
            return;
          }

          if (!argumentMustBeNonNull(paramSymbol, state)) {
            return;
          }

          state.reportMatch(describeMatch(argTree));
        });

    return NO_MATCH; // Any matches were reported through state.reportMatch.
  }

  private boolean argumentMustBeNonNull(VarSymbol sym, VisitorState state) {
    // We hardcode checking of one test method, ArgumentCaptor.forClass, which throws as of
    // https://github.com/mockito/mockito/commit/fe1cb2de0923e78bf7d7ae46cbab792dd4e94136#diff-8d274a9bda2d871524d15bbfcd6272bd893a47e6b1a0b460d82a8845615f26daR31
    // For discussion of hardcoding in general, see below.
    if (sym.owner.name.equals(FOR_CLASS_NAME.get(state))
        && isParameterOfMethodOnType(sym, ARGUMENT_CAPTOR_CLASS, state)) {
      return true;
    }

    if (state.errorProneOptions().isTestOnlyTarget()) {
      return false; // The tests of `foo` often invoke `foo(null)` to verify that it NPEs.
      /*
       * TODO(cpovirk): But consider still matching *some* cases. For example, we might check
       * primitives, since it would be strange to test that `foo(int i)` throws NPE if you call
       * `foo((Integer) null)`. And tests that *use* a class like `Optional` (as opposed to
       * *testing* Optional) could benefit from checking that they use `Optional.of` correctly.
       */
    }

    if (sym.asType().isPrimitive()) {
      return true;
    }

    /*
     * Though we get most of our nullness information from annotations, there are technical
     * obstacles to relying purely on them, including around type variables (see comments below)—not
     * to mention that there are no annotations on JDK classes.
     *
     * As a workaround, we can hardcode specific APIs that feel worth the effort. Most of the
     * hardcoding is below, but one bit is at the top of this method.
     */

    // Hardcoding #1: Optional.of
    if (sym.owner.name.equals(OF_NAME.get(state))
        && (isParameterOfMethodOnType(sym, JAVA_OPTIONAL_TYPE, state)
            || isParameterOfMethodOnType(sym, GUAVA_OPTIONAL_TYPE, state))) {
      return true;
    }

    // Hardcoding #2: Immutable*.of
    if (sym.owner.name.equals(OF_NAME.get(state))
        && (isParameterOfMethodOnTypeStartingWith(sym, GUAVA_COLLECT_IMMUTABLE_PREFIX, state)
            || isParameterOfMethodOnTypeStartingWith(sym, GUAVA_GRAPH_IMMUTABLE_PREFIX, state))) {
      return true;
    }

    // Hardcoding #3: Immutable*.Builder.*
    if (enclosingClass(sym).name.equals(BUILDER_NAME.get(state))
        && (isParameterOfMethodOnTypeStartingWith(sym, GUAVA_COLLECT_IMMUTABLE_PREFIX, state)
            || isParameterOfMethodOnTypeStartingWith(sym, GUAVA_GRAPH_IMMUTABLE_PREFIX, state))) {
      return true;
    }

    /*
     * TODO(b/203207989): In theory, we should program this check to exclude inner classes until we
     * fix the bug in MoreAnnotations.getDeclarationAndTypeAttributes, which is used by
     * fromAnnotationsOn. In practice, annotations on both inner classes and outer classes are rare
     * (especially when NullableOnContainingClass is enabled!), so this code currently still looks
     * at parameters that are inner types, even though we might misinterpret them.
     */
    Nullness nullness = NullnessAnnotations.fromAnnotationsOn(sym).orElse(null);

    if (nullness == Nullness.NONNULL && !beingConservative) {
      /*
       * Much code in the wild has @NonNull annotations on parameters that are apparently
       * legitimately passed null arguments. Thus, we don't trust such annotations when running in
       * conservative mode.
       *
       * TODO(cpovirk): Instead of ignoring @NonNull annotations entirely, add special cases for
       * specific badly annotated APIs. Or better yet, get the annotations fixed.
       */
      return true;
    }
    if (nullness == Nullness.NULLABLE) {
      return false;
    }

    if (sym.asType().getKind() == TYPEVAR) {
      /*
       * TODO(cpovirk): We could sometimes return true if we looked for @NullMarked and for any
       * annotations on the type-parameter bounds. But looking at type-parameter bounds is hard
       * because of JDK-8225377.
       */
      return false;
    }

    if (enclosingAnnotationDefaultsNonTypeVariablesToNonNull(sym, state)) {
      return true;
    }

    return false;
  }

  private static boolean isParameterOfMethodOnType(
      VarSymbol sym, Supplier<Type> typeSupplier, VisitorState state) {
    Type target = typeSupplier.get(state);
    return target != null && state.getTypes().isSameType(enclosingClass(sym).type, target);
  }

  private static boolean isParameterOfMethodOnTypeStartingWith(
      VarSymbol sym, Supplier<Name> nameSupplier, VisitorState state) {
    return enclosingClass(sym).fullname.startsWith(nameSupplier.get(state));
  }

  private boolean enclosingAnnotationDefaultsNonTypeVariablesToNonNull(
      Symbol sym, VisitorState state) {
    for (; sym != null; sym = sym.getEnclosingElement()) {
      if (hasAnnotation(sym, "com.google.protobuf.Internal$ProtoNonnullApi", state)) {
        return true;
      }
      if ((hasAnnotation(sym, "org.jspecify.annotations.NullMarked", state)
              // We break this string to avoid having it rewritten by Copybara.
              || hasAnnotation(sym, "org.jspecify.null" + "ness.NullMarked", state))
          && weTrustNullMarkedOn(sym, state)) {
        return true;
      }
    }
    return false;
  }

  private boolean weTrustNullMarkedOn(Symbol sym, VisitorState state) {
    /*
     * Similar to @NonNull (discussed above), the "default to non-null" annotation @NullMarked is
     * sometimes used on code that hasn't had @Nullable annotations added to it where necessary. To
     * avoid false positives, our conservative mode trusts @NullMarked only when it appears in a
     * package from a list that we maintain in this checker.
     *
     * TODO(cpovirk): Expand the list of packages that our conservative mode trusts @NullMarked on.
     * We might be able to identify some packages that would be safe to trust today. For others, we
     * could use ParameterMissingNullable, which adds missing annotations in situations similar to
     * the ones identified by this check. (But note that ParameterMissingNullable doesn't help with
     * calls that cross file boundaries.)
     */

    if (!beingConservative) {
      return true;
    }

    ImmutableSet<Name> packagesWeTrust = NULL_MARKED_PACKAGES_WE_TRUST.get(state);
    for (sym = enclosingPackage(sym); sym != null; sym = sym.owner) {
      if (packagesWeTrust.contains(sym.getQualifiedName())) {
        return true;
      }
    }
    return false;
  }
}
