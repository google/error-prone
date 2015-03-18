/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CompileTimeConstant;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFix.Builder;
import com.google.errorprone.matchers.CompileTimeConstantExpressionMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;

import javax.lang.model.element.Modifier;

/**
 * Enforce that @CompileTimeConstant parameters are final.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(name = "NonFinalCompileTimeConstant",
    summary = "@CompileTimeConstant parameters should be final",
    explanation = "If a method's formal parameter is annotated with @CompileTimeConstant, the"
        + " method will always be invoked with an argument that is a static constant. If the"
        + " parameter itself is non-final, then it is a mutable reference to immutable data."
        + " This is rarely useful, and can be confusing when trying to use the parameter in a"
        + " context that requires an compile-time constant. For example:\n\n"
        + "    void f(@CompileTimeConstant y) {}\n"
        + "    void g(@CompileTimeConstant x) {\n"
        + "      f(x); // x is not a constant, did you mean to declare it as final?\n"
        + "    }\n\n",
    category = JDK, severity = ERROR, maturity = MATURE)
public class NonFinalCompileTimeConstant extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    Tree firstTree = null;
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    for (VariableTree parameter : tree.getParameters()) {
      VarSymbol sym = ASTHelpers.getSymbol(parameter);
      if (sym == null) {
        continue;
      }
      if (CompileTimeConstantExpressionMatcher.hasCompileTimeConstantAnnotation(state, sym)
          && (maybeFix(fixBuilder, parameter, sym) && firstTree == null)) {
        firstTree = parameter;
      }
    }
    if (fixBuilder.isEmpty()) {
      return Description.NO_MATCH;
    }
    return describeMatch(firstTree, fixBuilder.build());
  }

  /**
   * Adds the final modifier to the parameter if it is annotated with @CompileTimeConstant.
   */
  private boolean maybeFix(Builder fixBuilder, VariableTree parameter, VarSymbol sym) {
    if (sym.getModifiers().contains(Modifier.FINAL)) {
      return false;
    }
    for (AnnotationTree annotation : parameter.getModifiers().getAnnotations()) {
      Symbol annotationSym = ASTHelpers.getSymbol(annotation.getAnnotationType());
      if (annotationSym == null) {
        continue;
      }
      if (annotationSym.getSimpleName().toString().contentEquals(
              CompileTimeConstant.class.getSimpleName())) {
        fixBuilder.postfixWith(annotation, " final");
        return true;
      }
    }
    return false;
  }
}

