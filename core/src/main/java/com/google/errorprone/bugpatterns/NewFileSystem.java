/*
 * Copyright 2022 The Error Prone Authors.
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

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Starting in JDK 13, this call is ambiguous with FileSystem.newFileSystem(Path,Map)",
    severity = WARNING)
public class NewFileSystem extends BugChecker implements BugChecker.MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      MethodMatchers.staticMethod()
          .onClass("java.nio.file.FileSystems")
          .named("newFileSystem")
          .withParameters("java.nio.file.Path", "java.lang.ClassLoader");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    ExpressionTree expressionTree = tree.getArguments().get(1);
    if (!expressionTree.getKind().equals(Tree.Kind.NULL_LITERAL)) {
      return NO_MATCH;
    }
    return describeMatch(tree, SuggestedFix.prefixWith(expressionTree, "(ClassLoader) "));
  }
}
