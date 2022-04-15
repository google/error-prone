/*
 * Copyright 2022 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.LIKELY_ERROR;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "The Refaster template contains a method without any Refaster annotations",
    severity = WARNING,
    tags = LIKELY_ERROR)
public final class MissingRefasterAnnotation extends BugChecker implements ClassTreeMatcher {
  private static final MultiMatcher<Tree, AnnotationTree> HAS_REFASTER_ANNOTATION =
      annotations(
          AT_LEAST_ONE,
          anyOf(
              isType("com.google.errorprone.refaster.annotation.Placeholder"),
              isType("com.google.errorprone.refaster.annotation.BeforeTemplate"),
              isType("com.google.errorprone.refaster.annotation.AfterTemplate")));

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    long methodTypeCount =
        tree.getMembers().stream()
            .filter(member -> member.getKind() == Tree.Kind.METHOD)
            .map(MethodTree.class::cast)
            .filter(method -> !ASTHelpers.isGeneratedConstructor(method))
            .map(method -> HAS_REFASTER_ANNOTATION.matches(method, state))
            .distinct()
            .count();

    return methodTypeCount < 2 ? Description.NO_MATCH : buildDescription(tree).build();
  }
}
