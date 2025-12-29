/*
 * Copyright 2015 The Error Prone Authors.
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
import static com.google.errorprone.util.ASTHelpers.findSuperMethods;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.sun.source.tree.Tree.Kind.NULL_LITERAL;

import com.google.common.util.concurrent.Futures;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.util.TreePath;

/**
 * Superclass for checks that {@code AsyncCallable} and {@code AsyncFunction} implementations do not
 * directly {@code return null}.
 */
abstract class AbstractAsyncTypeReturnsNull extends BugChecker implements ReturnTreeMatcher {
  private final Class<?> asyncClass;

  AbstractAsyncTypeReturnsNull(Class<?> asyncClass) {
    this.asyncClass = asyncClass;
  }

  @Override
  public final Description matchReturn(ReturnTree tree, VisitorState state) {
    if (tree.getExpression() == null || tree.getExpression().getKind() != NULL_LITERAL) {
      return NO_MATCH;
    }
    TreePath path = state.getPath();
    while (path != null && path.getLeaf() instanceof StatementTree) {
      path = path.getParentPath();
    }
    if (path == null || !(path.getLeaf() instanceof MethodTree methodTree)) {
      return NO_MATCH;
    }
    if (findSuperMethods(getSymbol(methodTree), state.getTypes()).stream()
        .noneMatch(
            superMethod ->
                superMethod.owner != null
                    && superMethod.owner.getQualifiedName().contentEquals(asyncClass.getName()))) {
      return NO_MATCH;
    }
    return describeMatch(tree, provideFix(tree.getExpression()));
  }

  protected SuggestedFix provideFix(ExpressionTree tree) {
    return SuggestedFix.builder()
        .replace(tree, "immediateFuture(null)")
        .addStaticImport(Futures.class.getName() + ".immediateFuture")
        .build();
  }
}
