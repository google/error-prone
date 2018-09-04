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

  /**
   * Container for information about suppressions. Either reference field may be null, which
   * indicates that the suppression sets are unchanged.
   */
  public static class SuppressionInfo {
    public Set<String> suppressWarningsStrings;
    public Set<Class<? extends Annotation>> customSuppressions;
    public boolean inGeneratedCode;

    public SuppressionInfo(
        Set<String> suppressWarningsStrings,
        Set<Class<? extends Annotation>> customSuppressions,
        boolean inGeneratedCode) {
      this.suppressWarningsStrings = suppressWarningsStrings;
      this.customSuppressions = customSuppressions;
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
   * @param suppressionsOnCurrentPath The set of strings in all {@code @SuppressWarnings}
   *     annotations on the current path through the AST
   * @param customSuppressionsOnCurrentPath The set of all custom suppression annotations
   */
  public SuppressionInfo extendSuppressionSets(
      Symbol sym,
      Type suppressWarningsType,
      Set<String> suppressionsOnCurrentPath,
      Set<Class<? extends Annotation>> customSuppressionsOnCurrentPath,
      boolean inGeneratedCode,
      VisitorState state) {

    boolean newInGeneratedCode = inGeneratedCode || isGenerated(sym, state);

    /** Handle custom suppression annotations. */
    Set<Class<? extends Annotation>> newCustomSuppressions = null;
    for (Class<? extends Annotation> annotationType : customSuppressionAnnotations) {
      if (ASTHelpers.hasAnnotation(sym, annotationType, state)) {
        if (newCustomSuppressions == null) {
          newCustomSuppressions = new HashSet<>(customSuppressionsOnCurrentPath);
        }
        newCustomSuppressions.add(annotationType);
      }
    }

    /** Handle {@code @SuppressWarnings} and {@code @SuppressLint}. */
    Set<String> newSuppressions = null;
    // Iterate over annotations on this symbol, looking for SuppressWarnings
    for (Attribute.Compound attr : sym.getAnnotationMirrors()) {
      if ((attr.type.tsym == suppressWarningsType.tsym)
          || attr.type.tsym.getQualifiedName().contentEquals("android.annotation.SuppressLint")) {
        for (Pair<MethodSymbol, Attribute> value : attr.values) {
          if (value.fst.name.contentEquals("value"))
            if (value.snd
                instanceof Attribute.Array) { // SuppressWarnings/SuppressLint take an array
              for (Attribute suppress : ((Attribute.Array) value.snd).values) {
                if (newSuppressions == null) {
                  newSuppressions = new HashSet<>(suppressionsOnCurrentPath);
                }
                // TODO(eaftan): check return value to see if this was a new warning?
                newSuppressions.add((String) suppress.getValue());
              }
            } else {
              throw new RuntimeException(
                  "Expected SuppressWarnings/SuppressLint annotation to take array type");
            }
        }
      }
    }

    return new SuppressionInfo(newSuppressions, newCustomSuppressions, newInGeneratedCode);
  }

  /**
   * Returns true if this checker should be suppressed on the current tree path.
   *
   * @param suppressible Holds information about the suppressibilty of a checker
   * @param suppressionsOnCurrentPath The set of strings in all {@code @SuppressWarnings}
   *     annotations on the current path through the AST
   * @param customSuppressionsOnCurrentPath The set of all custom suppression annotations on the
   *     current path through the AST
   * @param severityLevel of the check to be suppressed
   * @param inGeneratedCode true if the current code is generated
   * @param disableWarningsInGeneratedCode true if warnings in generated code should be suppressed
   */
  public static boolean isSuppressed(
      Suppressible suppressible,
      Set<String> suppressionsOnCurrentPath,
      Set<Class<? extends Annotation>> customSuppressionsOnCurrentPath,
      SeverityLevel severityLevel,
      boolean inGeneratedCode,
      boolean disableWarningsInGeneratedCode) {
    if (inGeneratedCode && disableWarningsInGeneratedCode && severityLevel != SeverityLevel.ERROR) {
      return true;
    }
    if (suppressible.supportsSuppressWarnings()
        && !Collections.disjoint(suppressible.allNames(), suppressionsOnCurrentPath)) {
      return true;
    }
    return !Collections.disjoint(
        suppressible.customSuppressionAnnotations(), customSuppressionsOnCurrentPath);
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
