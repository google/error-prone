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

package com.google.errorprone.matchers;

import com.google.errorprone.SuggestedFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.CapturingMatcher.TreeHolder;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import static com.google.errorprone.matchers.Matchers.*;
import static com.sun.source.tree.Tree.Kind.STRING_LITERAL;
import static java.lang.String.format;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class PreconditionsCheckNotNullMatcher
    extends ErrorProducingMatcher<MethodInvocationTree> {

  @Override
  public AstError matchWithError(MethodInvocationTree tree, VisitorState state) {
    TreeHolder stringLiteralValue = new TreeHolder();

    if (allOf(
        methodSelect(staticMethod("com.google.common.base", "Preconditions", "checkNotNull")),
        argument(0, capture(stringLiteralValue, kindOf(STRING_LITERAL))))
        .matches(tree, state)) {
      DiagnosticPosition pos = ((JCMethodInvocation) tree).pos();
      SuggestedFix fix = new SuggestedFix(
          pos.getStartPosition(), pos.getEndPosition(state.compilationUnit.endPositions), "");
      return new AstError(
          stringLiteralValue.get(),
          format("String literal %s passed as first argument to Preconditions#checkNotNull",
              stringLiteralValue.get()),
          fix
      );
    }
    return null;
  }
}
