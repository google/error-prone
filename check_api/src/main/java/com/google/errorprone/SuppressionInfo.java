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

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.hubspot.HubSpotUtils;
import com.google.errorprone.matchers.Suppressible;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Pair;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

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
public class SuppressionInfo {
  public static final SuppressionInfo EMPTY =
      new SuppressionInfo(ImmutableSet.of(), ImmutableSet.of(), false);

  private static final Supplier<Name> ANDROID_SUPPRESS_LINT =
      VisitorState.memoize(state -> state.getName("android.annotation.SuppressLint"));
  private static final Supplier<Name> VALUE = VisitorState.memoize(state -> state.getName("value"));
  private static final Supplier<ImmutableSet<Name>> GENERATED_ANNOTATIONS =
      VisitorState.memoize(
          state ->
              Stream.of("javax.annotation.Generated", "javax.annotation.processing.Generated")
                  .map(state::getName)
                  .collect(toImmutableSet()));
  private final ImmutableSet<String> suppressWarningsStrings;

  @SuppressWarnings("Immutable") /* Name is javac's interned version of a string. */
  private final ImmutableSet<Name> customSuppressions;

  private final boolean inGeneratedCode;

  private SuppressionInfo(
      Set<String> suppressWarningsStrings, Set<Name> customSuppressions, boolean inGeneratedCode) {
    this.suppressWarningsStrings = ImmutableSet.copyOf(suppressWarningsStrings);
    this.customSuppressions = ImmutableSet.copyOf(customSuppressions);
    this.inGeneratedCode = inGeneratedCode;
  }

  private static boolean isGenerated(Symbol sym, VisitorState state) {
    return !ASTHelpers.annotationsAmong(sym, GENERATED_ANNOTATIONS.get(state), state).isEmpty();
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

    if (HubSpotUtils.isCanonicalSuppressionEnabled(state)) {
      if (suppressible.supportsSuppressWarnings()
          && suppressWarningsStrings.contains(suppressible.canonicalName())) {
        return SuppressedState.SUPPRESSED;
      }
    } else {
      if (suppressible.supportsSuppressWarnings()
          && !Collections.disjoint(suppressible.allNames(), suppressWarningsStrings)) {
        return SuppressedState.SUPPRESSED;
      }
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
    if (HubSpotUtils.isGeneratedCodeInspectionEnabled(state)) {
      return new SuppressionInfo(suppressWarningsStrings, customSuppressions, HubSpotUtils.isGenerated(state));
    }

    AtomicBoolean generated = new AtomicBoolean(false);
    new SimpleTreeVisitor<Void, Void>() {
      @Override
      public Void visitClass(ClassTree node, Void unused) {
        ClassSymbol symbol = ASTHelpers.getSymbol(node);
        generated.compareAndSet(false, symbol != null && isGenerated(symbol, state));
        return null;
      }
    }.visit(tree.getTypeDecls(), null);
    return new SuppressionInfo(suppressWarningsStrings, customSuppressions, generated.get());
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
    boolean newInGeneratedCode;
    if (HubSpotUtils.isGeneratedCodeInspectionEnabled(state)) {
      newInGeneratedCode = inGeneratedCode || HubSpotUtils.isGenerated(state);
    } else {
      newInGeneratedCode = inGeneratedCode || isGenerated(sym, state);
    }

    boolean anyModification = newInGeneratedCode != inGeneratedCode;

    /* Handle custom suppression annotations. */
    Set<Name> lookingFor = new HashSet<>(customSuppressionAnnosToLookFor);
    lookingFor.removeAll(customSuppressions);
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
    Name suppressLint = ANDROID_SUPPRESS_LINT.get(state);
    Name valueName = VALUE.get(state);
    Set<String> newSuppressions = null;
    // Iterate over annotations on this symbol, looking for SuppressWarnings
    for (Attribute.Compound attr : sym.getAnnotationMirrors()) {
      if ((attr.type.tsym == state.getSymtab().suppressWarningsType.tsym)
          || attr.type.tsym.getQualifiedName().equals(suppressLint)) {
        for (Pair<MethodSymbol, Attribute> value : attr.values) {
          if (value.fst.name.equals(valueName)) {
            if (value.snd
                instanceof Attribute.Array) { // SuppressWarnings/SuppressLint take an array
              for (Attribute suppress : ((Attribute.Array) value.snd).values) {
                String suppressedWarning = (String) suppress.getValue();
                if (!suppressWarningsStrings.contains(suppressedWarning)) {
                  anyModification = true;
                  if (newSuppressions == null) {
                    newSuppressions = new HashSet<>(suppressWarningsStrings);
                  }
                  newSuppressions.add(suppressedWarning);
                }
              }
            } else {
              throw new RuntimeException(
                  "Expected SuppressWarnings/SuppressLint annotation to take array type");
            }
          }
        }
      }
    }

    // Since this is invoked every time we descend into a new node, let's save some garbage
    // by returning the same instance if there were no changes.
    if (!anyModification) {
      return this;
    }

    if (newSuppressions == null) {
      newSuppressions = suppressWarningsStrings;
    }
    return new SuppressionInfo(newSuppressions, newCustomSuppressions, newInGeneratedCode);
  }

  public enum SuppressedState {
    UNSUPPRESSED,
    SUPPRESSED
  }
}
