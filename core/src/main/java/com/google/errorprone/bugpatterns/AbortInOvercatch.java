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
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.LineMap;

import static com.sun.source.tree.Tree.Kind.EXPRESSION_STATEMENT;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

import java.util.List;

/**
 * A bug checker for abort in exception overcatch. See the following example: 
 *
 * <pre>
 * try {
 *   // stmt 1 throws exception B
 *   // stmt 2 throws exception A
 * } catch (Throwable t) {
 *   // more than one unique exceptions fall into this catch -- overcatch
 *   abort(); 
 * }
 * </pre>
 *
 * While it is a best practice to catch the precise exceptions, abort or System.exit()
 * in exception over-catches are particularly dangerous as it might accidentally
 * bring down the cluster on unexpected scenarios. 
 * 
 * Because it is difficult to check for the exceptions each statement throws in 
 * AST, we take the liberty to simplify our implementation: we simply check if the
 * catch block catches "Throwable" or "Exception", and then calls abort(). 
 * 
 * For more detail, refer to the paper:
 * "Simple Testing Can Prevent Most Critical Failures: 
 *  An Analysis of Production Failures in Distributed Data-intensive Systems"
 *  Yuan et al. Proceedings of the 11th Symposium on Operating Systems Design 
 *  and Implementation (OSDI), 2014
 *
 * @author yuan@eecg.utoronto.ca (Ding Yuan)
 */
@BugPattern(name = "AbortInOvercatch",
    summary = "Aborting the system after catching Throwable or Exception. "
            + "Because these exceptions are high-level exceptions, this might accidentally"
            + " take down the systems on unexpected exceptions.",
    explanation = "Aborting the system in Throwable or Exception is bad practice.",
    category = JDK, maturity = MATURE, severity = ERROR)
public class AbortInOvercatch extends BugChecker implements TryTreeMatcher {
  @Override
  public Description matchTry (TryTree tree, VisitorState state) {
    List<? extends CatchTree> catchList = tree.getCatches();
    if (catchList == null || catchList.size() == 0) {
      // TODO: this try block does not have a catch, we should further check the 
      // finally block!
      return Description.NO_MATCH;
    }
 
    CatchTree lastCatch = catchList.get(tree.getCatches().size() - 1);
    if (overcatch(lastCatch, state)) {
      if (abortInCatch(lastCatch, state)) {
        LineMap lineMap = state.getPath().getCompilationUnit().getLineMap();

        return describeMatch(lastCatch);
      }
    }
    return Description.NO_MATCH;
  }

  private boolean overcatch(CatchTree catchTree, VisitorState state) {    
    String caughtException = catchTree.getParameter().getType().toString();
    // System.out.println("DEBUG: Caught exception: " + caughtException);
    if (caughtException.equals("Throwable") || caughtException.equals("Exception")) {
      return true;
    }
    return false;
  }
  
  private boolean abortInCatch(CatchTree catchTree, VisitorState state) {
    List<? extends StatementTree> statements = catchTree.getBlock().getStatements();
    if (statements.isEmpty()) {
      return false;
    }
    StatementTree lastStmt = statements.get(statements.size() - 1);
    if (lastStmt.getKind() == EXPRESSION_STATEMENT) {
      ExpressionTree et = ((ExpressionStatementTree) lastStmt).getExpression();

      Symbol sym = ASTHelpers.getSymbol(et);
      if (sym == null || !(sym instanceof MethodSymbol)) {
        return false;
      }

      String methodName = sym.getQualifiedName().toString();
      String className = sym.owner.getQualifiedName().toString();

      // System.out.println("DEBUG: method: " + methodName + ", className: " + className);
      if (methodName.contains("abort") || 
             methodName.contains("shutdown") ||
             (methodName.equals("exit") && className.equals("java.lang.System"))) {
        return true;
      }
    }
    return false;
  }
}
