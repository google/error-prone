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
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.util.ListBuffer;
import java.util.List;

/**
 * {@link UTree} representation of a {@link BlockTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UBlock extends USimpleStatement implements BlockTree {
  public static UBlock create(List<UStatement> statements) {
    return new AutoValue_UBlock(ImmutableList.copyOf(statements));
  }

  public static UBlock create(UStatement... statements) {
    return create(ImmutableList.copyOf(statements));
  }

  @Override
  public abstract List<UStatement> getStatements();

  static Choice<Unifier> unifyStatementList(
      Iterable<? extends UStatement> statements,
      Iterable<? extends StatementTree> targets,
      Unifier unifier) {
    Choice<UnifierWithUnconsumedStatements> choice =
        Choice.of(UnifierWithUnconsumedStatements.create(unifier, ImmutableList.copyOf(targets)));
    for (UStatement statement : statements) {
      choice = choice.thenChoose(statement);
    }
    return choice.thenOption(
        (UnifierWithUnconsumedStatements state) ->
            state.unconsumedStatements().isEmpty()
                ? Optional.of(state.unifier())
                : Optional.<Unifier>absent());
  }

  static com.sun.tools.javac.util.List<JCStatement> inlineStatementList(
      Iterable<? extends UStatement> statements, Inliner inliner)
      throws CouldNotResolveImportException {
    ListBuffer<JCStatement> buffer = new ListBuffer<>();
    for (UStatement statement : statements) {
      buffer.appendList(statement.inlineStatements(inliner));
    }
    return buffer.toList();
  }

  @Override
  public Choice<Unifier> visitBlock(BlockTree block, Unifier unifier) {
    return unifyStatementList(getStatements(), block.getStatements(), unifier);
  }

  @Override
  public JCBlock inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner.maker().Block(0, inlineStatementList(getStatements(), inliner));
  }

  @Override
  public Kind getKind() {
    return Kind.BLOCK;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitBlock(this, data);
  }

  @Override
  public boolean isStatic() {
    return false;
  }
}
