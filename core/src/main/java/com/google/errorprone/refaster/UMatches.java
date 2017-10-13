/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import com.google.auto.value.AutoValue;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.util.Context;
import javax.annotation.Nullable;

/**
 * {@code UMatches} allows conditionally matching a {@code UExpression} predicated by an error-prone
 * {@code Matcher}.
 */
@AutoValue
abstract class UMatches extends UExpression {
  public static UMatches create(
      Class<? extends Matcher<? super ExpressionTree>> matcherClass,
      boolean positive,
      UExpression expression) {
    // Verify that we can instantiate the Matcher
    makeMatcher(matcherClass);

    return new AutoValue_UMatches(positive, matcherClass, expression);
  }

  abstract boolean positive();

  abstract Class<? extends Matcher<? super ExpressionTree>> matcherClass();

  abstract UExpression expression();

  @Override
  @Nullable
  protected Choice<Unifier> defaultAction(Tree target, @Nullable Unifier unifier) {
    final Tree exprTarget = ASTHelpers.stripParentheses(target);
    return expression()
        .unify(exprTarget, unifier)
        .condition((Unifier success) -> matches(exprTarget, success) == positive());
  }

  @Override
  public JCExpression inline(Inliner inliner) throws CouldNotResolveImportException {
    throw new UnsupportedOperationException("@Matches should not appear in an @AfterTemplate");
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return expression().accept(visitor, data);
  }

  @Override
  public Kind getKind() {
    return Kind.OTHER;
  }

  private transient Matcher<? super ExpressionTree> matcher;

  private boolean matches(Tree target, Unifier unifier) {
    if (matcher == null) {
      matcher = makeMatcher(matcherClass());
    }
    return target instanceof ExpressionTree
        && matcher.matches((ExpressionTree) target, makeVisitorState(target, unifier));
  }

  static <T> T makeMatcher(Class<T> klass) {
    try {
      return klass.newInstance();
    } catch (IllegalAccessException | InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  static VisitorState makeVisitorState(Tree target, Unifier unifier) {
    Context context = unifier.getContext();
    TreePath path = TreePath.getPath(context.get(JCCompilationUnit.class), target);
    return new VisitorState(context).withPath(path);
  }
}
