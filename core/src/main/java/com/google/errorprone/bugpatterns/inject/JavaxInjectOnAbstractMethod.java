/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.fixes.SuggestedFix.delete;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.InjectMatchers.IS_APPLICATION_OF_JAVAX_INJECT;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.DEFAULT;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.matchers.MultiMatcher.MultiMatchResult;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
@BugPattern(
  name = "JavaxInjectOnAbstractMethod",
  summary = "Abstract and default methods are not injectable with javax.inject.Inject",
  category = INJECT,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class JavaxInjectOnAbstractMethod extends BugChecker implements MethodTreeMatcher {
  private static final MultiMatcher<MethodTree, AnnotationTree> INJECT_FINDER =
      annotations(AT_LEAST_ONE, IS_APPLICATION_OF_JAVAX_INJECT);

  private static final Matcher<MethodTree> ABSTRACT_OR_DEFAULT_METHOD_WITH_INJECT =
      allOf(anyOf(hasModifier(ABSTRACT), hasModifier(DEFAULT)));

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    if (ABSTRACT_OR_DEFAULT_METHOD_WITH_INJECT.matches(methodTree, state)) {
      MultiMatchResult<AnnotationTree> injectAnnotations =
          INJECT_FINDER.multiMatchResult(methodTree, state);
      if (injectAnnotations.matches()) {
        AnnotationTree injectAnnotation = injectAnnotations.onlyMatchingNode();
        return describeMatch(injectAnnotation, delete(injectAnnotation));
      }
    }
    return Description.NO_MATCH;
  }
}
