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

package com.google.errorprone.bugpatterns.inject;

import static com.google.errorprone.BugPattern.Category.INJECT;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.not;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.InjectMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.MethodTree;

/** @author bhagwani@google.com (Sumit Bhagwani) */
@BugPattern(
    name = "CloseableProvides",
    summary = "Providing Closeable resources makes their lifecycle unclear",
    category = INJECT,
    severity = WARNING,
    providesFix = ProvidesFix.NO_FIX)
public class CloseableProvides extends BugChecker implements MethodTreeMatcher {

  private static final Matcher<MethodTree> CLOSEABLE_PROVIDES_MATCHER =
      allOf(
          InjectMatchers.hasProvidesAnnotation(),
          methodReturns(Matchers.isSubtypeOf("java.io.Closeable"))
          );

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!CLOSEABLE_PROVIDES_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree);
  }
}
