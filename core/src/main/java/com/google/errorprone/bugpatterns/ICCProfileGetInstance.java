/*
 * Copyright 2023 The Error Prone Authors.
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
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "This method searches the class path for the given file, prefer to read the file and call"
            + " getInstance(byte[]) or getInstance(InputStream)",
    severity = WARNING,
    tags = StandardTags.PERFORMANCE)
public class ICCProfileGetInstance extends BugChecker
    implements BugChecker.MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      staticMethod()
          .onClass("java.awt.color.ICC_Profile")
          .named("getInstance")
          .withParameters("java.lang.String");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    ExpressionTree arg = Iterables.getOnlyElement(tree.getArguments());
    return describeMatch(
        tree,
        SuggestedFix.builder()
            .prefixWith(arg, "Files.readAllBytes(Paths.get(")
            .postfixWith(arg, "))")
            .addImport("java.nio.file.Files")
            .addImport("java.nio.file.Paths")
            .build());
  }
}
