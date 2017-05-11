/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.matchers.Matchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * @author chy@google.com (Christine Yang)
 * @author kmuhlrad@google.com (Katy Muhlrad)
 */
@BugPattern(
  name = "GetClassOnClass",
  summary =
      "Calling getClass() on an object of type Class returns the Class object for "
          + "java.lang.Class; you probably meant to operate on the object directly",
  explanation =
      "Calling `getClass()` on an object of type Class returns the Class object for "
          + "java.lang.Class.  Usually this is a mistake, and people intend to operate on the "
          + "object itself (for example, to print an error message).  If you really did intend to "
          + "operate on the Class object for java.lang.Class, please use `Class.class` instead for "
          + "clarity.",
  category = JDK,
  severity = ERROR
)
public class GetClassOnClass extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> getClassMethodMatcher =
      instanceMethod().onExactClass("java.lang.Class").named("getClass");

  /** Suggests removing getClass() or changing to Class.class. */
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (getClassMethodMatcher.matches(tree, state)) {
      String methodInvoker = ASTHelpers.getReceiver(tree).toString();
      Fix removeGetClass = SuggestedFix.replace(tree, methodInvoker);
      Fix changeToClassDotClass = SuggestedFix.replace(tree, "Class.class");
      return buildDescription(tree).addFix(removeGetClass).addFix(changeToClassDotClass).build();
    }
    return Description.NO_MATCH;
  }
}
