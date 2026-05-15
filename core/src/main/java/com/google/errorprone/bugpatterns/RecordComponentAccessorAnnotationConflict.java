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

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasExplicitSource;
import static com.google.errorprone.util.ASTHelpers.isRecord;
import static com.google.errorprone.util.MoreAnnotations.asEnumValues;
import static com.google.errorprone.util.MoreAnnotations.getValue;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.RecordComponent;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.tree.JCTree;
import java.lang.annotation.ElementType;
import java.util.EnumSet;
import javax.lang.model.element.Name;

/** A BugPattern; see the summary. */
@BugPattern(summary = "Annotation on record component is ignored.", severity = WARNING)
public class RecordComponentAccessorAnnotationConflict extends BugChecker
    implements ClassTreeMatcher {

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ClassSymbol classSymbol = getSymbol(tree);
    if (classSymbol == null || !isRecord(classSymbol)) {
      return NO_MATCH;
    }

    ImmutableMap<Name, RecordComponent> components =
        classSymbol.getRecordComponents().stream()
            .collect(toImmutableMap(c -> c.getSimpleName(), c -> c));
    for (Tree member : tree.getMembers()) {
      if (!(member instanceof MethodTree methodTree)) {
        continue;
      }
      if (getSymbol(methodTree).isConstructor()) {
        for (VariableTree parameter : methodTree.getParameters()) {
          check(state, parameter, ElementType.PARAMETER, components);
        }
      }
      if (methodTree.getParameters().isEmpty()) {
        check(state, methodTree, ElementType.METHOD, components);
      }
    }
    return NO_MATCH;
  }

  private void check(
      VisitorState state,
      Tree tree,
      ElementType elementType,
      ImmutableMap<Name, RecordComponent> components) {
    if (!hasExplicitSource(tree, state)) {
      return;
    }
    Symbol symbol = getSymbol(tree);
    RecordComponent component = components.get(symbol.getSimpleName());
    if (component == null) {
      return;
    }
    for (JCTree.JCAnnotation annotation : component.getOriginalAnnos()) {
      TypeSymbol annotationElement = getType(annotation).tsym;
      if (!targetsOnly(annotationElement, elementType)) {
        continue;
      }
      if (symbol.getAnnotationMirrors().stream()
          .anyMatch(a -> a.getAnnotationType().asElement().equals(annotationElement))) {
        continue;
      }
      // The annotation doesn't have an end position, so we can't use
      // state.getSourceForNode.
      // See also https://bugs.openjdk.org/browse/JDK-8383786.
      @SuppressWarnings("TreeToString")
      String string = annotation.toString();
      state.reportMatch(
          buildDescription(tree)
              .setMessage(
                  switch (elementType) {
                    case METHOD ->
                        String.format(
                            "Annotation %s on record component %s with @Target(METHOD) is ignored"
                                + " when an explicit accessor is present.",
                            string, symbol);
                    case PARAMETER ->
                        String.format(
                            "Annotation %s on record component %s with @Target(PARAMETER) is"
                                + " ignored when an explicit constructor is present.",
                            string, symbol);
                    default -> message();
                  })
              .addFix(SuggestedFix.prefixWith(tree, string))
              .build());
    }
  }

  private static boolean targetsOnly(TypeSymbol annotation, ElementType elementType) {
    Attribute.Compound target = annotation.getAnnotationTypeMetadata().getTarget();
    if (target == null) {
      // If there's no explicit @Target, the annotation implicitly targets all declaration contexts
      return false;
    }
    return getValue(target, "value")
        .map(v -> asEnumValues(ElementType.class, v).equals(EnumSet.of(elementType)))
        .orElse(false);
  }
}
