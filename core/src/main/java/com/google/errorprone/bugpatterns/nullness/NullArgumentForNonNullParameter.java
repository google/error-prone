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

import static com.google.common.collect.Streams.forEachPair;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.VisitorState.memoize;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.hasDefinitelyNullBranch;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.hasExtraParameterForEnclosingInstance;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.nullnessChecksShouldBeConservative;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;
import static com.google.errorprone.util.ASTHelpers.enclosingPackage;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
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

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "Null is not permitted for this parameter.", severity = ERROR)
public final class NullArgumentForNonNullParameter extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {
  private static final Supplier<Type> JAVA_OPTIONAL_TYPE = typeFromString("java.util.Optional");
  private static final Supplier<Type> GUAVA_OPTIONAL_TYPE =
      typeFromString("com.google.common.base.Optional");
  private static final Supplier<Name> OF_NAME = memoize(state -> state.getName("of"));
  private static final Supplier<Name> COM_GOOGLE_COMMON_PREFIX_NAME =
      memoize(state -> state.getName("com.google.common."));

  private final boolean beingConservative;

  public NullArgumentForNonNullParameter(ErrorProneFlags flags) {
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
    if (state.errorProneOptions().isTestOnlyTarget()) {
      return NO_MATCH; // The tests of `foo` often invoke `foo(null)` to verify that it NPEs.
    }

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
    if (sym.asType().isPrimitive()) {
      return true;
    }

    /*
     * Though we get most of our nullness information from annotations, there are technical
     * obstacles to relying purely on them, including around type variables (see comments below)—not
     * to mention that there are no annotations on JDK classes.
     *
     * As a workaround, we can hardcode specific APIs that feel worth the effort. For now, the only
     * ones we hardcode are the two Optional.of methods. Those just happen to be the ones that I
     * thought of and found hits for in our codebase.
     */
    if (sym.owner.name.equals(OF_NAME.get(state))
        && (isParameterOfMethodOnType(sym, JAVA_OPTIONAL_TYPE, state)
            || isParameterOfMethodOnType(sym, GUAVA_OPTIONAL_TYPE, state))) {
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
    return target != null && state.getTypes().isSameType(sym.enclClass().type, target);
  }

  private boolean enclosingAnnotationDefaultsNonTypeVariablesToNonNull(
      Symbol sym, VisitorState state) {
    for (; sym != null; sym = sym.getEnclosingElement()) {
      if (hasAnnotation(sym, "com.google.protobuf.Internal$ProtoNonnullApi", state)) {
        return true;
      }
      /*
       * Similar to @NonNull (discussed above), the "default to non-null" annotation @NullMarked is
       * sometimes used on code that hasn't had @Nullable annotations added to it where necessary.
       * To avoid false positives, our conservative mode trusts @NullMarked only when it appears in
       * com.google.common.
       *
       * TODO(cpovirk): Expand the list of packages that our conservative mode trusts @NullMarked
       * on. We might be able to identify some packages that would be safe to trust today. For
       * others, we could use ParameterMissingNullable, which adds missing annotations in situations
       * similar to the ones identified by this check. (But note that ParameterMissingNullable
       * doesn't help with calls that cross file boundaries.)
       */
      if (hasAnnotation(sym, "org.jspecify.nullness.NullMarked", state)
          && (!beingConservative
              || enclosingPackage(sym)
                  .fullname
                  .startsWith(COM_GOOGLE_COMMON_PREFIX_NAME.get(state)))) {
        return true;
      }
    }
    return false;
  }
}
