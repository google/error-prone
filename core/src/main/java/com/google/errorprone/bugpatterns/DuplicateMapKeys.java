/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.HashSet;
import java.util.Set;

/**
 * Flags duplicate keys used in ImmutableMap construction.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    name = "DuplicateMapKeys",
    summary =
        "Map#ofEntries will throw an IllegalArgumentException if there are any duplicate keys",
    severity = ERROR,
    providesFix = ProvidesFix.NO_FIX)
public class DuplicateMapKeys extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> METHOD_MATCHER =
      MethodMatchers.staticMethod().onClass("java.util.Map").named("ofEntries");

  private static final Matcher<ExpressionTree> ENTRY_MATCHER =
      MethodMatchers.staticMethod().onClass("java.util.Map").named("entry");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, final VisitorState state) {
    if (!METHOD_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    Set<Object> keySet = new HashSet<>();
    for (ExpressionTree expr : tree.getArguments()) {
      if (!(expr instanceof MethodInvocationTree)) {
        continue;
      }
      if (!ENTRY_MATCHER.matches(expr, state)) {
        continue;
      }
      MethodInvocationTree entryInvocation = (MethodInvocationTree) expr;
      Object key = ASTHelpers.constValue(entryInvocation.getArguments().get(0));
      if (key == null) {
        continue;
      }
      if (!keySet.add(key)) {
        return buildDescription(tree)
            .setMessage(
                String.format(
                    "duplicate key '%s'; Map#ofEntries will throw an IllegalArgumentException",
                    key))
            .build();
      }
    }
    return Description.NO_MATCH;
  }
}
