/*
 * Copyright 2014 Google Inc. All rights reserved.
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

import static com.google.errorprone.refaster.Unifier.unifications;
import static com.google.errorprone.refaster.Unifier.unifyList;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;

/**
 * {@code UTree} representation of a {@code LambdaExpressionTree}.
 * 
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class ULambda extends UExpression implements LambdaExpressionTree {
  public static ULambda create(Iterable<UVariableDecl> parameters, UTree<?> body) {
    return new AutoValue_ULambda(ImmutableList.copyOf(parameters), body);
  }
  
  @Override
  public Kind getKind() {
    return Kind.LAMBDA_EXPRESSION;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitLambdaExpression(this, data);
  }

  @Override
  public Choice<Unifier> visitLambdaExpression(LambdaExpressionTree node, Unifier unifier) {
    return unifyList(unifier, getParameters(), node.getParameters())
        .thenChoose(unifications(getBody(), node.getBody()));
  }

  @Override
  public JCLambda inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner.maker().Lambda(
        List.convert(JCVariableDecl.class, inliner.inlineList(getParameters())), 
        getBody().inline(inliner));
  }

  @Override
  public abstract ImmutableList<UVariableDecl> getParameters();

  @Override
  public abstract UTree<?> getBody();

  @Override
  public BodyKind getBodyKind() {
    return getBody().getKind() == Kind.BLOCK ? BodyKind.STATEMENT : BodyKind.EXPRESSION;
  }
}
