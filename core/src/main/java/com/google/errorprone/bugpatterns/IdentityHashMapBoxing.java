/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Type;
import java.util.List;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Using IdentityHashMap with a boxed type as the key is risky since boxing may produce"
            + " distinct instances",
    severity = ERROR)
public class IdentityHashMapBoxing extends BugChecker
    implements NewClassTreeMatcher, MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> NEW_IDENTITY_HASH_MAP =
      constructor().forClass("java.util.IdentityHashMap");
  private static final Matcher<ExpressionTree> MAPS_NEW_IDENTITY_HASH_MAP =
      staticMethod().onClass("com.google.common.collect.Maps").named("newIdentityHashMap");

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!NEW_IDENTITY_HASH_MAP.matches(tree, state)) {
      return NO_MATCH;
    }
    return checkTypes(tree, state);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MAPS_NEW_IDENTITY_HASH_MAP.matches(tree, state)) {
      return NO_MATCH;
    }
    return checkTypes(tree, state);
  }

  private Description checkTypes(ExpressionTree tree, VisitorState state) {
    List<Type> argumentTypes = ASTHelpers.getResultType(tree).getTypeArguments();
    if (argumentTypes.size() != 2) {
      return Description.NO_MATCH;
    }
    Type type = state.getTypes().unboxedType(argumentTypes.get(0));
    return switch (type.getKind()) {
      case DOUBLE, LONG, INT, FLOAT -> describeMatch(tree);
      default -> Description.NO_MATCH;
    };
  }
}
