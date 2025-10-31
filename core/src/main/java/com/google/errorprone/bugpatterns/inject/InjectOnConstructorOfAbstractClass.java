/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFix.delete;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_INJECT_ANNOTATION;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.matchers.Matchers.methodIsConstructor;
import static javax.lang.model.element.Modifier.ABSTRACT;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.InjectMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.matchers.MultiMatcher.MultiMatchResult;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;

/**
 * @author glorioso@google.com (Nick Glorioso)
 */
@BugPattern(
    summary =
        "Constructors on abstract classes are never directly @Inject'ed, only the constructors"
            + " of their subclasses can be @Inject'ed.",
    severity = WARNING)
public class InjectOnConstructorOfAbstractClass extends BugChecker implements MethodTreeMatcher {

  private static final MultiMatcher<MethodTree, AnnotationTree> INJECT_FINDER =
      annotations(
          AT_LEAST_ONE,
          anyOf(isType(InjectMatchers.JAVAX_INJECT_ANNOTATION), isType(GUICE_INJECT_ANNOTATION)));

  private static final Matcher<MethodTree> TO_MATCH =
      allOf(methodIsConstructor(), enclosingClass(hasModifier(ABSTRACT)));

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    if (TO_MATCH.matches(methodTree, state)) {
      MultiMatchResult<AnnotationTree> injectAnnotations =
          INJECT_FINDER.multiMatchResult(methodTree, state);
      if (injectAnnotations.matches()) {
        AnnotationTree injectAnnotation = injectAnnotations.matchingNodes().get(0);
        return describeMatch(injectAnnotation, delete(injectAnnotation));
      }
    }
    return Description.NO_MATCH;
  }
}
