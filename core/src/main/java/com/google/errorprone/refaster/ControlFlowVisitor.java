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

import static com.google.errorprone.refaster.ControlFlowVisitor.Result.ALWAYS_RETURNS;
import static com.google.errorprone.refaster.ControlFlowVisitor.Result.MAY_BREAK_OR_RETURN;
import static com.google.errorprone.refaster.ControlFlowVisitor.Result.NEVER_EXITS;

import com.google.errorprone.refaster.ControlFlowVisitor.BreakContext;
import com.google.errorprone.refaster.ControlFlowVisitor.Result;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.SimpleTreeVisitor;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Name;

/**
 * Analyzes a series of statements to determine whether they don't, sometimes, or never return.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
class ControlFlowVisitor extends SimpleTreeVisitor<Result, BreakContext> {
  public static final ControlFlowVisitor INSTANCE = new ControlFlowVisitor();

  /**
   * The state of whether a sequence of statements may return, break out of the visited statements,
   * or neither.
   */
  enum Result {
    NEVER_EXITS {
      @Override
      Result or(Result other) {
        switch (other) {
          case MAY_BREAK_OR_RETURN:
          case NEVER_EXITS:
            return other;
          default:
            return MAY_RETURN;
        }
      }

      @Override
      Result then(Result other) {
        return other;
      }
    },
    MAY_BREAK_OR_RETURN {
      @Override
      Result or(Result other) {
        return MAY_BREAK_OR_RETURN;
      }

      @Override
      Result then(Result other) {
        return MAY_BREAK_OR_RETURN;
      }
    },
    MAY_RETURN {
      @Override
      Result or(Result other) {
        return (other == MAY_BREAK_OR_RETURN) ? MAY_BREAK_OR_RETURN : MAY_RETURN;
      }

      @Override
      Result then(Result other) {
        switch (other) {
          case MAY_BREAK_OR_RETURN:
          case ALWAYS_RETURNS:
            return other;
          default:
            return MAY_RETURN;
        }
      }
    },
    ALWAYS_RETURNS {

      @Override
      Result or(Result other) {
        switch (other) {
          case MAY_BREAK_OR_RETURN:
          case ALWAYS_RETURNS:
            return other;
          default:
            return MAY_RETURN;
        }
      }

      @Override
      Result then(Result other) {
        return ALWAYS_RETURNS;
      }
    };

    abstract Result or(Result other);

    abstract Result then(Result other);
  }

  static class BreakContext {
    final Set<Name> internalLabels;
    int loopDepth;

    private BreakContext() {
      this.internalLabels = new HashSet<>();
      this.loopDepth = 0;
    }

    void enter(Name label) {
      internalLabels.add(label);
    }

    void exit(Name label) {
      internalLabels.remove(label);
    }
  }

  private ControlFlowVisitor() {}

  public Result visitStatement(StatementTree node) {
    return node.accept(this, new BreakContext());
  }

  public Result visitStatements(Iterable<? extends StatementTree> nodes) {
    return visitStatements(nodes, new BreakContext());
  }

  private Result visitStatements(Iterable<? extends StatementTree> nodes, BreakContext cxt) {
    Result result = NEVER_EXITS;
    for (StatementTree node : nodes) {
      result = result.then(node.accept(this, cxt));
    }
    return result;
  }

  @Override
  protected Result defaultAction(Tree node, BreakContext cxt) {
    return NEVER_EXITS;
  }

  @Override
  public Result visitBlock(BlockTree node, BreakContext cxt) {
    return visitStatements(node.getStatements(), cxt);
  }

  @Override
  public Result visitDoWhileLoop(DoWhileLoopTree node, BreakContext cxt) {
    cxt.loopDepth++;
    try {
      return node.getStatement().accept(this, cxt).or(NEVER_EXITS);
    } finally {
      cxt.loopDepth--;
    }
  }

  @Override
  public Result visitWhileLoop(WhileLoopTree node, BreakContext cxt) {
    cxt.loopDepth++;
    try {
      return node.getStatement().accept(this, cxt).or(NEVER_EXITS);
    } finally {
      cxt.loopDepth--;
    }
  }

  @Override
  public Result visitForLoop(ForLoopTree node, BreakContext cxt) {
    cxt.loopDepth++;
    try {
      return node.getStatement().accept(this, cxt).or(NEVER_EXITS);
    } finally {
      cxt.loopDepth--;
    }
  }

  @Override
  public Result visitEnhancedForLoop(EnhancedForLoopTree node, BreakContext cxt) {
    cxt.loopDepth++;
    try {
      return node.getStatement().accept(this, cxt).or(NEVER_EXITS);
    } finally {
      cxt.loopDepth--;
    }
  }

  @Override
  public Result visitSwitch(SwitchTree node, BreakContext cxt) {
    Result result = null;
    boolean seenDefault = false;
    cxt.loopDepth++;
    try {
      for (CaseTree caseTree : node.getCases()) {
        if (caseTree.getExpression() == null) {
          seenDefault = true;
        }

        if (result == null) {
          result = caseTree.accept(this, cxt);
        } else {
          result = result.or(caseTree.accept(this, cxt));
        }
      }
      if (!seenDefault) {
        result = result.or(NEVER_EXITS);
      }
      return result;
    } finally {
      cxt.loopDepth--;
    }
  }

  @Override
  public Result visitCase(CaseTree node, BreakContext cxt) {
    return visitStatements(node.getStatements(), cxt);
  }

  @Override
  public Result visitSynchronized(SynchronizedTree node, BreakContext cxt) {
    return node.getBlock().accept(this, cxt);
  }

  @Override
  public Result visitTry(TryTree node, BreakContext cxt) {
    Result result = node.getBlock().accept(this, cxt);
    for (CatchTree catchTree : node.getCatches()) {
      result = result.or(catchTree.accept(this, cxt));
    }
    if (node.getFinallyBlock() != null) {
      result = result.then(node.getFinallyBlock().accept(this, cxt));
    }
    return result;
  }

  @Override
  public Result visitCatch(CatchTree node, BreakContext cxt) {
    return node.getBlock().accept(this, cxt);
  }

  @Override
  public Result visitIf(IfTree node, BreakContext cxt) {
    Result thenResult = node.getThenStatement().accept(this, cxt);
    Result elseResult =
        (node.getElseStatement() == null) ? NEVER_EXITS : node.getElseStatement().accept(this, cxt);
    return thenResult.or(elseResult);
  }

  @Override
  public Result visitExpressionStatement(ExpressionStatementTree node, BreakContext cxt) {
    return NEVER_EXITS;
  }

  @Override
  public Result visitLabeledStatement(LabeledStatementTree node, BreakContext cxt) {
    cxt.enter(node.getLabel());
    try {
      return node.getStatement().accept(this, cxt);
    } finally {
      cxt.exit(node.getLabel());
    }
  }

  @Override
  public Result visitBreak(BreakTree node, BreakContext cxt) {
    if (cxt.internalLabels.contains(node.getLabel())
        || (node.getLabel() == null && cxt.loopDepth > 0)) {
      return NEVER_EXITS;
    } else {
      return MAY_BREAK_OR_RETURN;
    }
  }

  @Override
  public Result visitContinue(ContinueTree node, BreakContext cxt) {
    if (cxt.internalLabels.contains(node.getLabel())
        || (node.getLabel() == null && cxt.loopDepth > 0)) {
      return NEVER_EXITS;
    } else {
      return MAY_BREAK_OR_RETURN;
    }
  }

  @Override
  public Result visitReturn(ReturnTree node, BreakContext cxt) {
    return ALWAYS_RETURNS;
  }

  @Override
  public Result visitThrow(ThrowTree node, BreakContext cxt) {
    return ALWAYS_RETURNS;
  }
}
