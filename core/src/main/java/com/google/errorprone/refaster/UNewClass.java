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
import static com.google.errorprone.refaster.Unifier.unifyNullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import java.util.List;
import javax.annotation.Nullable;

/**
 * {@link UTree} version of {@link NewClassTree}, which represents a constructor invocation.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UNewClass extends UExpression implements NewClassTree {

  public static UNewClass create(
      UExpression enclosingExpression,
      List<? extends UExpression> typeArguments,
      UExpression identifier,
      List<UExpression> arguments,
      @Nullable UClassDecl classBody) {
    return new AutoValue_UNewClass(
        enclosingExpression,
        ImmutableList.copyOf(typeArguments),
        identifier,
        ImmutableList.copyOf(arguments),
        classBody);
  }

  public static UNewClass create(
      List<? extends UExpression> typeArguments, UExpression identifier, UExpression... arguments) {
    return create(null, typeArguments, identifier, ImmutableList.copyOf(arguments), null);
  }

  public static UNewClass create(UExpression identifier, UExpression... arguments) {
    return create(ImmutableList.<UExpression>of(), identifier, arguments);
  }

  @Override
  @Nullable
  public abstract UExpression getEnclosingExpression();

  /**
   * Note: these are not the type arguments to the class, but to the constructor, for those
   * extremely rare constructors that look like e.g. {@code <E> Foo(E e)}, where the type parameter
   * is for the constructor alone and not the class.
   */
  @Override
  public abstract List<UExpression> getTypeArguments();

  @Override
  public abstract UExpression getIdentifier();

  @Override
  public abstract List<UExpression> getArguments();

  @Override
  @Nullable
  public abstract UClassDecl getClassBody();

  @Override
  @Nullable
  public Choice<Unifier> visitNewClass(NewClassTree newClass, @Nullable Unifier unifier) {
    return unifyNullable(unifier, getEnclosingExpression(), newClass.getEnclosingExpression())
        .thenChoose(unifications(getTypeArguments(), newClass.getTypeArguments()))
        .thenChoose(unifications(getIdentifier(), newClass.getIdentifier()))
        .thenChoose(unifications(getClassBody(), newClass.getClassBody()))
        .thenChoose(unifications(getArguments(), newClass.getArguments()));
  }

  @Override
  public Kind getKind() {
    return Kind.NEW_CLASS;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitNewClass(this, data);
  }

  @Override
  public JCNewClass inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner
        .maker()
        .NewClass(
            (getEnclosingExpression() == null) ? null : getEnclosingExpression().inline(inliner),
            inliner.<JCExpression>inlineList(getTypeArguments()),
            getIdentifier().inline(inliner),
            inliner.<JCExpression>inlineList(getArguments()),
            (getClassBody() == null) ? null : getClassBody().inline(inliner));
  }
}
