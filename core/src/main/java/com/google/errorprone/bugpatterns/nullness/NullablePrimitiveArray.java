/*
 * Copyright 2021 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "@Nullable type annotations should not be used for primitive types since they cannot be"
            + " null",
    severity = WARNING,
    tags = StandardTags.STYLE)
public class NullablePrimitiveArray extends BugChecker
    implements VariableTreeMatcher, MethodTreeMatcher {
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    return check(tree.getReturnType(), tree.getModifiers().getAnnotations(), state);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    return check(tree.getType(), tree.getModifiers().getAnnotations(), state);
  }

  // other cases of `@Nullable int[]` are covered by the existing NullablePrimitive
  private Description check(
      Tree typeTree, List<? extends AnnotationTree> allTreeAnnos, VisitorState state) {
    Type type = getType(typeTree);
    if (type == null) {
      return NO_MATCH;
    }
    if (!type.getKind().equals(TypeKind.ARRAY)) {
      return NO_MATCH;
    }
    while (type.getKind().equals(TypeKind.ARRAY)) {
      type = state.getTypes().elemtype(type);
    }
    if (!type.isPrimitive()) {
      return NO_MATCH;
    }
    ImmutableList<AnnotationTree> treeNullnessAnnos =
        NullnessAnnotations.annotationsRelevantToNullness(allTreeAnnos);
    if (treeNullnessAnnos.isEmpty()) {
      return NO_MATCH;
    }
    Symbol target = state.getSymtab().annotationTargetType.tsym;
    ImmutableList<AnnotationTree> typeNullnessAnnos =
        treeNullnessAnnos.stream()
            .filter(annotation -> isTypeAnnotation(getSymbol(annotation).attribute(target)))
            .collect(toImmutableList());
    if (typeNullnessAnnos.isEmpty()) {
      return NO_MATCH;
    }
    Tree dims = typeTree;
    while (dims instanceof ArrayTypeTree) {
      dims = ((ArrayTypeTree) dims).getType();
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    typeNullnessAnnos.forEach(fix::delete);
    boolean hasDeclarationNullnessAnno = typeNullnessAnnos.size() < treeNullnessAnnos.size();
    boolean hasTypeNullnessAnnoOnArray =
        dims instanceof AnnotatedTypeTree annotatedTypeTree
            && !NullnessAnnotations.annotationsRelevantToNullness(
                    annotatedTypeTree.getAnnotations())
                .isEmpty();
    if (!hasDeclarationNullnessAnno && !hasTypeNullnessAnnoOnArray) {
      fix.postfixWith(
          dims,
          typeNullnessAnnos.stream().map(state::getSourceForNode).collect(joining(" ", " ", " ")));
    }
    return describeMatch(typeNullnessAnnos.get(0), fix.build());
  }

  private static boolean isTypeAnnotation(Attribute.Compound attribute) {
    if (attribute == null) {
      return false;
    }
    Set<String> targets = new HashSet<>();
    Optional<Attribute> value = MoreAnnotations.getValue(attribute, "value");
    if (!value.isPresent()) {
      return false;
    }
    new SimpleAnnotationValueVisitor8<Void, Void>() {
      @Override
      public Void visitEnumConstant(VariableElement c, Void unused) {
        targets.add(c.getSimpleName().toString());
        return null;
      }

      @Override
      public Void visitArray(List<? extends AnnotationValue> list, Void unused) {
        list.forEach(x -> x.accept(this, null));
        return null;
      }
    }.visit(value.get(), null);
    for (String target : targets) {
      switch (target) {
        case "METHOD", "FIELD", "LOCAL_VARIABLE", "PARAMETER" -> {
          return false;
        }
        default -> {}
      }
    }
    return targets.contains("TYPE_USE");
  }
}
