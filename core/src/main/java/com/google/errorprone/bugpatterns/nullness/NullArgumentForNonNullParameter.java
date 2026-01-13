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
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.util.Name;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.lang.model.type.TypeMirror;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "Null is not permitted for this parameter.", severity = ERROR)
public final class NullArgumentForNonNullParameter extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {
  private static final Supplier<Type> JAVA_OPTIONAL_TYPE = typeFromString("java.util.Optional");
  private static final Supplier<Type> ARGUMENT_CAPTOR_CLASS =
      typeFromString("org.mockito.ArgumentCaptor");
  private static final Supplier<Name> OF_NAME = memoize(state -> state.getName("of"));
  private static final Supplier<Name> FOR_CLASS_NAME = memoize(state -> state.getName("forClass"));
  private static final Supplier<Name> PROTO_NONNULL_API_NAME =
      memoize(state -> state.getName("com.google.protobuf.Internal$ProtoNonnullApi"));
  private static final Supplier<Name> NULL_MARKED_NAME =
      memoize(state -> state.getName("org.jspecify.annotations.NullMarked"));
  private static final Supplier<Name> NULL_UNMARKED_NAME =
      memoize(state -> state.getName("org.jspecify.annotations.NullUnmarked"));
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
     * Since not all the classes that we care about have nullness annotations, we can hardcode
     * specific APIs that feel worth the effort, such as those in the JDK. Here's where we put such
     * hardcoding (for the tiny bit we have), aside from one case appears at the top of this method
     * instead so that it can cover test code. (But see the TODO about test code above.)
     */

    // Hardcoding: Optional.of
    if (sym.owner.name.equals(OF_NAME.get(state))
        && isParameterOfMethodOnType(sym, JAVA_OPTIONAL_TYPE, state)) {
      return true;
    }

    Nullness nullness = NullnessAnnotations.fromAnnotationsOn(sym).orElse(null);

    if (nullness == Nullness.NONNULL && !beingConservative) {
      /*
       * Much code in the wild has @NonNull annotations on parameters that are apparently
       * legitimately passed null arguments. Thus, we don't trust such annotations when running in
       * conservative mode (except on type variables, just because that's more convenient to our
       * implementation).
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
      return isNonNull(sym.asType(), /* typeUseSite= */ sym, state);
    }

    if (enclosingAnnotationDefaultsNonTypeVariablesToNonNull(sym, state)) {
      return true;
    }

    return false;
  }

  // https://jspecify.dev/docs/spec/#null-exclusive-under-every-parameterization, "all worlds"
  // but using enclosingAnnotationDefaultsNonTypeVariablesToNonNull instead of isInNullMarkedScope
  private boolean isNonNull(TypeMirror type, Symbol typeUseSite, VisitorState state) {
    Optional<Nullness> nullness = NullnessAnnotations.fromAnnotationsOn(type);
    if (nullness.isPresent()) {
      return nullness.get().equals(Nullness.NONNULL);
    }
    if (type instanceof TypeVar typeVar) {
      Type upperBound = typeVar.getUpperBound();
      return (upperBound instanceof Type.IntersectionClassType intersectionType
              ? intersectionType.getBounds().stream()
              : Stream.of(upperBound))
          .anyMatch(t -> isNonNull(t, /* typeUseSite= */ typeVar.tsym, state));
    }
    return enclosingAnnotationDefaultsNonTypeVariablesToNonNull(typeUseSite, state);
  }

  private static boolean isParameterOfMethodOnType(
      VarSymbol sym, Supplier<Type> typeSupplier, VisitorState state) {
    Type target = typeSupplier.get(state);
    return target != null && state.getTypes().isSameType(enclosingClass(sym).type, target);
  }

  /*
   * TODO(cpovirk): Unify this with NullnessUtils.isInNullMarkedScope, noting that this method also
   * recognizes @ProtoNonnullApi (though perhaps we will successfully migrate protobuf to
   * @NullMarked first) and also that this method trusts @NullMarked only on certain packages.
   */
  private boolean enclosingAnnotationDefaultsNonTypeVariablesToNonNull(
      Symbol sym, VisitorState state) {
    for (; sym != null; sym = sym.getEnclosingElement()) {
      if (hasDirectAnnotation(sym, PROTO_NONNULL_API_NAME.get(state))) {
        return true;
      }
      // https://jspecify.dev/docs/spec/#null-marked-scope
      // TODO(cpovirk): Including handling of @kotlin.Metadata.
      boolean marked = hasDirectAnnotation(sym, NULL_MARKED_NAME.get(state));
      boolean unmarked = hasDirectAnnotation(sym, NULL_UNMARKED_NAME.get(state));
      if (marked && !unmarked && weTrustNullMarkedOn(sym, state)) {
        return true;
      }
      if (unmarked && !marked) {
        return false;
      }
    }
    return false;
  }

  /*
   * ASTHelpers has hasAnnotation and hasDirectAnnotationWithSimpleName but I think not this.
   *
   * We avoid hasAnnotation not just because it's unnecessary but also because it would cause issues
   * under --release 8 on account of NullMarked's use of @Target(MODULE, ...).
   */
  private static boolean hasDirectAnnotation(Symbol sym, Name name) {
    return sym.getAnnotationMirrors().stream()
        .anyMatch(
            a -> ((Symbol) a.getAnnotationType().asElement()).getQualifiedName().equals(name));
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
