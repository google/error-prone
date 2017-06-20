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

import static com.google.errorprone.BugPattern.Category.TRUTH;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.instanceMethod;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import java.util.List;

/**
 * Checks that variable argument methods have even number of arguments.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
  name = "ShouldHaveEvenArgs",
  summary = "This method must be called with an even number of arguments.",
  category = TRUTH,
  severity = ERROR
)
public class ShouldHaveEvenArgs extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      instanceMethod()
          .onDescendantOfAny(
              "com.google.common.truth.MapSubject",
              "com.google.common.truth.MapSubject.UsingCorrespondence",
              "com.google.common.truth.MultimapSubject",
              "com.google.common.truth.MultimapSubject.UsingCorrespondence")
          .named("containsExactly");

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    if (!MATCHER.matches(methodInvocationTree, state)) {
      return Description.NO_MATCH;
    }
    if (methodInvocationTree.getArguments().size() % 2 == 0) {
      return Description.NO_MATCH;
    }
    JCMethodInvocation methodInvocation = (JCMethodInvocation) methodInvocationTree;
    List<JCExpression> arguments = methodInvocation.getArguments();

    Type typeVargs = methodInvocation.varargsElement;
    if (typeVargs == null) {
      return Description.NO_MATCH;
    }
    Type typeVarargsArr = state.arrayTypeForType(typeVargs);
    Type lastArgType = ASTHelpers.getType(Iterables.getLast(arguments));
    if (typeVarargsArr.equals(lastArgType)) {
      return Description.NO_MATCH;
    }
    return describeMatch(methodInvocationTree);
  }
}
