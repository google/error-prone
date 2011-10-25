/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.errorprone.checkers;

import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

import static com.google.errorprone.matchers.Matchers.*;
import static com.sun.source.tree.Tree.Kind.STRING_LITERAL;
import static java.lang.String.format;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class PreconditionsCheckNotNullChecker extends ErrorChecker<MethodInvocationTree> {

  @Override
  public Matcher<MethodInvocationTree> matcher() {
    return allOf(
        methodSelect(staticMethod("com.google.common.base", "Preconditions", "checkNotNull")),
        argument(0, Matchers.<ExpressionTree>kindIs(STRING_LITERAL)));
  }

  @Override
  public AstError produceError(MethodInvocationTree methodInvocationTree, VisitorState state) {
    ExpressionTree stringLiteralValue = methodInvocationTree.getArguments().get(0);
    Position pos = getSourcePosition(methodInvocationTree);
    SuggestedFix fix = new SuggestedFix(pos.start, pos.end, "");
    return new AstError(stringLiteralValue,
        format("String literal %s passed as first argument to Preconditions#checkNotNull",
            stringLiteralValue), fix);
  }
}
