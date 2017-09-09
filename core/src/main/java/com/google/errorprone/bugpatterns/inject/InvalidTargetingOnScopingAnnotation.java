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

import static com.google.common.collect.Sets.immutableEnumSet;
import static com.google.errorprone.BugPattern.Category.INJECT;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_SCOPE_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.JAVAX_SCOPE_ANNOTATION;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.util.ASTHelpers.getAnnotation;
import static com.sun.source.tree.Tree.Kind.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFix.Builder;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.matchers.MultiMatcher.MultiMatchResult;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
@BugPattern(
  name = "InjectInvalidTargetingOnScopingAnnotation",
  summary = "A scoping annotation's Target should include TYPE and METHOD.",
  explanation =
      "`@Scope` annotations should be applicable to TYPE (annotating classes that should"
          + " be scoped) and to METHOD (annotating `@Provides` methods to apply scoping to the"
          + " returned object.\n\n"
          + " If an annotation's use is restricted by `@Target` and it doesn't include those two"
          + " element types, the annotation can't be used where it should be able to be used.",
  category = INJECT,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class InvalidTargetingOnScopingAnnotation extends BugChecker implements ClassTreeMatcher {

  private static final String TARGET_ANNOTATION = "java.lang.annotation.Target";

  private static final MultiMatcher<ClassTree, AnnotationTree> HAS_TARGET_ANNOTATION =
      annotations(AT_LEAST_ONE, isType(TARGET_ANNOTATION));

  private static final Matcher<ClassTree> ANNOTATION_WITH_SCOPE_AND_TARGET =
      allOf(
          kindIs(ANNOTATION_TYPE),
          anyOf(hasAnnotation(GUICE_SCOPE_ANNOTATION), hasAnnotation(JAVAX_SCOPE_ANNOTATION)));

  private static final ImmutableSet<ElementType> REQUIRED_ELEMENT_TYPES =
      immutableEnumSet(TYPE, METHOD);

  @Override
  public final Description matchClass(ClassTree classTree, VisitorState state) {
    if (ANNOTATION_WITH_SCOPE_AND_TARGET.matches(classTree, state)) {
      MultiMatchResult<AnnotationTree> targetAnnotation =
          HAS_TARGET_ANNOTATION.multiMatchResult(classTree, state);
      if (targetAnnotation.matches()) {
        AnnotationTree targetTree = targetAnnotation.onlyMatchingNode();
        Target target = getAnnotation(classTree, Target.class);
        if (target != null
            && // Unlikely to occur, but just in case Target isn't on the classpath.
            !Arrays.asList(target.value()).containsAll(REQUIRED_ELEMENT_TYPES)) {
          return describeMatch(targetTree, replaceTargetAnnotation(target, targetTree));
        }
      }
    }
    return Description.NO_MATCH;
  }

  /**
   * Rewrite the annotation with static imports, adding TYPE and METHOD to the @Target annotation
   * value (and reordering them to their declaration order in ElementType).
   */
  private static Fix replaceTargetAnnotation(
      Target annotation, AnnotationTree targetAnnotationTree) {
    Set<ElementType> types = EnumSet.copyOf(REQUIRED_ELEMENT_TYPES);
    types.addAll(Arrays.asList(annotation.value()));

    return replaceTargetAnnotation(targetAnnotationTree, types);
  }

  static Fix replaceTargetAnnotation(AnnotationTree targetAnnotationTree, Set<ElementType> types) {
    Builder builder =
        SuggestedFix.builder()
            .replace(targetAnnotationTree, "@Target({" + Joiner.on(", ").join(types) + "})");

    for (ElementType type : types) {
      builder.addStaticImport("java.lang.annotation.ElementType." + type);
    }

    return builder.build();
  }
}
