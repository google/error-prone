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

import static com.google.errorprone.BugPattern.Category.INJECT;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Flags;

import java.lang.annotation.Retention;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@BugPattern(name = "InjectScopeOrQualifierAnnotationRetention",
    summary = "Scoping and qualifier annotations must have runtime retention.", explanation =
        "The JSR-330 spec allows use of reflection. Not having runtime "
        + "retention on scoping or qualifer annotations will cause unexpected "
        + "behavior in frameworks that use reflection.", category = INJECT, severity = ERROR,
    maturity = EXPERIMENTAL)
public class InjectScopeOrQualifierAnnotationRetention extends BugChecker
    implements ClassTreeMatcher {

  private static final String GUICE_SCOPE_ANNOTATION = "com.google.inject.ScopeAnnotation";
  private static final String JAVAX_SCOPE_ANNOTATION = "javax.inject.Scope";
  private static final String GUICE_BINDING_ANNOTATION = "com.google.inject.BindingAnnotation";
  private static final String JAVAX_QUALIFER_ANNOTATION = "javax.inject.Qualifier";
  private static final String RETENTION_ANNOTATION = "java.lang.annotation.Retention";

  /**
   * Matches classes that are annotated with @Scope or @ScopeAnnotation.
   */
  private static final Matcher<ClassTree> SCOPE_OR_QUALIFIER_ANNOTATION_MATCHER = Matchers.<
      ClassTree>anyOf(hasAnnotation(GUICE_SCOPE_ANNOTATION), hasAnnotation(JAVAX_SCOPE_ANNOTATION),
      hasAnnotation(GUICE_BINDING_ANNOTATION), hasAnnotation(JAVAX_QUALIFER_ANNOTATION));

  @Override
  public final Description matchClass(ClassTree classTree, VisitorState state) {
    if ((ASTHelpers.getSymbol(classTree).flags() & Flags.ANNOTATION) != 0) {
      if (SCOPE_OR_QUALIFIER_ANNOTATION_MATCHER.matches(classTree, state)) {
        Retention retention = ASTHelpers.getAnnotation(classTree, Retention.class);
        if (retention != null && retention.value().equals(RUNTIME)) {
          return Description.NO_MATCH;
        }
        //Default retention is CLASS, not RUNTIME, so return true if retention == null
        return describe(classTree, state);
      }
    }
    return Description.NO_MATCH;
  }

  public Description describe(ClassTree classTree, VisitorState state) {
    Retention retention = ASTHelpers.getAnnotation(classTree, Retention.class);
    if (retention == null) {
      return describeMatch(classTree, SuggestedFix.builder()
          .addImport("java.lang.annotation.Retention")
          .addStaticImport("java.lang.annotation.RetentionPolicy.RUNTIME")
          .prefixWith(classTree, "@Retention(RUNTIME)\n")
          .build());
    }
    AnnotationTree retentionNode = null;
    for (AnnotationTree annotation : classTree.getModifiers().getAnnotations()) {
      if (ASTHelpers.getSymbol(annotation)
          .equals(state.getSymbolFromString(RETENTION_ANNOTATION))) {
        retentionNode = annotation;
      }
    }
    return describeMatch(retentionNode, SuggestedFix.builder()
        .addImport("java.lang.annotation.Retention")
        .addStaticImport("java.lang.annotation.RetentionPolicy.RUNTIME")
        .replace(retentionNode, "@Retention(RUNTIME)")
        .build());
  }
}
