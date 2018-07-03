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

package com.google.errorprone.bugpatterns.threadsafety;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.regex.Pattern;

/**
 * Bug checker to detect usage of deprecated Thread methods as detailed in {@link Thread}
 *
 * @author siyuanl@google.com (Siyuan Liu)
 */
@BugPattern(
    name = "DeprecatedThreadMethods",
    summary = "Avoid deprecated Thread methods; read the method's javadoc for details.",
    severity = WARNING)
public class DeprecatedThreadMethods extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Pattern METHOD_NAME_REGEX =
      Pattern.compile("stop|countStackFrames|destroy|resume|suspend");

  // Might be overmatching--Thread subclasses could have additional methods with same names
  private static final Matcher<ExpressionTree> DEPRACATED =
      anyOf(
          Matchers.instanceMethod()
              .onDescendantOf("java.lang.Thread")
              .withNameMatching(METHOD_NAME_REGEX));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return DEPRACATED.matches(tree, state) ? describeMatch(tree) : Description.NO_MATCH;
  }
}
