/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.sun.source.tree.Tree.Kind.INTERFACE;
import static javax.lang.model.element.Modifier.DEFAULT;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

/** @author epmjohnston@google.com (Emily P.M. Johnston) */
@BugPattern(
    name = "StaticOrDefaultInterfaceMethod",
    summary =
        "Static and default interface methods are not natively supported on older Android devices. "
            ,
    severity = ERROR,
    documentSuppression = false // for slightly customized suppression documentation
    )
public class StaticOrDefaultInterfaceMethod extends BugChecker implements MethodTreeMatcher {

  private static final Matcher<Tree> IS_STATIC_OR_DEFAULT_METHOD_ON_INTERFACE =
      allOf(enclosingClass(kindIs(INTERFACE)), anyOf(hasModifier(STATIC), hasModifier(DEFAULT)));

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!state.isAndroidCompatible()) {
      return Description.NO_MATCH;
    }
    if (IS_STATIC_OR_DEFAULT_METHOD_ON_INTERFACE.matches(tree, state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }
}
