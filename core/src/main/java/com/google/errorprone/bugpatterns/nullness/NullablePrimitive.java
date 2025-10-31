/*
 * Copyright 2015 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AnnotatedTypeTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import java.util.List;

/**
 * @author sebastian.h.monte@gmail.com (Sebastian Monte)
 */
@BugPattern(
    summary =
        "Nullness annotations should not be used for primitive types since they cannot be null",
    severity = WARNING,
    tags = StandardTags.STYLE)
public class NullablePrimitive extends BugChecker
    implements AnnotatedTypeTreeMatcher, VariableTreeMatcher, MethodTreeMatcher {

  @Override
  public Description matchAnnotatedType(AnnotatedTypeTree tree, VisitorState state) {
    Type type = ASTHelpers.getType(tree);
    return check(type, tree.getAnnotations());
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol sym = ASTHelpers.getSymbol(tree);
    return check(sym.getReturnType(), tree.getModifiers().getAnnotations());
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    return check(ASTHelpers.getSymbol(tree).type, tree.getModifiers().getAnnotations());
  }

  private Description check(Type type, List<? extends AnnotationTree> annotations) {
    if (type == null) {
      return NO_MATCH;
    }
    if (!type.isPrimitive()) {
      return NO_MATCH;
    }
    ImmutableList<AnnotationTree> annotationsRelevantToNullness =
        NullnessAnnotations.annotationsRelevantToNullness(annotations);
    if (annotationsRelevantToNullness.isEmpty()) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    annotationsRelevantToNullness.forEach(fix::delete);
    return describeMatch(annotationsRelevantToNullness.get(0), fix.build());
  }
}
