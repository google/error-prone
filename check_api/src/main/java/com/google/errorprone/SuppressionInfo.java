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

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.matchers.Suppressible;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.util.Pair;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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

  private static final ImmutableSet<String> GENERATED_ANNOTATIONS =
      ImmutableSet.of("javax.annotation.Generated", "javax.annotation.processing.Generated");

  private final ImmutableSet<String> suppressWarningsStrings;
  private final ImmutableSet<Class<? extends Annotation>> customSuppressions;
  private final boolean inGeneratedCode;

  private SuppressionInfo(
      Set<String> suppressWarningsStrings,
      Set<Class<? extends Annotation>> customSuppressions,
      boolean inGeneratedCode) {
    this.suppressWarningsStrings = ImmutableSet.copyOf(suppressWarningsStrings);
    this.customSuppressions = ImmutableSet.copyOf(customSuppressions);
    this.inGeneratedCode = inGeneratedCode;
  }

  private static boolean isGenerated(Symbol sym, VisitorState state) {
    for (String annotation : GENERATED_ANNOTATIONS) {
      if (ASTHelpers.hasAnnotation(sym, annotation, state)) {
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
   *     if the signals in this object say we're in generated code.
   */
  public SuppressedState suppressedState(
      Suppressible suppressible, boolean suppressedInGeneratedCode) {
    if (inGeneratedCode && suppressedInGeneratedCode) {
      return SuppressedState.SUPPRESSED;
    }
    if (suppressible.supportsSuppressWarnings()
        && !Collections.disjoint(suppressible.allNames(), suppressWarningsStrings)) {
      return SuppressedState.SUPPRESSED;
    }
    if (!Collections.disjoint(suppressible.customSuppressionAnnotations(), customSuppressions)) {
      return SuppressedState.SUPPRESSED;
    }

    return SuppressedState.UNSUPPRESSED;
  }

  /**
   * Returns true if {@code name} is a suppressed name according to the suppression signals in this
   * object (namely, is a member of previously-seen {@code SuppressWarnings} or {@code SuppressLint}
   * annotation.
   */
  public boolean isNameSuppressed(String name) {
    return suppressWarningsStrings.contains(name);
  }

  /**
   * Generates the {@link SuppressionInfo} for a {@link CompilationUnitTree}. This differs in that
   * {@code isGenerated} is determined by inspecting the annotations of the outermost class so that
   * matchers on {@link CompilationUnitTree} will also be suppressed.
   */
  public SuppressionInfo forCompilationUnit(CompilationUnitTree tree, VisitorState state) {
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
      Symbol sym,
      VisitorState state,
      Set<Class<? extends Annotation>> customSuppressionAnnosToLookFor) {
    boolean newInGeneratedCode = inGeneratedCode || isGenerated(sym, state);
    boolean anyModification = newInGeneratedCode != inGeneratedCode;

    /* Handle custom suppression annotations. */
    Set<Class<? extends Annotation>> newCustomSuppressions = null;
    for (Class<? extends Annotation> annotationType : customSuppressionAnnosToLookFor) {
      // Don't need to check already-suppressed annos
      if (customSuppressions.contains(annotationType)) {
        continue;
      }
      if (ASTHelpers.hasAnnotation(sym, annotationType, state)) {
        anyModification = true;
        if (newCustomSuppressions == null) {
          newCustomSuppressions = new HashSet<>(customSuppressions);
        }
        newCustomSuppressions.add(annotationType);
      }
    }

    /* Handle {@code @SuppressWarnings} and {@code @SuppressLint}. */
    Set<String> newSuppressions = null;
    // Iterate over annotations on this symbol, looking for SuppressWarnings
    for (Attribute.Compound attr : sym.getAnnotationMirrors()) {
      if ((attr.type.tsym == state.getSymtab().suppressWarningsType.tsym)
          || attr.type.tsym.getQualifiedName().contentEquals("android.annotation.SuppressLint")) {
        for (Pair<MethodSymbol, Attribute> value : attr.values) {
          if (value.fst.name.contentEquals("value")) {
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

    if (newCustomSuppressions == null) {
      newCustomSuppressions = customSuppressions;
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
