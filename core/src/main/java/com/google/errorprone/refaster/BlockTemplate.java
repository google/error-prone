/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.refaster;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.logging.Level.SEVERE;

import com.google.auto.value.AutoValue;
import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.refaster.UStatement.UnifierWithUnconsumedStatements;
import com.google.errorprone.refaster.annotation.UseImportPolicy;
import com.sun.source.tree.StatementTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Warner;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Template representing a sequence of consecutive statements.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
public abstract class BlockTemplate extends Template<BlockTemplateMatch> {
  private static final Logger logger = Logger.getLogger(BlockTemplate.class.toString());

  public static BlockTemplate create(UStatement... templateStatements) {
    return create(ImmutableMap.<String, UType>of(), templateStatements);
  }

  public static BlockTemplate create(
      Map<String, ? extends UType> expressionArgumentTypes, UStatement... templateStatements) {
    return create(ImmutableList.<UTypeVar>of(), expressionArgumentTypes, templateStatements);
  }

  public static BlockTemplate create(
      Iterable<UTypeVar> typeVariables,
      Map<String, ? extends UType> expressionArgumentTypes,
      UStatement... templateStatements) {
    return create(
        ImmutableClassToInstanceMap.<Annotation>builder().build(),
        typeVariables,
        expressionArgumentTypes,
        ImmutableList.copyOf(templateStatements));
  }

  public static BlockTemplate create(
      ImmutableClassToInstanceMap<Annotation> annotations,
      Iterable<UTypeVar> typeVariables,
      Map<String, ? extends UType> expressionArgumentTypes,
      Iterable<? extends UStatement> templateStatements) {
    return new AutoValue_BlockTemplate(
        annotations,
        ImmutableList.copyOf(typeVariables),
        ImmutableMap.copyOf(expressionArgumentTypes),
        ImmutableList.copyOf(templateStatements));
  }

  public BlockTemplate withStatements(Iterable<? extends UStatement> templateStatements) {
    return create(
        annotations(), templateTypeVariables(), expressionArgumentTypes(), templateStatements);
  }

  abstract ImmutableList<UStatement> templateStatements();

  /**
   * If the tree is a {@link JCBlock}, returns a list of disjoint matches corresponding to the exact
   * list of template statements found consecutively; otherwise, returns an empty list.
   */
  @Override
  public Iterable<BlockTemplateMatch> match(JCTree tree, Context context) {
    // TODO(lowasser): consider nonconsecutive matches?
    if (tree instanceof JCBlock) {
      JCBlock block = (JCBlock) tree;
      ImmutableList<JCStatement> targetStatements = ImmutableList.copyOf(block.getStatements());
      return matchesStartingAnywhere(block, 0, targetStatements, context)
          .first()
          .or(List.<BlockTemplateMatch>nil());
    }
    return ImmutableList.of();
  }

  private Choice<List<BlockTemplateMatch>> matchesStartingAtBeginning(
      final JCBlock block,
      final int offset,
      final ImmutableList<? extends StatementTree> statements,
      final Context context) {
    if (statements.isEmpty()) {
      return Choice.none();
    }
    final JCStatement firstStatement = (JCStatement) statements.get(0);
    Choice<UnifierWithUnconsumedStatements> choice =
        Choice.of(UnifierWithUnconsumedStatements.create(new Unifier(context), statements));
    for (UStatement templateStatement : templateStatements()) {
      choice = choice.thenChoose(templateStatement);
    }
    return choice.thenChoose(
        (UnifierWithUnconsumedStatements state) -> {
          Unifier unifier = state.unifier();
          Inliner inliner = unifier.createInliner();
          try {
            Optional<Unifier> checkedUnifier =
                typecheck(
                    unifier,
                    inliner,
                    new Warner(firstStatement),
                    expectedTypes(inliner),
                    actualTypes(inliner));
            if (checkedUnifier.isPresent()) {
              int consumedStatements = statements.size() - state.unconsumedStatements().size();
              BlockTemplateMatch match =
                  new BlockTemplateMatch(
                      block, checkedUnifier.get(), offset, offset + consumedStatements);
              boolean verified =
                  ExpressionTemplate.trueOrNull(
                      ExpressionTemplate.PLACEHOLDER_VERIFIER.scan(
                          templateStatements(), checkedUnifier.get()));
              if (!verified) {
                return Choice.none();
              }
              return matchesStartingAnywhere(
                      block,
                      offset + consumedStatements,
                      statements.subList(consumedStatements, statements.size()),
                      context)
                  .transform(list -> list.prepend(match));
            }
          } catch (CouldNotResolveImportException e) {
            // fall through
          }
          return Choice.none();
        });
  }

  private Choice<List<BlockTemplateMatch>> matchesStartingAnywhere(
      JCBlock block,
      int offset,
      final ImmutableList<? extends StatementTree> statements,
      final Context context) {
    Choice<List<BlockTemplateMatch>> choice = Choice.none();
    for (int i = 0; i < statements.size(); i++) {
      choice =
          choice.or(
              matchesStartingAtBeginning(
                  block, offset + i, statements.subList(i, statements.size()), context));
    }
    return choice.or(Choice.of(List.<BlockTemplateMatch>nil()));
  }

  /** Returns a {@code String} representation of a statement, including semicolon. */
  private static String printStatement(Context context, JCStatement statement) {
    StringWriter writer = new StringWriter();
    try {
      pretty(context, writer).printStat(statement);
    } catch (IOException e) {
      throw new AssertionError("StringWriter cannot throw IOExceptions");
    }
    return writer.toString();
  }

  /**
   * Returns a {@code String} representation of a sequence of statements, with semicolons and
   * newlines.
   */
  private static String printStatements(Context context, Iterable<JCStatement> statements) {
    StringWriter writer = new StringWriter();
    try {
      pretty(context, writer).printStats(com.sun.tools.javac.util.List.from(statements));
    } catch (IOException e) {
      throw new AssertionError("StringWriter cannot throw IOExceptions");
    }
    return writer.toString();
  }

  @Override
  public Fix replace(BlockTemplateMatch match) {
    checkNotNull(match);
    SuggestedFix.Builder fix = SuggestedFix.builder();
    Inliner inliner = match.createInliner();
    Context context = inliner.getContext();
    if (annotations().containsKey(UseImportPolicy.class)) {
      ImportPolicy.bind(context, annotations().getInstance(UseImportPolicy.class).value());
    } else {
      ImportPolicy.bind(context, ImportPolicy.IMPORT_TOP_LEVEL);
    }
    ImmutableList<JCStatement> targetStatements = match.getStatements();
    try {
      ImmutableList.Builder<JCStatement> inlinedStatementsBuilder = ImmutableList.builder();
      for (UStatement statement : templateStatements()) {
        inlinedStatementsBuilder.addAll(statement.inlineStatements(inliner));
      }
      ImmutableList<JCStatement> inlinedStatements = inlinedStatementsBuilder.build();
      int nInlined = inlinedStatements.size();
      int nTargets = targetStatements.size();
      if (nInlined <= nTargets) {
        for (int i = 0; i < nInlined; i++) {
          fix.replace(targetStatements.get(i), printStatement(context, inlinedStatements.get(i)));
        }
        for (int i = nInlined; i < nTargets; i++) {
          fix.delete(targetStatements.get(i));
        }
      } else {
        for (int i = 0; i < nTargets - 1; i++) {
          fix.replace(targetStatements.get(i), printStatement(context, inlinedStatements.get(i)));
        }
        int last = nTargets - 1;
        ImmutableList<JCStatement> remainingInlined = inlinedStatements.subList(last, nInlined);
        fix.replace(
            targetStatements.get(last),
            CharMatcher.whitespace().trimTrailingFrom(printStatements(context, remainingInlined)));
      }
    } catch (CouldNotResolveImportException e) {
      logger.log(SEVERE, "Failure to resolve import in replacement", e);
    }
    return addImports(inliner, fix);
  }
}
