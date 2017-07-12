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
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.errorprone.refaster.ControlFlowVisitor.Result;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.util.List;
import javax.annotation.Nullable;

/**
 * {@link UTree} representation of an {@link IfTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UIf implements UStatement, IfTree {
  public static UIf create(
      UExpression condition, UStatement thenStatement, UStatement elseStatement) {
    return new AutoValue_UIf(condition, thenStatement, elseStatement);
  }

  @Override
  public abstract UExpression getCondition();

  @Override
  public abstract UStatement getThenStatement();

  @Override
  @Nullable
  public abstract UStatement getElseStatement();

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitIf(this, data);
  }

  @Override
  public Kind getKind() {
    return Kind.IF;
  }

  private static Function<Unifier, Choice<Unifier>> unifyUStatementWithSingleStatement(
      @Nullable final UStatement toUnify, @Nullable final StatementTree target) {
    return (Unifier unifier) -> {
      if (toUnify == null) {
        return (target == null) ? Choice.of(unifier) : Choice.<Unifier>none();
      }
      List<StatementTree> list = (target == null) ? List.<StatementTree>nil() : List.of(target);
      return toUnify
          .apply(UnifierWithUnconsumedStatements.create(unifier, list))
          .condition(s -> s.unconsumedStatements().isEmpty())
          .transform(UnifierWithUnconsumedStatements::unifier);
    };
  }

  @Override
  @Nullable
  public Choice<UnifierWithUnconsumedStatements> apply(UnifierWithUnconsumedStatements state) {
    java.util.List<? extends StatementTree> unconsumedStatements = state.unconsumedStatements();
    if (unconsumedStatements.isEmpty()) {
      return Choice.none();
    }
    final java.util.List<? extends StatementTree> unconsumedStatementsTail =
        unconsumedStatements.subList(1, unconsumedStatements.size());
    StatementTree firstStatement = unconsumedStatements.get(0);
    if (firstStatement.getKind() != Kind.IF) {
      return Choice.none();
    }
    final IfTree ifTree = (IfTree) firstStatement;
    Unifier unifier = state.unifier();
    Choice<UnifierWithUnconsumedStatements> forwardMatch =
        getCondition()
            .unify(ifTree.getCondition(), unifier.fork())
            .thenChoose(
                unifyUStatementWithSingleStatement(getThenStatement(), ifTree.getThenStatement()))
            .thenChoose(
                unifierAfterThen -> {
                  if (getElseStatement() != null
                      && ifTree.getElseStatement() == null
                      && ControlFlowVisitor.INSTANCE.visitStatement(ifTree.getThenStatement())
                          == Result.ALWAYS_RETURNS) {
                    Choice<UnifierWithUnconsumedStatements> result =
                        getElseStatement()
                            .apply(
                                UnifierWithUnconsumedStatements.create(
                                    unifierAfterThen.fork(), unconsumedStatementsTail));
                    if (getElseStatement() instanceof UBlock) {
                      Choice<UnifierWithUnconsumedStatements> alternative =
                          Choice.of(
                              UnifierWithUnconsumedStatements.create(
                                  unifierAfterThen.fork(), unconsumedStatementsTail));
                      for (UStatement stmt : ((UBlock) getElseStatement()).getStatements()) {
                        alternative = alternative.thenChoose(stmt);
                      }
                      result = result.or(alternative);
                    }
                    return result;
                  } else {
                    return unifyUStatementWithSingleStatement(
                            getElseStatement(), ifTree.getElseStatement())
                        .apply(unifierAfterThen)
                        .transform(
                            unifierAfterElse ->
                                UnifierWithUnconsumedStatements.create(
                                    unifierAfterElse, unconsumedStatementsTail));
                  }
                });
    Choice<UnifierWithUnconsumedStatements> backwardMatch =
        getCondition()
            .negate()
            .unify(ifTree.getCondition(), unifier.fork())
            .thenChoose(
                unifierAfterCond -> {
                  if (getElseStatement() == null) {
                    return Choice.none();
                  }
                  return getElseStatement()
                      .apply(
                          UnifierWithUnconsumedStatements.create(
                              unifierAfterCond, List.of(ifTree.getThenStatement())))
                      .thenOption(
                          (UnifierWithUnconsumedStatements stateAfterThen) ->
                              stateAfterThen.unconsumedStatements().isEmpty()
                                  ? Optional.of(stateAfterThen.unifier())
                                  : Optional.<Unifier>absent());
                })
            .thenChoose(
                unifierAfterThen -> {
                  if (ifTree.getElseStatement() == null
                      && ControlFlowVisitor.INSTANCE.visitStatement(ifTree.getThenStatement())
                          == Result.ALWAYS_RETURNS) {
                    Choice<UnifierWithUnconsumedStatements> result =
                        getThenStatement()
                            .apply(
                                UnifierWithUnconsumedStatements.create(
                                    unifierAfterThen.fork(), unconsumedStatementsTail));
                    if (getThenStatement() instanceof UBlock) {
                      Choice<UnifierWithUnconsumedStatements> alternative =
                          Choice.of(
                              UnifierWithUnconsumedStatements.create(
                                  unifierAfterThen.fork(), unconsumedStatementsTail));
                      for (UStatement stmt : ((UBlock) getThenStatement()).getStatements()) {
                        alternative = alternative.thenChoose(stmt);
                      }
                      result = result.or(alternative);
                    }
                    return result;
                  } else {
                    return unifyUStatementWithSingleStatement(
                            getThenStatement(), ifTree.getElseStatement())
                        .apply(unifierAfterThen)
                        .transform(
                            unifierAfterElse ->
                                UnifierWithUnconsumedStatements.create(
                                    unifierAfterElse, unconsumedStatementsTail));
                  }
                });
    return forwardMatch.or(backwardMatch);
  }

  @Override
  public List<JCStatement> inlineStatements(Inliner inliner) throws CouldNotResolveImportException {
    return List.<JCStatement>of(
        inliner
            .maker()
            .If(
                getCondition().inline(inliner),
                Iterables.getOnlyElement(getThenStatement().inlineStatements(inliner)),
                (getElseStatement() == null)
                    ? null
                    : Iterables.getOnlyElement(getElseStatement().inlineStatements(inliner))));
  }
}
