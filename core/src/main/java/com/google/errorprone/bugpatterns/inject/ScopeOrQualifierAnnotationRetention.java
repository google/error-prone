/*
 * Copyright 2013 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.inject.ElementPredicates.doesNotHaveRuntimeRetention;
import static com.google.errorprone.bugpatterns.inject.ElementPredicates.hasSourceRetention;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_BINDING_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_SCOPE_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.JAVAX_QUALIFIER_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.JAVAX_SCOPE_ANNOTATION;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.sun.source.tree.Tree.Kind.ANNOTATION_TYPE;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.InjectMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.lang.annotation.Retention;
import javax.annotation.Nullable;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
@BugPattern(
    name = "InjectScopeOrQualifierAnnotationRetention",
    summary = "Scoping and qualifier annotations must have runtime retention.",
    severity = ERROR)
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
    if (SCOPE_OR_QUALIFIER_ANNOTATION_MATCHER.matches(classTree, state)) {
      ClassSymbol classSymbol = ASTHelpers.getSymbol(classTree);
      if (hasSourceRetention(classSymbol)) {
        return describe(classTree, state, ASTHelpers.getAnnotation(classSymbol, Retention.class));
      }

      // TODO(glorioso): This is a poor hack to exclude android apps that are more likely to not
      // have reflective DI than normal java. JSR spec still says the annotations should be
      // runtime-retained, but this reduces the instances that are flagged.
      if (!state.isAndroidCompatible() && doesNotHaveRuntimeRetention(classSymbol)) {
        // Is this in a dagger component?
        ClassTree outer = ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class);
        if (outer != null
            && allOf(
                    InjectMatchers.IS_DAGGER_COMPONENT_OR_MODULE)
                .matches(outer, state)) {
          return Description.NO_MATCH;
        }
        return describe(classTree, state, ASTHelpers.getAnnotation(classSymbol, Retention.class));
      }
    }
    return Description.NO_MATCH;
  }

  private Description describe(
      ClassTree classTree, VisitorState state, @Nullable Retention retention) {
    if (retention == null) {
      AnnotationTree annotation = Iterables.getLast(classTree.getModifiers().getAnnotations());
      return describeMatch(
          classTree,
          SuggestedFix.builder()
              .addImport("java.lang.annotation.Retention")
              .addStaticImport("java.lang.annotation.RetentionPolicy.RUNTIME")
              .postfixWith(annotation, "@Retention(RUNTIME)")
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
