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

import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.sun.source.tree.Tree.Kind.NULL_LITERAL;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.util.TreePath;
import java.util.Optional;

/** Superclass for checks that method implementations that directly {@code return null}. */
abstract class AbstractMethodReturnsNull extends BugChecker implements ReturnTreeMatcher {
  private final Matcher<MethodTree> methodTreeMatcher;

  AbstractMethodReturnsNull(Matcher<MethodTree> methodTreeMatcher) {
    this.methodTreeMatcher = methodTreeMatcher;
  }

  protected abstract Optional<Fix> provideFix(ReturnTree tree);

  @Override
  public final Description matchReturn(ReturnTree tree, VisitorState state) {
    if (tree.getExpression() == null || tree.getExpression().getKind() != NULL_LITERAL) {
      return NO_MATCH;
    }
    TreePath path = state.getPath();
    while (path != null && path.getLeaf() instanceof StatementTree) {
      path = path.getParentPath();
    }
    if (path == null || !(path.getLeaf() instanceof MethodTree)) {
      return NO_MATCH;
    }
    if (!methodTreeMatcher.matches((MethodTree) path.getLeaf(), state)) {
      return NO_MATCH;
    }
    return provideFix(tree)
        .map(fix -> describeMatch(tree, fix))
        .orElseGet(() -> describeMatch(tree));
  }
}
