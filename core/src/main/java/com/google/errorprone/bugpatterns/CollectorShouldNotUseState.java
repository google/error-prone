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
import static com.google.errorprone.matchers.Matchers.contains;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import javax.lang.model.element.Modifier;

/** @author sulku@google.com (Marsela Sulku) */
@BugPattern(
    name = "CollectorShouldNotUseState",
    summary = "Collector.of() should not use state",
    category = JDK,
    severity = WARNING)
public class CollectorShouldNotUseState extends BugChecker implements MethodInvocationTreeMatcher {

  public static final Matcher<ExpressionTree> COLLECTOR_OF_CALL =
      staticMethod().onClass("java.util.stream.Collector").named("of");

  public final Matcher<Tree> containsAnonymousClassUsingState =
      contains(new AnonymousClassUsingStateMatcher());

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState visitorState) {

    if (COLLECTOR_OF_CALL.matches(methodInvocationTree, visitorState)
        && containsAnonymousClassUsingState.matches(methodInvocationTree, visitorState)) {
      return describeMatch(methodInvocationTree);
    }

    return Description.NO_MATCH;
  }

  /*
     Matches an anonymous inner class that contains one or more members that are not final
  */
  private class AnonymousClassUsingStateMatcher implements Matcher<Tree> {

    @Override
    public boolean matches(Tree tree, VisitorState visitorState) {
      if (!(tree instanceof NewClassTree)) {
        return false;
      }

      NewClassTree newClassTree = (NewClassTree) tree;

      if (newClassTree.getClassBody() == null) {
        return false;
      }

      return newClassTree.getClassBody().getMembers().stream()
          .filter(mem -> mem instanceof VariableTree)
          .anyMatch(mem -> !isFinal(mem));
    }

    private boolean isFinal(Tree tree) {
      return ASTHelpers.getSymbol((VariableTree) tree).getModifiers().contains(Modifier.FINAL);
    }
  }
}
