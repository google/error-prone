/*
 * Copyright 2011 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyMethod;
import static com.google.errorprone.matchers.Matchers.symbolHasAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.DoNotStore;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.VariableTree;

/**
 * Warns that methods annotated with {@code @DoNotStore} should not have their return value assigned
 * to a variable, but should instead be used directly as another method parameter.
 */
@BugPattern(name = "DoNotStore", summary = "Do not store return value.", severity = WARNING)
public class DoNotStoreChecker extends BugChecker
    implements AssignmentTreeMatcher, VariableTreeMatcher {
  private static final Matcher<ExpressionTree> MATCHER =
      allOf(anyMethod(), symbolHasAnnotation(DoNotStore.class));

  @Override
  public Description matchAssignment(AssignmentTree tree, VisitorState state) {
    return match(tree.getExpression(), state);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    return match(tree.getInitializer(), state);
  }

  private Description match(ExpressionTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }

    return buildDescription(tree)
        .setMessage(
            "Do not store the return value of methods annotated with @DoNotStore. Instead, call "
                + "the method directly as another method parameter.")
        .build();
  }
}
