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

package com.google.errorprone.bugpatterns.android;

import static com.google.errorprone.BugPattern.Category.ANDROID;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.AbstractReturnValueIgnored;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/** @author avenet@google.com (Arnaud J. Venet) */
@BugPattern(
  name = "RectIntersectReturnValueIgnored",
  summary = "Return value of android.graphics.Rect.intersect() must be checked",
  explanation =
      "`android.graphics.Rect.intersect(Rect r)` and "
          + "`android.graphics.Rect.intersect(int, int, int, int)` do not always modify the "
          + "rectangle to the intersected result. If the rectangles do not intersect, no change "
          + "is made and the original rectangle is not modified. These methods return false to "
          + "indicate that this has happened.\n\n"
          + "If you don’t check the return value of these methods, you may end up drawing the "
          + "wrong rectangle.",
  category = ANDROID,
  severity = ERROR
)
public final class RectIntersectReturnValueIgnored extends AbstractReturnValueIgnored {
  @Override
  public Matcher<? super ExpressionTree> specializedMatcher() {
    return instanceMethod().onExactClass("android.graphics.Rect").named("intersect");
  }

  @Override
  public Description describe(MethodInvocationTree methodInvocationTree, VisitorState state) {
    return describeMatch(methodInvocationTree);
  }
}
