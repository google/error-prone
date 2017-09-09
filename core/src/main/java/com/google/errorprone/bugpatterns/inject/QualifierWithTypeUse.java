/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.inject;

import static com.google.errorprone.BugPattern.Category.INJECT;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.sun.source.tree.Tree.Kind.ANNOTATION_TYPE;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.InjectMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.matchers.MultiMatcher.MultiMatchResult;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** @author glorioso@google.com (Nick Glorioso) */
@BugPattern(
  name = "QualifierWithTypeUse",
  summary =
      "Injection frameworks currently don't understand Qualifiers in TYPE_PARAMETER or"
          + " TYPE_USE contexts.",
  category = INJECT,
  severity = WARNING,
  tags = StandardTags.FRAGILE_CODE,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class QualifierWithTypeUse extends BugChecker implements ClassTreeMatcher {

  private static final MultiMatcher<ClassTree, AnnotationTree> HAS_TARGET_ANNOTATION =
      annotations(AT_LEAST_ONE, isType("java.lang.annotation.Target"));

  private static final Matcher<ClassTree> IS_QUALIFIER_WITH_TARGET =
      allOf(
          kindIs(ANNOTATION_TYPE),
          anyOf(
              hasAnnotation(InjectMatchers.JAVAX_QUALIFIER_ANNOTATION),
              hasAnnotation(InjectMatchers.GUICE_BINDING_ANNOTATION)));

  private static final ImmutableSet<ElementType> FORBIDDEN_ELEMENT_TYPES =
      ImmutableSet.of(ElementType.TYPE_PARAMETER, ElementType.TYPE_USE);

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (IS_QUALIFIER_WITH_TARGET.matches(tree, state)) {
      MultiMatchResult<AnnotationTree> targetAnnotation =
          HAS_TARGET_ANNOTATION.multiMatchResult(tree, state);
      if (targetAnnotation.matches()) {
        AnnotationTree annotationTree = targetAnnotation.onlyMatchingNode();
        Target target = ASTHelpers.getAnnotation(tree, Target.class);
        if (hasTypeUseOrTypeParameter(target)) {
          return describeMatch(annotationTree, removeTypeUse(target, annotationTree));
        }
      }
    }
    return Description.NO_MATCH;
  }

  private boolean hasTypeUseOrTypeParameter(Target targetAnnotation) {
    // Should only be in cases where Target is not in the classpath
    return targetAnnotation != null
        && !Collections.disjoint(FORBIDDEN_ELEMENT_TYPES, Arrays.asList(targetAnnotation.value()));
  }

  private Fix removeTypeUse(Target targetAnnotation, AnnotationTree tree) {
    Set<ElementType> elements = EnumSet.copyOf(Arrays.asList(targetAnnotation.value()));
    elements.removeAll(FORBIDDEN_ELEMENT_TYPES);
    if (elements.isEmpty()) {
      return SuggestedFix.delete(tree);
    }
    return InvalidTargetingOnScopingAnnotation.replaceTargetAnnotation(tree, elements);
  }
}
