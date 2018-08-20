/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.Category.JUNIT;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT_AFTER_CLASS_ANNOTATION;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT_BEFORE_CLASS_ANNOTATION;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isStatic;
import static com.google.errorprone.matchers.Matchers.isType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.matchers.MultiMatcher.MultiMatchResult;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

/** {@code @BeforeClass} or {@code @AfterClass} should be applied to static methods. */
@BugPattern(
    name = "JUnit4ClassAnnotationNonStatic",
    summary = "This method should be static",
    category = JUNIT,
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class JUnit4ClassAnnotationNonStatic extends BugChecker implements MethodTreeMatcher {

  private static final MultiMatcher<MethodTree, AnnotationTree> CLASS_INIT_ANNOTATION =
      annotations(
          AT_LEAST_ONE,
          anyOf(isType(JUNIT_AFTER_CLASS_ANNOTATION), isType(JUNIT_BEFORE_CLASS_ANNOTATION)));

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MultiMatchResult<AnnotationTree> matchResult =
        CLASS_INIT_ANNOTATION.multiMatchResult(tree, state);
    if (!matchResult.matches() || isStatic().matches(tree, state)) {
      return Description.NO_MATCH;
    }

    return buildDescription(tree)
        .setMessage(messageForAnnos(matchResult.matchingNodes()))
        .addFix(SuggestedFixes.addModifiers(tree, state, Modifier.STATIC))
        .build();
  }

  // Might be a bit overkill just in case people add @BeforeClass and @AfterClass to the same
  // method.
  private static String messageForAnnos(List<AnnotationTree> annotationTrees) {
    String annoNames =
        annotationTrees.stream()
            .map(a -> Signatures.prettyType(ASTHelpers.getType(a)))
            .collect(Collectors.joining(" and "));
    return annoNames + " can only be applied to static methods.";
  }
}
