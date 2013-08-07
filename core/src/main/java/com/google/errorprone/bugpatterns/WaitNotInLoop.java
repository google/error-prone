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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isDescendantOfMethod;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.not;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;

/**
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
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class WaitNotInLoop extends DescribingMatcher<MethodInvocationTree> {

  /**
   * Matches if:
   * 1) The method call is a call to any of Object.wait(), Object.wait(long), or
   *    Object.wait(long, int), and
   * 2) There is no enclosing loop before reaching a synchronized block or method declaration.
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean matches(MethodInvocationTree t, VisitorState state) {
    return allOf(
        methodSelect(anyOf(
            isDescendantOfMethod("java.lang.Object", "wait()"),
            isDescendantOfMethod("java.lang.Object", "wait(long)"),
            isDescendantOfMethod("java.lang.Object", "wait(long,int)"))),
        not(inLoopBeforeSynchronizedMatcher))
        .matches(t, state);
  }

  /**
   * Matches tree nodes that are enclosed in a loop before hitting a synchronized block or
   * method definition.
   */
  private Matcher<Tree> inLoopBeforeSynchronizedMatcher = new Matcher<Tree>() {
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
   * TODO(eaftan): Proper fix.  My guess is that you want to look for an enclosing if statement
   * and replace it with a while.
   */
  @Override
  public Description describe(MethodInvocationTree methodInvocationTree, VisitorState state) {
    return new Description(methodInvocationTree, getDiagnosticMessage(), new SuggestedFix().delete(methodInvocationTree));
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    public DescribingMatcher<MethodInvocationTree> matcher = new WaitNotInLoop();

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitMethodInvocation(node, visitorState);
    }
  }

}
