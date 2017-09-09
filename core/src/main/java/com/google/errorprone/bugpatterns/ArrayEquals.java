/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.predicates.TypePredicates.isArray;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;

/** @author eaftan@google.com (Eddie Aftandilian) */
@BugPattern(
  name = "ArrayEquals",
  summary = "Reference equality used to compare arrays",
  explanation =
      "Generally when comparing arrays for equality, the programmer intends to check that the "
          + "the contents of the arrays are equal rather than that they are actually the same "
          + "object.  But many commonly used equals methods compare arrays for reference equality "
          + "rather than content equality. These include the instance .equals() method, Guava's "
          + "com.google.common.base.Objects#equal(), and the JDK's java.util.Objects#equals().\n\n"
          + "If reference equality is needed, == should be used instead for clarity. Otherwise, "
          + "use java.util.Arrays#equals() to compare the contents of the arrays.",
  category = JDK,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class ArrayEquals extends BugChecker implements MethodInvocationTreeMatcher {
  /** Matches when the equals instance method is used to compare two arrays. */
  private static final Matcher<MethodInvocationTree> instanceEqualsMatcher =
      Matchers.allOf(
          instanceMethod().onClass(isArray()).named("equals"),
          argument(0, Matchers.<ExpressionTree>isArrayType()));

  /**
   * Matches when the Guava com.google.common.base.Objects#equal or the JDK7
   * java.util.Objects#equals method is used to compare two arrays.
   */
  private static final Matcher<MethodInvocationTree> staticEqualsMatcher =
      allOf(
          anyOf(
              staticMethod().onClass("com.google.common.base.Objects").named("equal"),
              staticMethod().onClass("java.util.Objects").named("equals")),
          argument(0, Matchers.<ExpressionTree>isArrayType()),
          argument(1, Matchers.<ExpressionTree>isArrayType()));

  /**
   * Suggests replacing with Arrays.equals(a, b). Also adds the necessary import statement for
   * java.util.Arrays.
   */
  @Override
  public Description matchMethodInvocation(MethodInvocationTree t, VisitorState state) {
    String arg1;
    String arg2;
    if (instanceEqualsMatcher.matches(t, state)) {
      arg1 = ((JCFieldAccess) t.getMethodSelect()).getExpression().toString();
      arg2 = t.getArguments().get(0).toString();
    } else if (staticEqualsMatcher.matches(t, state)) {
      arg1 = t.getArguments().get(0).toString();
      arg2 = t.getArguments().get(1).toString();
    } else {
      return NO_MATCH;
    }

    Fix fix =
        SuggestedFix.builder()
            .replace(t, "Arrays.equals(" + arg1 + ", " + arg2 + ")")
            .addImport("java.util.Arrays")
            .build();
    return describeMatch(t, fix);
  }
}
