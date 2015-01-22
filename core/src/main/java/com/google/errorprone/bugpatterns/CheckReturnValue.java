/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.enclosingPackage;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.TypeTag;

import javax.lang.model.element.ElementKind;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(name = "CheckReturnValue",
    altNames = {"ResultOfMethodCallIgnored", "ReturnValueIgnored"},
    summary = "Ignored return value of method that is annotated with @CheckReturnValue",
    explanation = "The JSR 305 @CheckReturnValue annotation marks methods whose return values "
        + "should be checked.  This error is triggered when one of these methods is called but "
        + "the result is not used.",
    category = JDK, severity = ERROR, maturity = MATURE)
public class CheckReturnValue extends AbstractReturnValueIgnored implements MethodTreeMatcher {
  
  private static final Matcher<MethodInvocationTree> MATCHER = 
      new Matcher<MethodInvocationTree>() {
        @Override
        public boolean matches(MethodInvocationTree tree, VisitorState state) {
          MethodSymbol method = ASTHelpers.getSymbol(tree);
          if (method.getReturnType().getTag() == TypeTag.VOID) {
            return false;
          }
          return hasAnnotation(method, javax.annotation.CheckReturnValue.class)
              || hasAnnotation(enclosingClass(method), javax.annotation.CheckReturnValue.class)
              || hasAnnotation(enclosingPackage(method), javax.annotation.CheckReturnValue.class);
        }
      };

  /**
   * Return a matcher for method invocations in which the method being called has the
   * @CheckReturnValue annotation.
   */
  @Override
  public Matcher<MethodInvocationTree> specializedMatcher() {
    return MATCHER;
  }

  /**
   * This enforces that {@link @CheckReturnValue} is not applied to void-returning method
   * declarations. Doing so makes no sense, because there is no return value to check.
   */
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol method = ASTHelpers.getSymbol(tree);
    if (!hasAnnotation(method, javax.annotation.CheckReturnValue.class)) {
      return Description.NO_MATCH;
    }
    if (method.getKind() != ElementKind.METHOD) {
      // skip contructors (which javac thinks are void-returning)
      return Description.NO_MATCH;
    }
    if (method.getReturnType().getTag() != TypeTag.VOID) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage("@CheckReturnValue may not be applied to void-returning methods")
        .build();
  }
}
