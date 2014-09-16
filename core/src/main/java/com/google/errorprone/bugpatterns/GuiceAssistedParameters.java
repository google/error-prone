/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.GUICE;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

import javax.lang.model.element.TypeElement;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@BugPattern(name = "GuiceAssistedParameters", summary =
    "A constructor cannot have two @Assisted parameters of the same type unless they are "
    + "disambiguated with named @Assisted annotations. ",
    explanation = "See http://google-guice.googlecode.com/git/javadoc/com/google/inject/assistedinject/FactoryModuleBuilder.html",
    category = GUICE, severity = ERROR, maturity = EXPERIMENTAL)
public class GuiceAssistedParameters extends BugChecker implements VariableTreeMatcher {

  private static final String ASSISTED_ANNOTATION = "com.google.inject.assistedinject.Assisted";

  private Matcher<VariableTree> constructorAssistedParameterMatcher = new Matcher<VariableTree>() {
    @Override
    public boolean matches(VariableTree t, VisitorState state) {
      Symbol modified = ASTHelpers.getSymbol(state.getPath().getParentPath().getLeaf());
      return modified != null && modified.isConstructor()
          && Matchers.<VariableTree>hasAnnotation(ASSISTED_ANNOTATION).matches(t, state);
    }
  };

  @Override
  public final Description matchVariable(VariableTree variableTree, VisitorState state) {
    if (constructorAssistedParameterMatcher.matches(variableTree, state)) {
      Compound thisParamsAssisted = null;
      for (Compound c : ASTHelpers.getSymbol(variableTree).getAnnotationMirrors()) {
        if (((TypeElement) c.getAnnotationType().asElement()).getQualifiedName()
            .contentEquals(ASSISTED_ANNOTATION)) {
          thisParamsAssisted = c;
        }
      }
      MethodTree enclosingMethod = (MethodTree) state.getPath().getParentPath().getLeaf();
      // count the number of parameters of this type and value. One is expected since we
      // will be iterating through all parameters including the one we're matching.
      int numIdentical = 0;
      for (VariableTree parameter : enclosingMethod.getParameters()) {
        if (Matchers.<VariableTree>isSameType(variableTree).matches(parameter, state)) {
          Compound otherParamsAssisted = null;
          for (Compound c : ASTHelpers.getSymbol(parameter).getAnnotationMirrors()) {
            if (((TypeElement) c.getAnnotationType().asElement()).getQualifiedName()
                .contentEquals(ASSISTED_ANNOTATION)) {
              otherParamsAssisted = c;
            }
          }
          if (otherParamsAssisted != null) {
            if (thisParamsAssisted.getElementValues().isEmpty()
                && otherParamsAssisted.getElementValues().isEmpty()) {
              //both have unnamed @Assisted annotations
              numIdentical++;
            }
            //if value is specified, check that they are equal
            //also, there can only be one value which is why I didn't check for equality
            //in both directions
            for (MethodSymbol m : thisParamsAssisted.getElementValues().keySet())
              if (otherParamsAssisted.getElementValues().get(m).getValue()
                  .equals(thisParamsAssisted.getElementValues().get(m).getValue())) {
                numIdentical++;
              }
          }
          if (numIdentical > 1) {
            return describe(variableTree, state);
          }
        }
      }
    }
    return Description.NO_MATCH;
  }

  public Description describe(VariableTree variableTree, VisitorState state) {
    // find the @Assisted annotation to put the error on
    for (AnnotationTree annotation : variableTree.getModifiers().getAnnotations()) {
      if (ASTHelpers.getSymbol(annotation).equals(state.getSymbolFromString(ASSISTED_ANNOTATION))) {
        return describeMatch(annotation, SuggestedFix.delete(annotation));
      }
    }
    throw new IllegalStateException("Expected to find @Assisted on this parameter");
  }
}
