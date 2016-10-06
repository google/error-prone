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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.InjectMatchers.ASSISTED_INJECT_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.hasInjectAnnotation;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ClassTree;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
@BugPattern(
  name = "AssistedInjectAndInjectOnConstructors",
  summary =
      "@AssistedInject and @Inject should not be used on different constructors in the same class.",
  explanation =
      "Mixing @Inject and @AssistedInject leads to confusing code and the "
          + "documentation specifies not to do it. See "
          + "https://google.github.io/guice/api-docs/latest/javadoc/com/google/inject/assistedinject/AssistedInject.html",
  category = INJECT,
  severity = WARNING
)
public class AssistedInjectAndInjectOnConstructors extends BugChecker implements ClassTreeMatcher {

  /**
   * Matches if a class has a constructor that is annotated with @Inject and a constructor annotated
   * with @AssistedInject.
   */
  private static final Matcher<ClassTree> HAS_CONSTRUCTORS_WITH_INJECT_AND_ASSISTED_INJECT =
      allOf(
          constructor(AT_LEAST_ONE, hasInjectAnnotation()),
          constructor(AT_LEAST_ONE, hasAnnotation(ASSISTED_INJECT_ANNOTATION)));

  @Override
  public final Description matchClass(ClassTree classTree, VisitorState state) {
    if (HAS_CONSTRUCTORS_WITH_INJECT_AND_ASSISTED_INJECT.matches(classTree, state)) {
      return describeMatch(classTree);
    }

    return Description.NO_MATCH;
  }
}
