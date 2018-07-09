/*
 * Copyright 2018 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.methodReturns;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodTree;

/**
 * Check for functional return types.
 *
 * @author seibelsabrina@google.com (Sabrina Seibel)
 */
@BugPattern(
    name = "NoFunctionalReturnType",
    summary =
        "Instead of returning a functional type, return the actual type that the returned function"
            + " would return and use lambdas at use site.",
    explanation =
        "Returning the actual type that the returned function would return instead of a functional"
            + " type creates a more versatile method",
    severity = WARNING)
public final class NoFunctionalReturnType extends BugChecker implements MethodTreeMatcher {

  private static final Matcher<MethodTree> FUNCTIONAL_RETURN_TYPE =
      methodReturns(
          (tree, state) ->
              ASTHelpers.getType(tree).tsym.packge().fullname.contentEquals("java.util.function"));

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    return FUNCTIONAL_RETURN_TYPE.matches(tree, state) ? describeMatch(tree) : Description.NO_MATCH;
  }
}
