/*
 * Copyright 2021 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.methodHasVisibility;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.MethodVisibility;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Type;

/**
 * Checks if public APIs named "stream" returns a type whose name ends with Stream.
 *
 * @author sauravtiwary@google.com (Saurav Tiwary)
 */
@BugPattern(
    summary =
        "Public methods named stream() are generally expected to return a type whose name ends with"
            + " Stream. Consider choosing a different method name instead.",
    severity = SUGGESTION)
public class PublicApiNamedStreamShouldReturnStream extends BugChecker
    implements MethodTreeMatcher {
  private static final String STREAM = "stream";

  private static final Matcher<MethodTree> CONFUSING_PUBLIC_API_STREAM_MATCHER =
      allOf(
          methodIsNamed(STREAM),
          methodHasVisibility(MethodVisibility.Visibility.PUBLIC),
          PublicApiNamedStreamShouldReturnStream::returnTypeDoesNotEndsWithStream);

  private static boolean returnTypeDoesNotEndsWithStream(
      MethodTree methodTree, VisitorState state) {
    Type returnType = ASTHelpers.getSymbol(methodTree).getReturnType();

    // Constructors have no return type.
    return returnType != null && !returnType.tsym.getSimpleName().toString().endsWith("Stream");
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!CONFUSING_PUBLIC_API_STREAM_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    return describeMatch(tree);
  }
}
