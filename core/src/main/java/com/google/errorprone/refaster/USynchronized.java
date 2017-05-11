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
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCSynchronized;

/**
 * {@link UTree} representation of a {@link SynchronizedTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class USynchronized extends USimpleStatement implements SynchronizedTree {
  public static USynchronized create(UExpression expression, UBlock block) {
    return new AutoValue_USynchronized(expression, block);
  }

  @Override
  public abstract UExpression getExpression();

  @Override
  public abstract UBlock getBlock();

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitSynchronized(this, data);
  }

  @Override
  public Kind getKind() {
    return Kind.SYNCHRONIZED;
  }

  @Override
  public JCSynchronized inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner
        .maker()
        .Synchronized(getExpression().inline(inliner), getBlock().inline(inliner));
  }

  @Override
  public Choice<Unifier> visitSynchronized(SynchronizedTree synced, Unifier unifier) {
    return getExpression()
        .unify(synced.getExpression(), unifier)
        .thenChoose(unifications(getBlock(), synced.getBlock()));
  }
}
