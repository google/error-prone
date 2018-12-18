/*
 * Copyright 2014 The Error Prone Authors.
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
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.matchers.Suppressible;
import com.google.errorprone.util.ASTHelpers;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Pair;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Encapsulates the logic of handling suppressions, both via {@code @SuppressWarnings} and via
 * custom suppression annotations. To do this we have to maintains 2 sets of suppression info:
 *
 * <ol>
 *   <li>A set of the suppression strings in all {@code @SuppressWarnings} annotations down this
 *       path of the AST.
 *   <li>A set of all custom suppression annotations down this path of the AST.
 * </ol>
 */
public class SuppressionHelper {

  private static final ImmutableSet<String> GENERATED_ANNOTATIONS =
      ImmutableSet.of("javax.annotation.Generated", "javax.annotation.processing.Generated");

  /** The set of custom suppression annotations that this SuppressionHelper should look for. */
  private final Set<Class<? extends Annotation>> customSuppressionAnnotations;

  /**
   * @param customSuppressionAnnotations The set of custom suppression annotations that this
   *     SuppressionHelper should look for.
   */
  public SuppressionHelper(Set<Class<? extends Annotation>> customSuppressionAnnotations) {
    if (customSuppressionAnnotations == null) {
      throw new IllegalArgumentException("customSuppressionAnnotations must be non-null");
    }
    this.customSuppressionAnnotations = customSuppressionAnnotations;
  }

  /** Immutable container for information about currently-known-about suppressions. */
  @Immutable
  public static class SuppressionInfo {
    public static final SuppressionInfo EMPTY =
        new SuppressionInfo(ImmutableSet.of(), ImmutableSet.of(), false);

    public final ImmutableSet<String> suppressWarningsStrings;
    public final ImmutableSet<Class<? extends Annotation>> customSuppressions;
    public final boolean inGeneratedCode;

    private SuppressionInfo(
        Set<String> suppressWarningsStrings,
        Set<Class<? extends Annotation>> customSuppressions,
        boolean inGeneratedCode) {
      this.suppressWarningsStrings = ImmutableSet.copyOf(suppressWarningsStrings);
      this.customSuppressions = ImmutableSet.copyOf(customSuppressions);
      this.inGeneratedCode = inGeneratedCode;
    }
  }

  /**
   * Extend suppression sets for both {@code @SuppressWarnings} and custom suppression annotations.
   * When we explore a new node, we have to extend the suppression sets with any new suppressed
   * warnings or custom suppression annotations. We also have to retain the previous suppression set
   * so that we can reinstate it when we move up the tree.
   *
   * <p>We do not modify the existing suppression sets, so they can be restored when moving up the
   * tree. We also avoid copying the suppression sets if the next node to explore does not have any
   * suppressed warnings or custom suppression annotations. This is the common case.
   *
   * @param sym The {@code Symbol} for the AST node currently being scanned
   * @param suppressWarningsType The {@code Type} for {@code @SuppressWarnings}, as given by javac's
   *     symbol table
   * @param toExtend The suppression info to extend from.
   * @param state VisitorState for checking the current tree
   */
  public SuppressionInfo extendSuppressionSets(
      Symbol sym, Type suppressWarningsType, SuppressionInfo toExtend, VisitorState state) {
    boolean newInGeneratedCode = toExtend.inGeneratedCode || isGenerated(sym, state);
    boolean anyModification = newInGeneratedCode != toExtend.inGeneratedCode;

    /* Handle custom suppression annotations. */
    Set<Class<? extends Annotation>> newCustomSuppressions = null;
    for (Class<? extends Annotation> annotationType : customSuppressionAnnotations) {
      // Don't need to check already-suppressed annos
      if (toExtend.customSuppressions.contains(annotationType)) {
        continue;
      }
      if (ASTHelpers.hasAnnotation(sym, annotationType, state)) {
        anyModification = true;
        if (newCustomSuppressions == null) {
          newCustomSuppressions = new HashSet<>(toExtend.customSuppressions);
        }
        newCustomSuppressions.add(annotationType);
      }
    }

    /* Handle {@code @SuppressWarnings} and {@code @SuppressLint}. */
    Set<String> newSuppressions = null;
    // Iterate over annotations on this symbol, looking for SuppressWarnings
    for (Attribute.Compound attr : sym.getAnnotationMirrors()) {
      if ((attr.type.tsym == suppressWarningsType.tsym)
          || attr.type.tsym.getQualifiedName().contentEquals("android.annotation.SuppressLint")) {
        for (Pair<MethodSymbol, Attribute> value : attr.values) {
          if (value.fst.name.contentEquals("value")) {
            if (value.snd
                instanceof Attribute.Array) { // SuppressWarnings/SuppressLint take an array
              for (Attribute suppress : ((Attribute.Array) value.snd).values) {
                String suppressedWarning = (String) suppress.getValue();
                if (!toExtend.suppressWarningsStrings.contains(suppressedWarning)) {
                  anyModification = true;
                  if (newSuppressions == null) {
                    newSuppressions = new HashSet<>(toExtend.suppressWarningsStrings);
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
      return toExtend;
    }

    if (newCustomSuppressions == null) {
      newCustomSuppressions = toExtend.customSuppressions;
    }
    if (newSuppressions == null) {
      newSuppressions = toExtend.suppressWarningsStrings;
    }
    return new SuppressionInfo(newSuppressions, newCustomSuppressions, newInGeneratedCode);
  }

  /**
   * Returns true if this checker should be suppressed on the current tree path.
   *
   * @param suppressible Holds information about the suppressibilty of a checker
   * @param severityLevel of the check to be suppressed
   * @param suppressionInfo The current set of suppressions.
   * @param disableWarningsInGeneratedCode true if warnings in generated code should be suppressed
   */
  public static boolean isSuppressed(
      Suppressible suppressible,
      SeverityLevel severityLevel,
      SuppressionInfo suppressionInfo,
      boolean disableWarningsInGeneratedCode) {

    if (suppressionInfo.inGeneratedCode
        && disableWarningsInGeneratedCode
        && severityLevel != SeverityLevel.ERROR) {
      return true;
    }
    if (suppressible.supportsSuppressWarnings()
        && !Collections.disjoint(
            suppressible.allNames(), suppressionInfo.suppressWarningsStrings)) {
      return true;
    }
    return !Collections.disjoint(
        suppressible.customSuppressionAnnotations(), suppressionInfo.customSuppressions);
  }

  private static boolean isGenerated(Symbol sym, VisitorState state) {
    for (String annotation : GENERATED_ANNOTATIONS) {
      if (ASTHelpers.hasAnnotation(sym, annotation, state)) {
        return true;
      }
    }
    return false;
  }
}
