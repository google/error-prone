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

import static com.google.errorprone.refaster.Unifier.unifications;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import java.util.List;
import javax.annotation.Nullable;

/**
 * {@link UTree} version of {@link MethodInvocationTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
public abstract class UMethodInvocation extends UExpression implements MethodInvocationTree {
  public static UMethodInvocation create(UExpression methodSelect, List<UExpression> arguments) {
    return new AutoValue_UMethodInvocation(methodSelect, arguments);
  }

  public static UMethodInvocation create(UExpression methodSelect, UExpression... arguments) {
    return create(methodSelect, ImmutableList.copyOf(arguments));
  }

  @Override
  public abstract UExpression getMethodSelect();

  @Override
  public abstract List<UExpression> getArguments();

  @Override
  @Nullable
  public Choice<Unifier> visitMethodInvocation(
      MethodInvocationTree methodInvocation, @Nullable Unifier unifier) {
    return getMethodSelect()
        .unify(methodInvocation.getMethodSelect(), unifier)
        .thenChoose(unifications(getArguments(), methodInvocation.getArguments(), true));
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitMethodInvocation(this, data);
  }

  @Override
  public Kind getKind() {
    return Kind.METHOD_INVOCATION;
  }

  @Override
  public List<UTree<?>> getTypeArguments() {
    return ImmutableList.of();
  }

  @Override
  public JCMethodInvocation inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner
        .maker()
        .Apply(
            com.sun.tools.javac.util.List.<JCExpression>nil(),
            getMethodSelect().inline(inliner),
            inliner.<JCExpression>inlineList(getArguments()));
  }
}
