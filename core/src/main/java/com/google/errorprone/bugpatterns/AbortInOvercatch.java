/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.anyMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TryTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import static com.sun.source.tree.Tree.Kind.EXPRESSION_STATEMENT;

import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;

import java.util.List;
import java.util.regex.Pattern;



/**
 * A bug checker for abort in exception overcatch. See the following example: 
 *
 * <pre>
 * {@code
 * try {
 *   // stmt 1 throws exception B
 *   // stmt 2 throws exception A
 * } catch (Throwable t) {
 *   // more than one unique exceptions fall into this catch -- overcatch
 *   abort(); 
 * }
 * }
 * </pre>
 *
 * While it is a best practice to catch the precise exceptions, abort or System.exit()
 * in exception over-catches are particularly dangerous as it might accidentally
 * bring down the cluster on unexpected scenarios. 
 * <p>
 * Because it is difficult to check for the exceptions each statement throws in 
 * AST, we take the liberty to simplify our implementation: we simply check if the
 * catch block catches "Throwable" or "Exception", and then calls abort(). This is
 * useful even if there is only one exception type can fall through to such catch blocks
 * as later code evolutions might enable more exceptions and result in an over catch. 
 * <p>
 * For more detail, refer to the paper:
 * "Simple Testing Can Prevent Most Critical Failures: 
 *  An Analysis of Production Failures in Distributed Data-intensive Systems"
 *  Yuan et al. Proceedings of the 11th Symposium on Operating Systems Design 
 *  and Implementation (OSDI), 2014
 *
 * @author yuan@eecg.utoronto.ca (Ding Yuan)
 */
@BugPattern(name = "AbortInOvercatch",
    summary = "Exiting from a catch block that cathces overly-broad types can cause the "
            + "program to exit when an unexpected exception is thrown",
    explanation = "Exiting from a catch block that cathces overly-broad types (i.e. "
            + "Throwable or Exception) can cause the program to exit when an unexpected "
            + "exception is thrown. This can be particularly deadly in systems where "
            + "high availability is desired.\n\n" 
            + "Even when there is only a single exception can possibly fall into such "
            + "`catch` block, later code evolutions might enable more exceptions and "
            + "result in unexpected exit.\n\n"
            + "To fix this, you usually want to catch the precise exception rather than "
            + "Throwable or Exception when existing. ",
    category = JDK, maturity = EXPERIMENTAL, severity = WARNING)
public class AbortInOvercatch extends BugChecker implements TryTreeMatcher {
  private static final Matcher<ExpressionTree> ABORT_MATCHER = anyOf(
      staticMethod().onClass("java.lang.System").named("exit"),
      anyMethod().anyClass().withNameMatching(Pattern.compile(".*(?i:abort).*")),
      anyMethod().anyClass().withNameMatching(Pattern.compile(".*(?i:shutdown).*")));

  @Override
  public Description matchTry (TryTree tree, VisitorState state) {
    List<? extends CatchTree> catchList = tree.getCatches();
    if (catchList == null || catchList.size() == 0) {
      // TODO: this try block does not have a catch, we should further check the 
      // finally block!
      return Description.NO_MATCH;
    }
 
    CatchTree lastCatch = Iterables.getLast(catchList);
    if (overcatch(lastCatch, state)) {
      if (abortInCatch(lastCatch, state)) {
        return describeMatch(lastCatch);
      }
    }
    return Description.NO_MATCH;
  }

  private boolean overcatch(CatchTree catchTree, VisitorState state) {
    Types types = state.getTypes();
    Symtab symtab = state.getSymtab();
    Type paramType = ASTHelpers.getType(catchTree.getParameter().getType());
    return (types.isSameType(paramType, symtab.throwableType) || types.isSameType(paramType, symtab.exceptionType));
  }
  
  private boolean abortInCatch(CatchTree catchTree, VisitorState state) {
    List<? extends StatementTree> statements = catchTree.getBlock().getStatements();
    if (statements.isEmpty()) {
      return false;
    }
    StatementTree lastStmt = statements.get(statements.size() - 1);
    if (lastStmt.getKind() == EXPRESSION_STATEMENT) {
      ExpressionTree et = ((ExpressionStatementTree) lastStmt).getExpression();

      if (ABORT_MATCHER.matches(et, state)) {
        return true;
      }
    }
    return false;
  }
}
