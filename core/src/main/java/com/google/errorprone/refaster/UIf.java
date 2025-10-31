/*
 * Copyright 2013 The Error Prone Authors.
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.refaster.ControlFlowVisitor.Result;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

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
  public abstract @Nullable UStatement getElseStatement();

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitIf(this, data);
  }

  @Override
  public Kind getKind() {
    return Kind.IF;
  }

  private static Function<Unifier, Choice<Unifier>> unifyUStatementWithSingleStatement(
      @Nullable UStatement toUnify, @Nullable StatementTree target) {
    return (Unifier unifier) -> {
      if (toUnify == null) {
        return (target == null) ? Choice.of(unifier) : Choice.<Unifier>none();
      }
      List<StatementTree> list = (target == null) ? List.<StatementTree>nil() : List.of(target);
      return toUnify
          .apply(UnifierWithUnconsumedStatements.create(unifier, list))
          .filter(s -> s.unconsumedStatements().isEmpty())
          .map(UnifierWithUnconsumedStatements::unifier);
    };
  }

  @Override
  public @Nullable Choice<UnifierWithUnconsumedStatements> apply(
      UnifierWithUnconsumedStatements state) {
    ImmutableList<? extends StatementTree> unconsumedStatements = state.unconsumedStatements();
    if (unconsumedStatements.isEmpty()) {
      return Choice.none();
    }
    ImmutableList<? extends StatementTree> unconsumedStatementsTail =
        unconsumedStatements.subList(1, unconsumedStatements.size());
    StatementTree firstStatement = unconsumedStatements.get(0);
    if (!(firstStatement instanceof IfTree ifTree)) {
      return Choice.none();
    }
    Unifier unifier = state.unifier();
    Choice<UnifierWithUnconsumedStatements> forwardMatch =
        getCondition()
            .unify(ifTree.getCondition(), unifier.fork())
            .flatMap(
                unifyUStatementWithSingleStatement(getThenStatement(), ifTree.getThenStatement()))
            .flatMap(
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
                    if (getElseStatement() instanceof UBlock uBlock) {
                      Choice<UnifierWithUnconsumedStatements> alternative =
                          Choice.of(
                              UnifierWithUnconsumedStatements.create(
                                  unifierAfterThen.fork(), unconsumedStatementsTail));
                      for (UStatement stmt : uBlock.getStatements()) {
                        alternative = alternative.flatMap(stmt);
                      }
                      result = result.concat(alternative);
                    }
                    return result;
                  } else {
                    return unifyUStatementWithSingleStatement(
                            getElseStatement(), ifTree.getElseStatement())
                        .apply(unifierAfterThen)
                        .map(
                            unifierAfterElse ->
                                UnifierWithUnconsumedStatements.create(
                                    unifierAfterElse, unconsumedStatementsTail));
                  }
                });
    Choice<UnifierWithUnconsumedStatements> backwardMatch =
        getCondition()
            .negate()
            .unify(ifTree.getCondition(), unifier.fork())
            .flatMap(
                unifierAfterCond -> {
                  if (getElseStatement() == null) {
                    return Choice.none();
                  }
                  return getElseStatement()
                      .apply(
                          UnifierWithUnconsumedStatements.create(
                              unifierAfterCond, List.of(ifTree.getThenStatement())))
                      .mapIfPresent(
                          (UnifierWithUnconsumedStatements stateAfterThen) ->
                              stateAfterThen.unconsumedStatements().isEmpty()
                                  ? Optional.of(stateAfterThen.unifier())
                                  : Optional.<Unifier>empty());
                })
            .flatMap(
                unifierAfterThen -> {
                  if (ifTree.getElseStatement() == null
                      && ControlFlowVisitor.INSTANCE.visitStatement(ifTree.getThenStatement())
                          == Result.ALWAYS_RETURNS) {
                    Choice<UnifierWithUnconsumedStatements> result =
                        getThenStatement()
                            .apply(
                                UnifierWithUnconsumedStatements.create(
                                    unifierAfterThen.fork(), unconsumedStatementsTail));
                    if (getThenStatement() instanceof UBlock uBlock) {
                      Choice<UnifierWithUnconsumedStatements> alternative =
                          Choice.of(
                              UnifierWithUnconsumedStatements.create(
                                  unifierAfterThen.fork(), unconsumedStatementsTail));
                      for (UStatement stmt : uBlock.getStatements()) {
                        alternative = alternative.flatMap(stmt);
                      }
                      result = result.concat(alternative);
                    }
                    return result;
                  } else {
                    return unifyUStatementWithSingleStatement(
                            getThenStatement(), ifTree.getElseStatement())
                        .apply(unifierAfterThen)
                        .map(
                            unifierAfterElse ->
                                UnifierWithUnconsumedStatements.create(
                                    unifierAfterElse, unconsumedStatementsTail));
                  }
                });
    return forwardMatch.concat(backwardMatch);
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
