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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.prettyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.MAIN_METHOD;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.hasOverloadWithOnlyOneParameter;
import static com.google.errorprone.util.ASTHelpers.methodIsPublicAndNotAnOverride;
import static java.util.function.Predicate.isEqual;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import javax.lang.model.element.ElementKind;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Object arrays are inferior to collections in almost every way. Prefer immutable"
            + " collections (e.g., ImmutableSet, ImmutableList, etc.) over an object array whenever"
            + " possible.",
    severity = WARNING)
public class AvoidObjectArrays extends BugChecker implements MethodTreeMatcher {
  private static final ImmutableSet<String> ANNOTATIONS_TO_IGNORE =
      ImmutableSet.of(
          "com.tngtech.java.junit.dataprovider.DataProvider",
          "org.junit.runners.Parameterized.Parameters",
          "org.junit.experimental.theories.DataPoints",
          "junitparams.Parameters");

  @Override
  public Description matchMethod(MethodTree method, VisitorState state) {
    if (shouldApplyApiChecks(method, state)) {
      // check the return type
      if (isObjectArray(method.getReturnType())) {
        state.reportMatch(
            createDescription(method.getReturnType(), state, "returning", "ImmutableList"));
      }

      MethodSymbol methodSymbol = getSymbol(method);

      // check each method parameter
      for (int i = 0; i < method.getParameters().size(); i++) {
        VariableTree varTree = method.getParameters().get(i);
        if (isObjectArray(varTree)) {
          // we allow an Object[] param if it's the last parameter and it's var args
          if (methodSymbol.isVarArgs() && i == method.getParameters().size() - 1) {
            continue;
          }

          // we allow an Object[] param if there's also an overload with an Iterable param
          if (hasOverloadWithOnlyOneParameter(
              methodSymbol, methodSymbol.name, state.getSymtab().iterableType, state)) {
            continue;
          }

          // we _may_ want to eventually allow `String[] args` as a method param, since folks
          // sometimes pass around command-line arguments to other methods
          state.reportMatch(createDescription(varTree.getType(), state, "accepting", "Iterable"));
        }
      }
    }
    return NO_MATCH;
  }

  private Description createDescription(
      Tree tree, VisitorState state, String verb, String newType) {
    Type type = getType(tree);

    String message = String.format("Avoid %s a %s", verb, prettyType(type, state));

    if (type instanceof ArrayType) {
      type = ((ArrayType) type).getComponentType();
      boolean isMultiDimensional = (type instanceof ArrayType);
      if (!isMultiDimensional) {
        message += String.format("; consider an %s<%s> instead", newType, prettyType(type, state));
      }
    }

    return buildDescription(tree).setMessage(message).build();
  }

  /** Returns whether the tree represents an object array or not. */
  private static boolean isObjectArray(Tree tree) {
    Type type = getType(tree);
    return (type instanceof ArrayType) && !((ArrayType) type).getComponentType().isPrimitive();
  }

  private static boolean shouldApplyApiChecks(MethodTree methodTree, VisitorState state) {
    // don't match main methods
    if (MAIN_METHOD.matches(methodTree, state)) {
      return false;
    }

    // don't match certain framework-y APIs like parameterized test data providers
    for (String annotationName : ANNOTATIONS_TO_IGNORE) {
      if (hasAnnotation(methodTree, annotationName, state)) {
        return false;
      }
    }

    MethodSymbol method = getSymbol(methodTree);

    // don't match methods inside an annotation type
    if (state.getTypes().closure(method.owner.type).stream()
        .map(superType -> superType.tsym.getKind())
        .anyMatch(isEqual(ElementKind.ANNOTATION_TYPE))) {
      return false;
    }

    return methodIsPublicAndNotAnOverride(method, state);
  }
}
