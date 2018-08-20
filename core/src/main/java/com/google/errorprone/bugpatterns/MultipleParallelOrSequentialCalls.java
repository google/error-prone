/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.method.MethodMatchers.MethodNameMatcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePath;

/** @author mariasam@google.com (Maria Sam) */
@BugPattern(
    name = "MultipleParallelOrSequentialCalls",
    summary =
        "Multiple calls to either parallel or sequential are unnecessary and cause confusion.",
    category = JDK,
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class MultipleParallelOrSequentialCalls extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final MethodNameMatcher STREAM =
      instanceMethod().onDescendantOf("java.util.Collection").named("stream");

  private static final MethodNameMatcher PARALLELSTREAM =
      instanceMethod().onDescendantOf("java.util.Collection").named("parallelStream");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree t, VisitorState state) {

    if (STREAM.matches(t, state) || PARALLELSTREAM.matches(t, state)) {
      int appropriateAmount = STREAM.matches(t, state) ? 1 : 0;
      SuggestedFix.Builder builder = SuggestedFix.builder();
      TreePath pathToMet =
          ASTHelpers.findPathFromEnclosingNodeToTopLevel(
              state.getPath(), MethodInvocationTree.class);
      // counts how many instances of parallel / sequential
      int count = 0;
      String toReplace = "empty";
      while (pathToMet != null) {
        MethodInvocationTree methodInvocationTree = (MethodInvocationTree) pathToMet.getLeaf();
        // this check makes it so that we stop iterating up once it's done
        if (methodInvocationTree.getArguments().stream()
            .map(m -> m.toString())
            .anyMatch(m -> m.contains(t.toString()))) {
          break;
        }
        if (methodInvocationTree.getMethodSelect() instanceof MemberSelectTree) {
          MemberSelectTree memberSelectTree =
              (MemberSelectTree) methodInvocationTree.getMethodSelect();
          String memberSelectIdentifier = memberSelectTree.getIdentifier().toString();
          // checks for the first instance of parallel / sequential
          if (toReplace.equals("empty")
              && (memberSelectIdentifier.equals("parallel")
                  || memberSelectIdentifier.equals("sequential"))) {
            toReplace = memberSelectIdentifier.equals("parallel") ? "parallel" : "sequential";
          }
          // immediately removes any instances of the appropriate string
          if (memberSelectIdentifier.equals(toReplace)) {
            int endOfExpression = state.getEndPosition(memberSelectTree.getExpression());
            builder.replace(endOfExpression, state.getEndPosition(methodInvocationTree), "");
            count++;
          }
        }
        pathToMet =
            ASTHelpers.findPathFromEnclosingNodeToTopLevel(pathToMet, MethodInvocationTree.class);
      }
      // if there are too many parallel / sequential calls, then we describe match and actually
      // use the builder's replacements and add a postfix
      if (count > appropriateAmount) {
        // parallel stream doesn't need a postfix
        if (appropriateAmount == 1) {
          builder.postfixWith(t, "." + toReplace + "()");
        }
        return describeMatch(t, builder.build());
      }
    }
    return Description.NO_MATCH;
  }
}
