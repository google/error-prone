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

import com.google.auto.value.AutoValue;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import javax.annotation.Nullable;

/**
 * {@code UTree} representation of a {@code CatchTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UCatch extends UTree<JCCatch> implements CatchTree {
  static UCatch create(UVariableDecl parameter, UBlock block) {
    return new AutoValue_UCatch(parameter, block);
  }

  @Override
  public abstract UVariableDecl getParameter();

  @Override
  public abstract UBlock getBlock();

  @Override
  public Kind getKind() {
    return Kind.CATCH;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitCatch(this, data);
  }

  @Override
  public JCCatch inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner.maker().Catch(getParameter().inline(inliner), getBlock().inline(inliner));
  }

  @Override
  @Nullable
  public Choice<Unifier> visitCatch(CatchTree node, @Nullable Unifier unifier) {
    return getParameter()
        .unify(node.getParameter(), unifier)
        .thenChoose(unifications(getBlock(), node.getBlock()));
  }
}
