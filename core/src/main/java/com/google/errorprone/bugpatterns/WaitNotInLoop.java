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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.isDescendantOfMethod;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.not;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree.JCIf;

import java.util.Arrays;
import java.util.List;

/**
 * TODO(user): Doesn't handle the case that the enclosing method is intended to be called
 * in a loop.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(name = "WaitNotInLoop",
    summary = "Object.wait() should always be called in a loop",
    explanation = "Object.wait() can be woken up in multiple ways, none of which guarantee that " +
        "the condition it was waiting for has become true (spurious wakeups, for example). " +
        "Thus, Object.wait() should always be called in a loop that checks the condition " +
        "predicate.  Additionally, the loop should be inside a synchronized block or " +
        "method to avoid race conditions on the condition predicate.\n\n" +
        "See Java Concurrency in Practice section 14.2.2, \"Waking up too soon,\" and " +
        "[http://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#wait() " +
        "the Javadoc for Object.wait()].",
    category = JDK, severity = WARNING, maturity = MATURE)
public class WaitNotInLoop extends BugChecker implements MethodInvocationTreeMatcher {

  // Since some of the fixes have formatting problems, do not supply them unless explicitly enabled.
  private static final boolean SUPPLY_FIX = false;

  /**
   * Matches tree nodes that are enclosed in a loop before hitting a synchronized block or
   * method definition.
   */
  private static Matcher<Tree> inLoopBeforeSynchronizedMatcher = new Matcher<Tree>() {
    @Override
    public boolean matches(Tree t, VisitorState state) {
      TreePath path = state.getPath().getParentPath();
      Tree node = path.getLeaf();
      while (path != null) {
        if (node.getKind() == Kind.SYNCHRONIZED || node.getKind() == Kind.METHOD) {
          return false;
        }
        if (node.getKind() == Kind.WHILE_LOOP || node.getKind() == Kind.FOR_LOOP ||
            node.getKind() == Kind.ENHANCED_FOR_LOOP || node.getKind() == Kind.DO_WHILE_LOOP) {
          return true;
        }
        path = path.getParentPath();
        node = path.getLeaf();
      }
      return false;
    }
  };

  /**
   * Matches if:
   * 1) The method call is a call to any of Object.wait(), Object.wait(long), or
   *    Object.wait(long, int), and
   * 2) There is no enclosing loop before reaching a synchronized block or method declaration.
   */
  private static Matcher<MethodInvocationTree> waitMatcher = allOf(
        methodSelect(Matchers.<ExpressionTree>anyOf(
            isDescendantOfMethod("java.lang.Object", "wait()"),
            isDescendantOfMethod("java.lang.Object", "wait(long)"),
            isDescendantOfMethod("java.lang.Object", "wait(long,int)"))),
        not(inLoopBeforeSynchronizedMatcher));

  // TODO(user): Better suggested fixes.
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!waitMatcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    if (!SUPPLY_FIX) {
      return describeMatch(tree);
    }

    // if -> while case
    JCIf enclosingIf =
        ASTHelpers.findEnclosingNode(state.getPath().getParentPath(), JCIf.class);
    if (enclosingIf != null && enclosingIf.getElseStatement() == null) {
      // Assume first 2 characters of the IfTree are "if", replace with while.
      int startPos = enclosingIf.getStartPosition();
      return describeMatch(tree, SuggestedFix.replace(startPos, startPos + 2, "while"));
    }

    // loop outside synchronized block -> move synchronized outside
    List<Class<? extends StatementTree>> loopClasses = Arrays.asList(WhileLoopTree.class, ForLoopTree.class,
        EnhancedForLoopTree.class, DoWhileLoopTree.class);
    StatementTree enclosingLoop = null;
    for (Class<? extends StatementTree> loopClass : loopClasses) {
      enclosingLoop = ASTHelpers.findEnclosingNode(state.getPath().getParentPath(), loopClass);
      if (enclosingLoop != null) {
        break;
      }
    }
    if (enclosingLoop != null) {
      SynchronizedTree enclosingSynchronized = ASTHelpers.findEnclosingNode(
          state.getPath().getParentPath(), SynchronizedTree.class);
      if (enclosingSynchronized != null) {
        String blockStatements = enclosingSynchronized.getBlock().toString();
        int openBracketIndex = blockStatements.indexOf('{');
        int closeBracketIndex = blockStatements.lastIndexOf('}');
        blockStatements = blockStatements.substring(openBracketIndex + 1, closeBracketIndex).trim();
        return describeMatch(tree, SuggestedFix.builder()
            .replace(enclosingSynchronized, blockStatements)
            .prefixWith(enclosingLoop, "synchronized " + enclosingSynchronized.getExpression() + " {\n")
            .postfixWith(enclosingLoop, "\n}")
            .build());
      }
    }

    // Intent is to wait forever -> wrap in while (true)
    // Heuristic: this is the last statement in a method called main, inside a synchronized block.
    /*
    if (enclosingIf == null
        && (ASTHelpers.findEnclosingNode(state.getPath().getParentPath(), WhileLoopTree.class) == null)
        && (ASTHelpers.findEnclosingNode(state.getPath().getParentPath(), ForLoopTree.class) == null)
        && (ASTHelpers.findEnclosingNode(state.getPath().getParentPath(), EnhancedForLoopTree.class) == null)
        && (ASTHelpers.findEnclosingNode(state.getPath().getParentPath(), DoWhileLoopTree.class) == null)) {
      TreeMaker treeMaker = TreeMaker.instance(state.context);
      JCLiteral trueLiteral = treeMaker.Literal(true);
      treeMaker.WhileLoop(trueLiteral,
    }
    */

    return describeMatch(tree);
  }
}
