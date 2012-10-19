/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.MaturityLevel.ON_BY_DEFAULT;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.sameArgument;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@BugPattern(name = "SelfEquals",
    summary = "An object is tested for equality to itself",
    explanation =
        "The arguments to this equal method are the same object, so it always returns " +
        "true.  Either change the arguments to point to different objects or substitute true.",
    category = GUAVA, severity = ERROR, maturity = ON_BY_DEFAULT)
public class SelfEquals extends DescribingMatcher<MethodInvocationTree> {

  /**
   * Matches calls to the Guava method Objects.equal() in which the two arguments are
   * the same reference.
   *
   * Example: Objects.equal(foo, foo)
   */
  @SuppressWarnings({"unchecked"})
  private static final Matcher<MethodInvocationTree> guavaMatcher = allOf(
      methodSelect(staticMethod("com.google.common.base.Objects", "equal")),
      sameArgument(0, 1));

  /**
   * Matches calls to any instance method called "equals" with exactly one argument in which the
   * receiver is the same reference as the argument.
   *
   * Example: foo.equals(foo)
   *
   * TODO(eaftan): This may match too many things, if people are calling methods "equals" that
   * don't really mean equals.
   */
  @SuppressWarnings("unchecked")
  private static final Matcher<MethodInvocationTree> equalMatcher = allOf(
      methodSelect(Matchers.instanceMethod(Matchers.<ExpressionTree>anything(), "equals")),
      Matchers.receiverSameAsArgument(0));

  @Override
  public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
    return guavaMatcher.matches(methodInvocationTree, state) ||
        equalMatcher.matches(methodInvocationTree, state);
  }

  @Override
  public Description describe(MethodInvocationTree methodInvocationTree, VisitorState state) {
    // If we don't find a good field to use, then just replace with "true"
    SuggestedFix fix = new SuggestedFix().replace(methodInvocationTree, "true");

    if (guavaMatcher.matches(methodInvocationTree, state)) {

      JCExpression toReplace = (JCExpression) methodInvocationTree.getArguments().get(1);
      // Find containing block
      TreePath path = state.getPath();
      while(path.getLeaf().getKind() != Kind.BLOCK) {
        path = path.getParentPath();
      }
      JCBlock block = (JCBlock)path.getLeaf();
      for (JCStatement jcStatement : block.getStatements()) {
        if (jcStatement.getKind() == Kind.VARIABLE) {
          JCVariableDecl declaration = (JCVariableDecl) jcStatement;
          TypeSymbol variableTypeSymbol = declaration.getType().type.tsym;

          if (((JCIdent)toReplace).sym.isMemberOf(variableTypeSymbol, state.getTypes())) {
            fix = new SuggestedFix().prefixWith(toReplace, declaration.getName().toString() + ".");
          }
        }
      }
    }

    return new Description(methodInvocationTree, diagnosticMessage, fix);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    private final DescribingMatcher<MethodInvocationTree> matcher =
        new SelfEquals();

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitMethodInvocation(node, visitorState);
    }
  }
}
