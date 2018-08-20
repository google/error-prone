/*
 * Copyright 2017 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ForLoopTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import com.sun.tools.javac.code.Symbol;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** @author mariasam@google.com (Maria Sam) */
@BugPattern(
    name = "IncrementInForLoopAndHeader",
    summary = "This for loop increments the same variable in the header and in the body",
    category = JDK,
    severity = WARNING)
public class IncrementInForLoopAndHeader extends BugChecker implements ForLoopTreeMatcher {

  private static final ImmutableSet<Kind> CONDITIONALS =
      ImmutableSet.of(
          Kind.IF, Kind.DO_WHILE_LOOP, Kind.WHILE_LOOP, Kind.FOR_LOOP, Kind.ENHANCED_FOR_LOOP);

  @Override
  public Description matchForLoop(ForLoopTree forLoopTree, VisitorState visitorState) {
    List<? extends ExpressionStatementTree> updates = forLoopTree.getUpdate();

    // keep track of all the symbols that are updated in the for loop header
    final Set<Symbol> incrementedSymbols =
        updates.stream()
            .filter(expStateTree -> expStateTree.getExpression() instanceof UnaryTree)
            .map(
                expStateTree ->
                    ASTHelpers.getSymbol(
                        ((UnaryTree) expStateTree.getExpression()).getExpression()))
            .collect(Collectors.toCollection(HashSet::new));

    // track if they are updated in the body without a conditional surrounding them
    StatementTree body = forLoopTree.getStatement();
    List<? extends StatementTree> statementTrees =
        body instanceof BlockTree ? ((BlockTree) body).getStatements() : ImmutableList.of(body);
    for (StatementTree s : statementTrees) {
      if (!CONDITIONALS.contains(s.getKind())) {
        Optional<Symbol> opSymbol = returnUnarySym(s);
        if (opSymbol.isPresent() && incrementedSymbols.contains(opSymbol.get())) {
          // both ++ and --
          return describeMatch(forLoopTree);
        }
      }
    }
    return Description.NO_MATCH;
  }

  private static Optional<Symbol> returnUnarySym(StatementTree s) {
    if (s instanceof ExpressionStatementTree) {
      if (((ExpressionStatementTree) s).getExpression() instanceof UnaryTree) {
        UnaryTree unaryTree = (UnaryTree) ((ExpressionStatementTree) s).getExpression();
        return Optional.ofNullable(ASTHelpers.getSymbol(unaryTree.getExpression()));
      }
    }
    return Optional.empty();
  }
}
