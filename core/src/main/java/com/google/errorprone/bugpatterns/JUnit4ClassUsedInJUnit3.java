/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.JUnitMatchers.isJUnit3TestClass;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

/** @author jdesprez@google.com (Julien Desprez) */
@BugPattern(
  name = "JUnit4ClassUsedInJUnit3",
  summary =
      "Some JUnit4 construct cannot be used in a JUnit3 context. Convert your class to JUnit4 "
          + "style to use them.",
  category = JUNIT,
  severity = WARNING
)
public class JUnit4ClassUsedInJUnit3 extends BugChecker
    implements MethodInvocationTreeMatcher, AnnotationTreeMatcher {

  private static final Matcher<ExpressionTree> ASSUME_CHECK =
      allOf(
          staticMethod().onClass("org.junit.Assume").withAnyName(),
          enclosingClass(isJUnit3TestClass));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    // An Assume method has been called within a JUnit3 class
    if (!ASSUME_CHECK.matches(tree, state)) {
      return NO_MATCH;
    }

    return makeDescription("Assume", tree);
  }

  @Override
  public Description matchAnnotation(AnnotationTree tree, VisitorState state) {
    if (!enclosingClass(isJUnit3TestClass).matches(tree, state)) {
      return NO_MATCH;
    }
    // If we are inside a JUnit3 test class some annotation should not appear.
    if (isType("org.junit.Ignore").matches(tree, state)) {
      return makeDescription("@Ignore", tree);
    }
    if (isType("org.junit.Rule").matches(tree, state)) {
      return makeDescription("@Rule", tree);
    }

    return NO_MATCH;
  }

  /**
   * Returns a {@link Description} of the error based on the rejected JUnit4 construct in the JUnit3
   * class.
   */
  private Description makeDescription(String rejectedJUnit4, Tree tree) {
    Description.Builder builder =
        buildDescription(tree)
            .setMessage(
                String.format(
                    "%s cannot be used inside a JUnit3 class. Convert your class to JUnit4 style.",
                    rejectedJUnit4));
    return builder.build();
  }
}
