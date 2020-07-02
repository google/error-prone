/*
 * Copyright 2020 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Type;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "JavaUtilDate",
    summary = "Date has a bad API that leads to bugs; prefer java.time.Instant or LocalDate.",
    severity = WARNING)
public class JavaUtilDateChecker extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private static final Matcher<ExpressionTree> EXEMPTIONS =
      anyOf(
          instanceMethod().onExactClass("java.util.Date").named("toInstant"),
          staticMethod()
              .onClass("java.util.Date")
              .named("from")
              .withParameters("java.time.Instant"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!isDate(state, ASTHelpers.getReceiverType(tree))) {
      return NO_MATCH;
    }
    if (EXEMPTIONS.matches(tree, state)) {
      return NO_MATCH;
    }
    return describeMatch(tree);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    return isDate(state, ASTHelpers.getType(tree.getIdentifier())) ? describeMatch(tree) : NO_MATCH;
  }

  private static boolean isDate(VisitorState state, Type type) {
    return ASTHelpers.isSameType(type, state.getTypeFromString("java.util.Date"), state);
  }
}
