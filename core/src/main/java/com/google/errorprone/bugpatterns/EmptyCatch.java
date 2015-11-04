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

import static com.sun.source.tree.Tree.Kind;
import static com.sun.source.tree.Tree.Kind.RETURN;
import static com.sun.source.tree.Tree.Kind.CONTINUE;
import static com.sun.source.tree.Tree.Kind.BREAK;
import static com.sun.source.tree.Tree.Kind.ASSIGNMENT;
import static com.sun.source.tree.Tree.Kind.EXPRESSION_STATEMENT;
import static com.sun.source.tree.Tree.Kind.EMPTY_STATEMENT;
import static com.sun.source.tree.Tree.Kind.CATCH;
import static com.sun.source.tree.Tree.Kind.BLOCK;
import static com.sun.source.tree.Tree.Kind.IF;
import static com.sun.source.tree.Tree.Kind.FOR_LOOP;
import static com.sun.source.tree.Tree.Kind.DO_WHILE_LOOP;
import static com.sun.source.tree.Tree.Kind.WHILE_LOOP;
import static com.sun.source.tree.Tree.Kind.ENHANCED_FOR_LOOP;
import static com.sun.source.tree.Tree.Kind.SWITCH;
import static com.sun.source.tree.Tree.Kind.TRY;
import static com.sun.source.tree.Tree.Kind.VARIABLE;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;

import java.util.List;

/**
 * A bug checker for empty exception handling block:
 *
 * <pre>
 * try {
 *   // do something
 * } catch (XX e) {
 *   // empty, only comment, or only log printing statements
 * }
 * </pre>
 * We also implement a variety of false positive suppression heuristics.
 * 
 * For more detail, refer to the paper:
 * "Simple Testing Can Prevent Most Critical Failures: 
 *  An Analysis of Production Failures in Distributed Data-intensive Systems"
 *  Yuan et al. Proceedings of the 11th USENIX Symposium on Operating Systems Design 
 *  and Implementation (OSDI), 2014
 *
 * @author yuan@eecg.utoronto.ca (Ding Yuan)
 */
@BugPattern(name = "EmptyCatch",
    summary = "Empty catch blocks that ignore exceptions",
    explanation = "Ignoring exception is a bad practice.",
    category = JDK, maturity = MATURE, severity = WARNING)
public class EmptyCatch extends BugChecker implements TryTreeMatcher {
  /* If one of the following exceptions is ignored, we suppress the warning. */
  String[] harmlessExceptions = {
    "FileNotFoundException", "ReflectiveOperationException", 
    "ClassNotFoundException", "InvocationTargetException", 
    "NoSuchFieldException", "NoSuchMethodException", "InstantiationException",
    "InvocationTargetException"
  };
  
  /* If the method is one of the following, we allow it to ignore exceptions. */
  String[] whitelistedMethods = {
    "cleanup", "dispose", "shutdown", "close", "stop"
  };

  @Override
  public Description matchTry (TryTree tree, VisitorState state) {
    if (badEmptyCatchBlock(tree, state)) {
      // If it has finally block, assume it's OK
      BlockTree bt = tree.getFinallyBlock();
      if (bt == null || bt.getStatements().size() == 0) {
        CatchTree lastCatch = tree.getCatches().get(tree.getCatches().size() - 1);
        LineMap lineMap = state.getPath().getCompilationUnit().getLineMap();

        return describeMatch(lastCatch);

      }
    }
    return Description.NO_MATCH;
  }

  private boolean badEmptyCatchBlock(TryTree tryTree, VisitorState state) {    
    List<? extends CatchTree> catchList = tryTree.getCatches();
    if (catchList == null || catchList.size() == 0) {
      // TODO: this try block does not have a catch, we should further check the 
      // finally block!
      return false;
    }
    CatchTree catchTree = catchList.get(catchList.size() - 1);

    /* If there are multiple catch blocks with a try tree, we only check the last catch block. */
    if (isCatchEmpty(catchTree, state)) {
      if (exceptionWhitelisted(catchTree, state)) {
        // System.out.println("   *** Not a problem b/c the excpetion is harmless.");
        return false;
      }

      if (handledByControlflow(tryTree, state)) {
        // System.out.println("   *** Not a problem b/c the excpetion is handled by control flow.");
        return false;
      }
      
      if (alreadyInCatch(tryTree, state)) {
        // System.out.println("   *** Not a problem b/c the try block is already in error handling block.");
        return false;
      }
      
      if (handledByDataflow(tryTree, state)) {
        // System.out.println("   *** Not a problem b/c handled by data-flow.");
        return false;
      }
      
      /* We check if this method is a white listed method. If so, we allow 
       * it to ignore exceptions. */
      if (methodWhitelisted(tryTree, state)) {
        // System.out.println("   *** Not a problem b/c the method is whitelisted.");
        return false;
      }
      return true;
    }
 
    return false;
  }

  /*
   * This method checks if the try/catch is already in a catch block; if so, 
   * it will return true and causing the checker NOT reporting it as an error.
   * 
   * The reason is that we observe the following code pattern:
   *    } catch (IOException e) {
   *      try {
   *        saslClient.dispose();
   *      } catch (SaslException ignored) {
   *        // ignore further exceptions during cleanup
   *      }
   *      throw e;
   *    }
   */
  private boolean alreadyInCatch(TryTree tryTree, VisitorState state) {
    TreePath enclosing = state.getPath().getParentPath();
    Kind kind = enclosing.getLeaf().getKind();
    while (kind == BLOCK || kind == IF || kind == WHILE_LOOP || kind == SWITCH
        || kind == FOR_LOOP || kind == DO_WHILE_LOOP || kind == ENHANCED_FOR_LOOP) {
      enclosing = enclosing.getParentPath();
      kind = enclosing.getLeaf().getKind();
    }
  
    if (kind == CATCH) {
      // OK, we are already in a catch block
      return true;
    }
 
    /* We should also ignore the empty catch block in a finally block. However,
     * there is no com.sun.source.tree.Tree.Kind.FINALLY kind, and the finally
     * block has a kind = TRY, therefore, we will simply ignore the empty catch
     * blocks from try as well! */
    if (kind == TRY) {
      // System.out.println("DEBUG: The parent of the try tree is TRY or FINALLY, will ignore!");
      return true;
    }
    // System.out.println("DEBUG: parent of the try tree, kind: " + kind);
      
    return false;
  }
  
  /* This method checks if the exception ignored in catch block is handled by
   * control-flow change. Specifically, the following code pattern:
   * 
   *  try {
   *    // do something
   *    return; // or break, continue
   *  } catch (Exception e) {
   *    // empty
   *  }
   *  // actual handling logic. 
   *  
   *  should not result in a warning from error-prone. */
  private boolean handledByControlflow (TryTree tryTree, VisitorState state) {
    List<? extends StatementTree> statements = tryTree.getBlock().getStatements();
    if (statements.isEmpty()) {
      return false;
    }
    Kind lastTrystmtKind = statements.get(statements.size() - 1).getKind();
    // System.out.println("DEBUG: control flow check; last stmt in try: " + lastTrystmtKind);
    if (lastTrystmtKind == RETURN ||
           lastTrystmtKind == CONTINUE ||
           lastTrystmtKind == BREAK) {
      return true; // it is handled by control flow!
      /* TODO: to be more accurate, we should further check if the basic block
       * after this try is not empty. 
       */
      }
    return false;
  }

  private boolean methodWhitelisted (TryTree tryTree, VisitorState state) {
    MethodTree enclosingMethod = state.findEnclosing(MethodTree.class);
    if (enclosingMethod != null) {
      String enclosingMethodName = enclosingMethod.getName().toString();
      for (String whitelistedMethod : whitelistedMethods) {
        if (enclosingMethodName.contains(whitelistedMethod)) {
          // System.out.println("DEBUG: whitelisted method: " + enclosingMethodName + " ignores exception!");
          return true; // it's OK for a cleanup method to ignore exceptions...
        }
      }
    }
    
    /* Next: try to check the methods in the tryTree to see if they're
     * whitelisted ones. In particular, we will only check a tryTree with less
     * or equal to 2 statements, because mostly the pattern is:
     * try {
     *   shutdown();
     *  } catch(..) {//empty} */
    List<? extends StatementTree> statements = tryTree.getBlock().getStatements();
    if (statements.isEmpty()) {
      return true;
    }

    if (statements.size() < 3) {
      String lastTrystmt = statements.get(statements.size() - 1).toString();
      for (String whitelistedMethod : whitelistedMethods) {
        if (lastTrystmt.contains(whitelistedMethod)) {
           // System.out.println("DEBUG: whitelisted method as invocation target: " + lastTrystmt + " ignores exception!");
           return true; // it's OK for a cleanup method to ignore exceptions...
        }
      }
    }
    return false;
  }
  
  /* Checks if this exception is harmless (white listed)*/
  private boolean exceptionWhitelisted (CatchTree catchTree, VisitorState state) {
    /* We further check the exception type, and ignore these exceptions. */
    String caughtException = catchTree.getParameter().toString();
    boolean canBeIgnored = false;
    for (String harmlessExp : harmlessExceptions) {
      if (caughtException.contains(harmlessExp)) {
        canBeIgnored = true;
        break;
      }
    }
    if (canBeIgnored) {
      // System.out.println("DEBUG: found empty handler for an harmless exception: " + caughtException);
      return true;
    }
    // System.out.println("DEBUG: catchType = " + caughtException);
    return false;
  }

  private boolean isCatchEmpty(CatchTree catchTree, VisitorState state) {
    List<? extends StatementTree> statements = catchTree.getBlock().getStatements();
    if (statements.isEmpty()) {
      // System.out.println("DEBUG: found an purely empty catch block " + catchTree);
      return true;
    }
 
    for (StatementTree stmt : statements) {
      // System.out.println("DEBUG: statement in catch: " + stmt + ", kind: " + stmt.getKind());

      if (isLoggingStmt(stmt, state)) {
        // System.out.println("DEBUG: logging statement: " + stmt);
        continue;
      } else if (stmt.getKind() == EMPTY_STATEMENT) {
        continue;
      }
      // if we falls to here, it means that this statement is neither a logging
      // stmt or an empty one, therefore, it's a meaningful stmt!
      // System.out.println("DEBUG: statement: " + stmt + " is a meaningful stmt!");
      return false;
    }
    // We didn't return from the loop body above, so this is an empty
    // catch block or only contains logging stmt!
    return true;
  }
  
  /* returns true if the statement is a logging statement. */
  private boolean isLoggingStmt(StatementTree stmt, VisitorState state) {
    // Turns out, a logging statement is first an expression_stmt
    if (stmt.getKind() == EXPRESSION_STATEMENT) {
    ExpressionTree et = ((ExpressionStatementTree) stmt).getExpression();

    /* System.out.println("stmt: " + stmt + " is EXPRESSION_STMT, expression: " 
        + et + ", et.kind: " + et.getKind()); */
    Symbol sym = ASTHelpers.getSymbol(et);
    if (sym == null || !(sym instanceof MethodSymbol)) {
      return false;
    }
     
    String methodName = sym.getQualifiedName().toString();
    String className = sym.owner.getQualifiedName().toString();
          
    // System.out.println("  methodName: " + methodName + ", className: " + className);
    return ((methodName.contains("print")
    		&& className.equals("java.io.PrintStream"))
    		|| 
    		((methodName.equals("debug") 
          || methodName.equals("error")
          || methodName.equals("info")
          || methodName.equals("warn")
          || methodName.equals("trace"))
          && (className.contains("log") // this includes log4j
              || className.contains("sl4j")))); // we assume "fatal" is a proper handling...
    }
    return false;
  }
  
  /* This method checks if the exception ignored in catch block is handled by
   * data-flow. Specifically, the following code pattern:
   * 
   *  try {
   *    // do something that might thrown exception
   *    a = 10; 
   *  } catch (Exception e) {
   *    // empty
   *  }
   *  if (a != 10) {
   *    // actual handling logic
   *  } 
   *  
   *  should not result in a warning from error-prone. */
  private boolean handledByDataflow (TryTree tryTree, VisitorState state) {
    List<? extends StatementTree> statements = tryTree.getBlock().getStatements();
    if (statements.isEmpty()) {
      return false;
    }
    int index = statements.size() - 1;
      StatementTree lastTrystmt = statements.get(index);
      while (isLoggingStmt (lastTrystmt, state) || 
          lastTrystmt.getKind() == IF) {
        index--;
        if (index < 0) {
          // We reached the beginning of the try and cannot find any meaningful statement!
          return true;
        }
        lastTrystmt = statements.get(index);
      }
      if (lastTrystmt.getKind() == EXPRESSION_STATEMENT) {
        // ((EXPRESSION_STATEMENT) lastTrystmt)
        ExpressionTree et = ((ExpressionStatementTree) lastTrystmt).getExpression();
        if (et.getKind() == ASSIGNMENT) {
          // it is an assignment statement...
          return true;
        }
      } else if (lastTrystmt.getKind() == VARIABLE) {
        // This could be a vairable declaration followed by assignment
        return true;
      }
      // System.out.println("DEBUG: data-flow check, the last stmt in try: " + lastTrystmt + ", KIND: " + lastTrystmt.getKind());
      return false;
  }
}
