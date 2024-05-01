/*
 * Copyright 2024 The Error Prone Authors.
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
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static java.util.stream.Collectors.joining;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.List;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Nesting Modules.combine() inside Guice.createInjector() is unnecessary.",
    severity = WARNING)
public class GuiceCreateInjectorWithCombineRefactor extends BugChecker
    implements MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> GUICE_CREATE_INJECTOR_METHOD =
      staticMethod().onClass("com.google.inject.Guice").named("createInjector");

  private static final Matcher<ExpressionTree> MODULES_COMBINE_METHOD =
      staticMethod().onClass("com.google.inject.util.Modules").named("combine");

  /** Matches if Guice.createInjector() is called with a Modules.combine() argument. */
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!GUICE_CREATE_INJECTOR_METHOD.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    SuggestedFix.Builder suggestedFixBuilder = SuggestedFix.builder();
    for (ExpressionTree arg : tree.getArguments()) {
      if (MODULES_COMBINE_METHOD.matches(arg, state)) {
        MethodInvocationTree combineInvocation = (MethodInvocationTree) arg;
        List<? extends ExpressionTree> subModules = combineInvocation.getArguments();
        String replacement =
            subModules.stream()
                .map(module -> state.getSourceForNode(module))
                .collect(joining(", "));
        suggestedFixBuilder.replace(arg, replacement);
      }
    }
    var fix = suggestedFixBuilder.build();
    return fix.isEmpty() ? Description.NO_MATCH : describeMatch(tree, fix);
  }
}
