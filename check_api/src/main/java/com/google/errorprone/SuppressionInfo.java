/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone;

import static com.google.common.collect.Sets.difference;
import static com.google.errorprone.VisitorState.memoize;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.matchers.Suppressible;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.util.Name;
import java.util.Collections;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;

/**
 * Immutable container of "suppression signals" - annotations or other information gathered from
 * source - that can be used to determine if a specific {@link Suppressible} object should be
 * considered suppressed.
 *
 * <p>SuppressionInfo instances are obtained by starting with the {@link #EMPTY} instance, then
 * getting new instances by calling {@link #withExtendedSuppressions} with symbols discovered as you
 * descend a program tree.
 */
@Immutable
@CheckReturnValue
public final class SuppressionInfo {
  public static final SuppressionInfo EMPTY =
      new SuppressionInfo(
          /* suppressWarningsStrings= */ ImmutableSet.of(),
          /* hasSuppressWarningsAll= */ false,
          /* customSuppressions= */ ImmutableSet.of(),
          /* inGeneratedCode= */ false);

  private static final ClassName ANDROID_SUPPRESS_LINT =
      ClassName.of("android.annotation.SuppressLint");
  private static final Supplier<Name> GENERATED = memoize(state -> state.getName("Generated"));

  /**
   * Strings present in {@code @SuppressWarnings} or {@code @SuppressLint}, except {@code "all"}
   * (which sets {@link #hasSuppressWarningsAll} instead).
   */
  private final ImmutableSet<String> suppressWarningsStrings;

  private final boolean hasSuppressWarningsAll;

  @SuppressWarnings("Immutable") /* Name is javac's interned version of a string. */
  private final ImmutableSet<Name> customSuppressions;

  private final boolean inGeneratedCode;

  private SuppressionInfo(
      ImmutableSet<String> suppressWarningsStrings,
      boolean hasSuppressWarningsAll,
      Set<Name> customSuppressions,
      boolean inGeneratedCode) {
    this.suppressWarningsStrings = suppressWarningsStrings;
    this.hasSuppressWarningsAll = hasSuppressWarningsAll;
    this.customSuppressions = ImmutableSet.copyOf(customSuppressions);
    this.inGeneratedCode = inGeneratedCode;
  }

  private static boolean isGenerated(Symbol sym, VisitorState state) {
    var attributes = sym.getRawAttributes();
    if (attributes.isEmpty()) {
      return false;
    }
    Name generated = GENERATED.get(state);
    for (Compound attribute : attributes) {
      if (attribute.type.tsym.getSimpleName().equals(generated)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if this checker should be considered suppressed given the signals present in this
   * object.
   *
   * @param suppressible Holds information about the suppressibilty of a checker
   * @param suppressedInGeneratedCode true if this checker instance should be considered suppressed
   */
  public SuppressedState suppressedState(
      Suppressible suppressible, boolean suppressedInGeneratedCode, VisitorState state) {
    if (inGeneratedCode && suppressedInGeneratedCode) {
      return SuppressedState.SUPPRESSED;
    }
    if (suppressible.supportsSuppressWarnings()
        && (hasSuppressWarningsAll
            || (!suppressWarningsStrings.isEmpty()
                && !Collections.disjoint(suppressible.allNames(), suppressWarningsStrings)))) {
      return SuppressedState.SUPPRESSED;
    }
    if (suppressible.suppressedByAnyOf(customSuppressions, state)) {
      return SuppressedState.SUPPRESSED;
    }

    return SuppressedState.UNSUPPRESSED;
  }

  /**
   * Generates the {@link SuppressionInfo} for a {@link CompilationUnitTree}. This differs in that
   * {@code isGenerated} is determined by inspecting the annotations of the outermost class so that
   * matchers on {@link CompilationUnitTree} will also be suppressed.
   */
  public SuppressionInfo forCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    var visitor =
        new SimpleTreeVisitor<Void, Void>() {
          boolean generated;

          @Override
          public Void visitClass(ClassTree node, Void unused) {
            generated = generated || isGenerated(getSymbol(node), state);
            return null;
          }
        };
    visitor.visit(tree.getTypeDecls(), null);
    return new SuppressionInfo(
        suppressWarningsStrings, hasSuppressWarningsAll, customSuppressions, visitor.generated);
  }

  /**
   * Returns an instance of {@code SuppressionInfo} that takes into account any suppression signals
   * present on {@code sym} as well as those already stored in {@code this}.
   *
   * <p>Checks suppressions for any {@code @SuppressWarnings}, Android's {@code SuppressLint}, and
   * custom suppression annotations described by {@code customSuppressionAnnosToLookFor}.
   *
   * <p>We do not modify the existing suppression sets, so they can be restored when moving up the
   * tree. We also avoid copying the suppression sets if the next node to explore does not have any
   * suppressed warnings or custom suppression annotations. This is the common case.
   *
   * @param sym The {@code Symbol} for the AST node currently being scanned
   * @param state VisitorState for checking the current tree, as well as for getting the {@code
   *     SuppressWarnings symbol type}.
   */
  public SuppressionInfo withExtendedSuppressions(
      Symbol sym, VisitorState state, Set<? extends Name> customSuppressionAnnosToLookFor) {
    boolean newInGeneratedCode = inGeneratedCode || isGenerated(sym, state);
    boolean anyModification = newInGeneratedCode != inGeneratedCode;

    /* Handle custom suppression annotations. */
    Set<? extends Name> lookingFor =
        difference(customSuppressionAnnosToLookFor, customSuppressions);
    /*
     * TODO(cpovirk): Inline annotationsAmong logic here (but it handles @Inherited!), and change
     * this logic to look more like the SuppressWarnings+SuppressLint handling below, including
     * avoiding constructing even the lookingFor view?
     */
    Set<Name> newlyPresent = ASTHelpers.annotationsAmong(sym, lookingFor, state);
    Set<Name> newCustomSuppressions;
    if (!newlyPresent.isEmpty()) {
      anyModification = true;
      newCustomSuppressions = newlyPresent;
      newCustomSuppressions.addAll(customSuppressions);
    } else {
      newCustomSuppressions = customSuppressions;
    }

    /* Handle {@code @SuppressWarnings} and {@code @SuppressLint}. */
    Name suppressLint = ANDROID_SUPPRESS_LINT.binaryName(state);
    boolean newHasSuppressWarningsAll = hasSuppressWarningsAll;
    ImmutableSet.Builder<String> newSuppressions = null;
    // Iterate over annotations on this symbol, looking for SuppressWarnings
    for (AnnotationMirror attr : sym.getAnnotationMirrors()) {
      TypeSymbol tsym = MoreAnnotations.asElement(attr);
      if (tsym.equals(state.getSymtab().suppressWarningsType.tsym)
          || tsym.flatName().equals(suppressLint)) {
        for (String suppressedWarning : MoreAnnotations.getStrings(attr, "value")) {
          if (suppressedWarning.equals("all")) {
            if (!newHasSuppressWarningsAll) {
              newHasSuppressWarningsAll = true;
              anyModification = true;
            }
          } else if (!suppressWarningsStrings.contains(suppressedWarning)) {
            anyModification = true;
            if (newSuppressions == null) {
              newSuppressions =
                  ImmutableSet.builderWithExpectedSize(suppressWarningsStrings.size() + 1);
              newSuppressions.addAll(suppressWarningsStrings);
            }
            newSuppressions.add(suppressedWarning);
          }
        }
      }
    }

    // Since this is invoked every time we descend into a new node, let's save some garbage
    // by returning the same instance if there were no changes.
    if (!anyModification) {
      return this;
    }

    return new SuppressionInfo(
        newSuppressions == null ? suppressWarningsStrings : newSuppressions.build(),
        newHasSuppressWarningsAll,
        newCustomSuppressions,
        newInGeneratedCode);
  }

  public enum SuppressedState {
    UNSUPPRESSED,
    SUPPRESSED
  }
}
