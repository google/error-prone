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

import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.tree.JCTree;
import javax.annotation.Nullable;

/**
 * A serializable representation of a template syntax tree which can be unified with a target AST
 * and inlined based on a set of substitutions.
 *
 * @param <T> The type this tree inlines to.
 * @author Louis Wasserman (lowasser@google.com)
 */
public abstract class UTree<T extends JCTree> extends SimpleTreeVisitor<Choice<Unifier>, Unifier>
    implements Unifiable<Tree>, Inlineable<T>, Tree {
  @Override
  public Choice<Unifier> unify(@Nullable Tree target, Unifier unifier) {
    return (target != null) ? target.accept(this, unifier) : Choice.<Unifier>none();
  }

  @Override
  protected Choice<Unifier> defaultAction(Tree node, Unifier unifier) {
    return Choice.none();
  }
}
