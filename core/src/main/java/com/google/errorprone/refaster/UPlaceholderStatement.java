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

import com.google.auto.value.AutoValue;
import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.refaster.PlaceholderUnificationVisitor.State;
import com.google.errorprone.refaster.UPlaceholderExpression.UncheckedCouldNotResolveImportException;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;

/**
 * A representation of a block placeholder.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UPlaceholderStatement implements UStatement {
  static UPlaceholderStatement create(
      PlaceholderMethod placeholder,
      Iterable<? extends UExpression> arguments,
      ControlFlowVisitor.Result implementationFlow) {
    ImmutableList<UVariableDecl> placeholderParams = placeholder.parameters().asList();
    ImmutableList<UExpression> argumentsList = ImmutableList.copyOf(arguments);
    ImmutableMap.Builder<UVariableDecl, UExpression> builder = ImmutableMap.builder();
    for (int i = 0; i < placeholderParams.size(); i++) {
      builder.put(placeholderParams.get(i), argumentsList.get(i));
    }
    return new AutoValue_UPlaceholderStatement(placeholder, builder.build(), implementationFlow);
  }

  abstract PlaceholderMethod placeholder();

  abstract ImmutableMap<UVariableDecl, UExpression> arguments();

  abstract ControlFlowVisitor.Result implementationFlow();

  @Override
  public Kind getKind() {
    return Kind.OTHER;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitOther(this, data);
  }

  @AutoValue
  abstract static class ConsumptionState {
    static ConsumptionState empty() {
      return new AutoValue_UPlaceholderStatement_ConsumptionState(0, List.<JCStatement>nil());
    }

    abstract int consumedStatements();

    abstract List<JCStatement> placeholderImplInReverseOrder();

    ConsumptionState consume(JCStatement impl) {
      return new AutoValue_UPlaceholderStatement_ConsumptionState(
          consumedStatements() + 1, placeholderImplInReverseOrder().prepend(impl));
    }
  }

  public boolean reverify(Unifier unifier) {
    return MoreObjects.firstNonNull(
        new PlaceholderVerificationVisitor(
                Collections2.transform(
                    placeholder().requiredParameters(), Functions.forMap(arguments())),
                arguments().values())
            .scan(unifier.getBinding(placeholder().blockKey()), unifier),
        true);
  }

  @Override
  public Choice<UnifierWithUnconsumedStatements> apply(
      final UnifierWithUnconsumedStatements initState) {
    final PlaceholderUnificationVisitor visitor =
        PlaceholderUnificationVisitor.create(
            TreeMaker.instance(initState.unifier().getContext()), arguments());

    PlaceholderVerificationVisitor verification =
        new PlaceholderVerificationVisitor(
            Collections2.transform(
                placeholder().requiredParameters(), Functions.forMap(arguments())),
            arguments().values());

    // The choices where we might conceivably have a completed placeholder match.
    Choice<State<ConsumptionState>> realOptions = Choice.none();

    // The choice of consumption states to this point in the block.
    Choice<State<ConsumptionState>> choiceToHere =
        Choice.of(
            State.create(List.<UVariableDecl>nil(), initState.unifier(), ConsumptionState.empty()));

    if (verification.allRequiredMatched()) {
      realOptions = choiceToHere.or(realOptions);
    }
    for (final StatementTree targetStatement : initState.unconsumedStatements()) {
      if (!verification.scan(targetStatement, initState.unifier())) {
        break; // we saw a variable that's not allowed to be referenced
      }
      // Consume another statement, or if that fails, fall back to the previous choices...
      choiceToHere =
          choiceToHere.thenChoose(
              (final State<ConsumptionState> consumptionState) ->
                  visitor
                      .unifyStatement(targetStatement, consumptionState)
                      .transform(
                          (State<? extends JCStatement> stmtState) ->
                              stmtState.withResult(
                                  consumptionState.result().consume(stmtState.result()))));
      if (verification.allRequiredMatched()) {
        realOptions = choiceToHere.or(realOptions);
      }
    }
    return realOptions.thenOption(
        (State<ConsumptionState> consumptionState) -> {
          if (ImmutableSet.copyOf(consumptionState.seenParameters())
              .containsAll(placeholder().requiredParameters())) {
            Unifier resultUnifier = consumptionState.unifier().fork();
            int nConsumedStatements = consumptionState.result().consumedStatements();
            java.util.List<? extends StatementTree> remainingStatements =
                initState
                    .unconsumedStatements()
                    .subList(nConsumedStatements, initState.unconsumedStatements().size());
            UnifierWithUnconsumedStatements result =
                UnifierWithUnconsumedStatements.create(resultUnifier, remainingStatements);
            List<JCStatement> impl =
                consumptionState.result().placeholderImplInReverseOrder().reverse();
            ControlFlowVisitor.Result implFlow = ControlFlowVisitor.INSTANCE.visitStatements(impl);
            if (implFlow == implementationFlow()) {
              List<JCStatement> prevBinding = resultUnifier.getBinding(placeholder().blockKey());
              if (prevBinding != null && prevBinding.toString().equals(impl.toString())) {
                return Optional.of(result);
              } else if (prevBinding == null) {
                resultUnifier.putBinding(placeholder().blockKey(), impl);
                return Optional.of(result);
              }
            }
          }
          return Optional.absent();
        });
  }

  @Override
  public List<JCStatement> inlineStatements(final Inliner inliner)
      throws CouldNotResolveImportException {
    try {
      Optional<List<JCStatement>> binding = inliner.getOptionalBinding(placeholder().blockKey());

      // If a placeholder was used as an expression binding in the @BeforeTemplate,
      // and as a bare statement or as a return in the @AfterTemplate, we may need to convert.
      Optional<JCExpression> exprBinding = inliner.getOptionalBinding(placeholder().exprKey());
      binding =
          binding.or(
              exprBinding.transform(
                  (JCExpression expr) -> {
                    switch (implementationFlow()) {
                      case NEVER_EXITS:
                        return List.of((JCStatement) inliner.maker().Exec(expr));
                      case ALWAYS_RETURNS:
                        return List.of((JCStatement) inliner.maker().Return(expr));
                      default:
                        throw new AssertionError();
                    }
                  }));
      return UPlaceholderExpression.copier(arguments(), inliner).copy(binding.get(), inliner);
    } catch (UncheckedCouldNotResolveImportException e) {
      throw e.getCause();
    }
  }
}
