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

package com.google.errorprone.matchers.method;

import com.google.common.base.Optional;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/** Base matcher for member methods. */
abstract class MethodMatcher extends AbstractChainedMatcher<MatchState, MatchState> {
  private static final AbstractSimpleMatcher<MatchState> BASE_METHOD_MATCHER =
      new AbstractSimpleMatcher<MatchState>() {
        @Override
        public Optional<MatchState> matchResult(ExpressionTree tree, VisitorState state) {
          Symbol sym = ASTHelpers.getSymbol(tree);
          if (!(sym instanceof MethodSymbol)) {
            return Optional.absent();
          }
          if (tree instanceof NewClassTree) {
            // Don't match constructors as they are neither static nor instance methods.
            return Optional.absent();
          }
          if (tree instanceof MethodInvocationTree) {
            tree = ((MethodInvocationTree) tree).getMethodSelect();
          }
          return Optional.of(
              MatchState.create(ASTHelpers.getReceiverType(tree), (MethodSymbol) sym));
        }
      };

  MethodMatcher() {
    super(BASE_METHOD_MATCHER);
  }
}
