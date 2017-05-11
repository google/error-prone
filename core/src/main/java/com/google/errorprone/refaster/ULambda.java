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
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCLambda.ParameterKind;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

/**
 * {@code UTree} representation of a {@code LambdaExpressionTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class ULambda extends UExpression implements LambdaExpressionTree {
  public static ULambda create(
      ParameterKind parameterKind, Iterable<UVariableDecl> parameters, UTree<?> body) {
    return new AutoValue_ULambda(parameterKind, ImmutableList.copyOf(parameters), body);
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
    return inliner.maker().Lambda(inlineParams(inliner), inlineBody(inliner));
  }

  public List<JCVariableDecl> inlineParams(Inliner inliner) throws CouldNotResolveImportException {
    if (parameterKind() == ParameterKind.EXPLICIT) {
      return List.convert(JCVariableDecl.class, inliner.inlineList(getParameters()));
    }
    ListBuffer<JCVariableDecl> params = new ListBuffer<>();
    for (UVariableDecl param : getParameters()) {
      params.add(param.inlineImplicitType(inliner));
    }
    return params.toList();
  }

  JCTree inlineBody(Inliner inliner) throws CouldNotResolveImportException {
    if (getBody() instanceof UPlaceholderExpression) {
      UPlaceholderExpression body = (UPlaceholderExpression) getBody();
      Optional<List<JCStatement>> blockBinding =
          inliner.getOptionalBinding(body.placeholder().blockKey());
      if (blockBinding.isPresent()) {
        // this lambda is of the form args -> blockPlaceholder();
        List<JCStatement> blockInlined =
            UPlaceholderExpression.copier(body.arguments(), inliner)
                .copy(blockBinding.get(), inliner);
        if (blockInlined.size() == 1) {
          if (blockInlined.get(0) instanceof JCReturn) {
            return ((JCReturn) blockInlined.get(0)).getExpression();
          } else if (blockInlined.get(0) instanceof JCExpressionStatement) {
            return ((JCExpressionStatement) blockInlined.get(0)).getExpression();
          }
        }
        return inliner.maker().Block(0, blockInlined);
      }
    }
    return getBody().inline(inliner);
  }

  abstract ParameterKind parameterKind();

  @Override
  public abstract ImmutableList<UVariableDecl> getParameters();

  @Override
  public abstract UTree<?> getBody();

  @Override
  public BodyKind getBodyKind() {
    return getBody().getKind() == Kind.BLOCK ? BodyKind.STATEMENT : BodyKind.EXPRESSION;
  }
}
