/*
 * Copyright 2026 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getAnnotationMirror;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;

/** Recommends using @TestParameter for enums and booleans instead of exhaustive @TestParameters. */
@BugPattern(
    summary =
        "When exhaustively testing all values of a single enum or boolean parameter, prefer"
            + " @TestParameter over @TestParameters.",
    severity = WARNING)
public final class PreferTestParameter extends BugChecker implements MethodTreeMatcher {

  private static final String TEST_PARAMETER =
      "com.google.testing.junit.testparameterinjector.TestParameter";
  private static final Supplier<Type> TEST_PARAMETERS_TYPE =
      Suppliers.typeFromString("com.google.testing.junit.testparameterinjector.TestParameters");

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (tree.getParameters().size() != 1) {
      return NO_MATCH;
    }

    VariableTree paramTree = getOnlyElement(tree.getParameters());
    VarSymbol paramSym = getSymbol(paramTree);

    // Check if the parameter itself already has @TestParameter, if so skip.
    if (hasAnnotation(paramSym, TEST_PARAMETER, state)) {
      return NO_MATCH;
    }

    Type paramType = getType(paramTree);
    if (paramType == null) {
      return NO_MATCH;
    }

    List<String> expectedConstants = new ArrayList<>();
    if (paramType.tsym != null && paramType.tsym.getKind() == ElementKind.ENUM) {
      ClassSymbol enumSym = (ClassSymbol) paramType.tsym;
      for (Symbol sym : enumSym.members().getSymbols()) {
        if (sym.getKind() == ElementKind.ENUM_CONSTANT) {
          expectedConstants.add(sym.getSimpleName().toString());
        }
      }
    } else if (state.getTypes().unboxedTypeOrType(paramType).getKind() == TypeKind.BOOLEAN) {
      expectedConstants.add("true");
      expectedConstants.add("false");
    } else {
      return NO_MATCH;
    }

    if (expectedConstants.isEmpty()) {
      return NO_MATCH;
    }

    List<String> values = new ArrayList<>();
    List<AnnotationTree> annotationsToRemove = new ArrayList<>();

    for (AnnotationTree annotationTree : tree.getModifiers().getAnnotations()) {
      Attribute.Compound attribute = (Attribute.Compound) getAnnotationMirror(annotationTree);
      if (attribute == null) {
        continue;
      }
      if (isSameType(attribute.type, TEST_PARAMETERS_TYPE.get(state), state)) {
        if (MoreAnnotations.getValue(attribute, "customName").isPresent()) {
          return NO_MATCH;
        }
        MoreAnnotations.getValue(attribute, "value")
            .ifPresent(v -> MoreAnnotations.asStrings(v).forEach(values::add));
        annotationsToRemove.add(annotationTree);
      }
    }

    // Values may be {mode: FOO} so we just check if every enum constant is mentioned in the values.
    // Ensure all enum constants are represented.
    // Use regex word boundaries to avoid false positives with subset enum values
    // (e.g., matching "OPEN" inside "{mode: OPEN_PENDING}").
    boolean allPresent =
        expectedConstants.stream()
            .allMatch(
                constant -> {
                  Pattern pattern = Pattern.compile("\\b" + Pattern.quote(constant) + "\\b");
                  return values.stream().anyMatch(val -> pattern.matcher(val).find());
                });

    // Also ensure that the size matches exactly so we aren't suggesting if they are testing
    // combinations or missing one, etc.
    if (!allPresent || values.size() != expectedConstants.size()) {
      return NO_MATCH;
    }

    SuggestedFix.Builder fix = SuggestedFix.builder();
    for (AnnotationTree annotationToRemove : annotationsToRemove) {
      fix.delete(annotationToRemove);
    }

    fix.prefixWith(
        paramTree, String.format("@%s ", SuggestedFixes.qualifyType(state, fix, TEST_PARAMETER)));

    return describeMatch(tree, fix.build());
  }
}
