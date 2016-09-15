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

package com.google.errorprone.bugpatterns.inject;

import static com.google.errorprone.BugPattern.Category.INJECT;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_BINDING_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_SCOPE_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.JAVAX_QUALIFIER_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.JAVAX_SCOPE_ANNOTATION;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.sun.source.tree.Tree.Kind.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import java.lang.annotation.Retention;
import javax.annotation.Nullable;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@BugPattern(
  name = "InjectScopeOrQualifierAnnotationRetention",
  summary = "Scoping and qualifier annotations must have runtime retention.",
  explanation =
      "The JSR-330 spec allows use of reflection. Not having runtime "
          + "retention on scoping or qualifer annotations will cause unexpected "
          + "behavior in frameworks that use reflection.",
  category = INJECT,
  severity = ERROR,
  maturity = EXPERIMENTAL
)
public class ScopeOrQualifierAnnotationRetention extends BugChecker implements ClassTreeMatcher {

  private static final String RETENTION_ANNOTATION = "java.lang.annotation.Retention";

  /** Matches classes that are annotated with @Scope or @ScopeAnnotation. */
  private static final Matcher<ClassTree> SCOPE_OR_QUALIFIER_ANNOTATION_MATCHER =
      allOf(
          kindIs(ANNOTATION_TYPE),
          anyOf(
              hasAnnotation(GUICE_SCOPE_ANNOTATION),
              hasAnnotation(JAVAX_SCOPE_ANNOTATION),
              hasAnnotation(GUICE_BINDING_ANNOTATION),
              hasAnnotation(JAVAX_QUALIFIER_ANNOTATION)));

  @Override
  public final Description matchClass(ClassTree classTree, VisitorState state) {
    // TODO(glorioso): This is a poor hack to exclude android apps that are more likely to not have
    // reflective DI than normal java. JSR spec still says the annotations should be
    // runtime-retained, but this reduces the instances that are erroneously flagged.
    if (!state.isAndroidCompatible()
        && SCOPE_OR_QUALIFIER_ANNOTATION_MATCHER.matches(classTree, state)) {
      Retention retention = ASTHelpers.getAnnotation(classTree, Retention.class);
      if (retention != null && retention.value().equals(RUNTIME)) {
        return Description.NO_MATCH;
      }
      // Default retention is CLASS, not RUNTIME, so return true if retention == null
      return describe(classTree, state, retention);
    }
    return Description.NO_MATCH;
  }

  private Description describe(
      ClassTree classTree, VisitorState state, @Nullable Retention retention) {
    if (retention == null) {
      return describeMatch(
          classTree,
          SuggestedFix.builder()
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
    return describeMatch(
        retentionNode,
        SuggestedFix.builder()
            .addImport("java.lang.annotation.Retention")
            .addStaticImport("java.lang.annotation.RetentionPolicy.RUNTIME")
            .replace(retentionNode, "@Retention(RUNTIME)")
            .build());
  }
}
