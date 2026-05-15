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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.inject.ElementPredicates.doesNotHaveRuntimeRetention;
import static com.google.errorprone.bugpatterns.inject.ElementPredicates.hasSourceRetention;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.InjectMatchers.DAGGER_MAP_KEY_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_MAP_KEY_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.QUALIFIER_ANNOTATIONS;
import static com.google.errorprone.matchers.InjectMatchers.SCOPE_ANNOTATIONS;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.util.ASTHelpers.annotationsAmong;
import static com.google.errorprone.util.ASTHelpers.getDeclaredSymbol;
import static com.sun.source.tree.Tree.Kind.ANNOTATION_TYPE;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.InjectMatchers;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.util.Name;
import java.lang.annotation.Retention;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@BugPattern(
    name = "MissingRuntimeRetention",
    altNames = "InjectScopeOrQualifierAnnotationRetention",
    summary = "Scoping and qualifier annotations must have runtime retention.",
    severity = ERROR)
public class MissingRuntimeRetention extends BugChecker implements ClassTreeMatcher {

  private static final String RETENTION_ANNOTATION = "java.lang.annotation.Retention";

  private static final Supplier<ImmutableSet<Name>> INJECT_ANNOTATIONS =
      VisitorState.memoize(
          state ->
              Streams.concat(
                      SCOPE_ANNOTATIONS.stream(),
                      QUALIFIER_ANNOTATIONS.stream(),
                      Stream.of(GUICE_MAP_KEY_ANNOTATION, DAGGER_MAP_KEY_ANNOTATION))
                  .map(state::binaryNameFromClassname)
                  .collect(toImmutableSet()));

  private static final Supplier<ImmutableSet<Name>> ANNOTATIONS =
      VisitorState.memoize(
          state ->
              Streams.concat(
                      Stream.of("com.google.apps.framework.annotations.ProcessorAnnotation")
                          .map(state::binaryNameFromClassname),
                      INJECT_ANNOTATIONS.get(state).stream())
                  .collect(toImmutableSet()));

  @Override
  public final Description matchClass(ClassTree classTree, VisitorState state) {
    if (!classTree.getKind().equals(ANNOTATION_TYPE)) {
      return NO_MATCH;
    }
    Set<Name> annotations =
        annotationsAmong(getDeclaredSymbol(classTree), ANNOTATIONS.get(state), state);
    if (annotations.isEmpty()) {
      return NO_MATCH;
    }
    ClassSymbol classSymbol = ASTHelpers.getSymbol(classTree);
    if (!doesNotHaveRuntimeRetention(classSymbol)) {
      return NO_MATCH;
    }
    if (!Collections.disjoint(annotations, INJECT_ANNOTATIONS.get(state))
        && exemptInjectAnnotation(state, classSymbol)) {
      return NO_MATCH;
    }
    return describe(classTree, state, ASTHelpers.getAnnotation(classSymbol, Retention.class));
  }

  private static boolean exemptInjectAnnotation(VisitorState state, ClassSymbol classSymbol) {
    // TODO(glorioso): This is a poor hack to exclude android apps that are more likely to not
    // have reflective DI than normal java. JSR spec still says the annotations should be
    // runtime-retained, but this reduces the instances that are flagged.
    if (hasSourceRetention(classSymbol)) {
      return false;
    }
    if (state.isAndroidCompatible()) {
      return true;
    }
    // Is this in a dagger component?
    ClassTree outer = ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class);
    if (outer != null
        && allOf(InjectMatchers.IS_DAGGER_COMPONENT_OR_MODULE).matches(outer, state)) {
      return true;
    }
    return false;
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
      if (ASTHelpers.getSymbol(annotation).equals(JAVA_LANG_ANNOTATION_RETENTION.get(state))) {
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

  private static final Supplier<Symbol> JAVA_LANG_ANNOTATION_RETENTION =
      VisitorState.memoize(state -> state.getSymbolFromString(RETENTION_ANNOTATION));
}
