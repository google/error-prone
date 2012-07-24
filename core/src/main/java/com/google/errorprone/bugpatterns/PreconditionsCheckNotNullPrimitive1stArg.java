/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol.OperatorSymbol;
import com.sun.tools.javac.tree.JCTree.JCBinary;

import java.util.List;

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.MaturityLevel.ON_BY_DEFAULT;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.*;

/**
 * Checks that the 1st argument to Preconditions.checkNotNull() isn't a primitive
 * boolean.
 *
 * A common special case is considered in the suggested fixes, e.g.
 *
 * <pre>
 * Preconditions.checkNotNull(foo != null, "Foo is null!");
 * </pre>
 * This is converted to:
 * <pre>
 * Preconditions.checkNotNull(foo, "Foo is null!");
 * </pre>
 *
 * Otherwise, the code is unwrapped into an explicit {@code if} block.
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
@BugPattern(name = "PreconditionsCheckNotNullBoolean",
    summary = "First argument to Preconditions.checkNotNull() is a boolean rather " +
    		"than an object reference",
    explanation =
        "Preconditions.checkNotNull() takes as an argument a reference that should be " +
        "non-null. Often a primitive boolean is passed as the argument to check, e.g., " +
        "`Preconditions.checkNotNull(foo != null, \"Foo is null!\")`. The primitive boolean " +
        "will be autoboxed into a boxed Boolean, which is non-null, causing the check to " +
        "always pass without the condition being evaluated. This check ensures that the " +
        "first argument to Preconditions.checkNotNull() is not a primitive boolean.",
    category = GUAVA, severity = ERROR, maturity = ON_BY_DEFAULT)
public class PreconditionsCheckNotNullPrimitive1stArg
    extends DescribingMatcher<MethodInvocationTree> {

  @SuppressWarnings("unchecked")
  @Override
  public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
    return allOf(
        methodSelect(staticMethod(
            "com.google.common.base.Preconditions", "checkNotNull")),
        argument(0, Matchers.<ExpressionTree>isSubtypeOf(state.getSymtab().booleanType)))
        .matches(methodInvocationTree, state);
  }

  @Override
  public Description describe(MethodInvocationTree methodInvocationTree,
                              VisitorState state) {
    SuggestedFix fix = null;
    ExpressionTree expression = methodInvocationTree.getArguments().get(0);
    if (expression instanceof JCBinary) {
      JCBinary binary = (JCBinary) expression;
      if (binary.getOperator() instanceof OperatorSymbol) {
        OperatorSymbol operator = (OperatorSymbol) binary.getOperator();
        if (operator.name.toString().equals("!=")) {
          if (binary.getRightOperand().getKind() == Kind.NULL_LITERAL) {
            fix = new SuggestedFix().replace(binary,
                binary.getLeftOperand().toString());
          } else if (binary.getLeftOperand().getKind() == Kind.NULL_LITERAL) {
            fix = new SuggestedFix().replace(binary, binary.getRightOperand().toString());
          }
        }
      }
    }
    if (fix == null) {
      List<? extends ExpressionTree> args = methodInvocationTree.getArguments();
      if (args.size() == 1) {
        fix = new SuggestedFix().replace(methodInvocationTree,
            String.format(
                "if (!(%s)) { throw new NullPointerException(); }",
                args.get(0)));
      } else if (args.size() == 2) {
        fix = new SuggestedFix().replace(methodInvocationTree,
            String.format(
                "if (!(%s)) { throw new NullPointerException(%s); }",
                args.get(0), args.get(1)));
      } else {
        // We would use Join.join() here, but we don't want to pull in Guava.
        StringBuilder extraArgs = new StringBuilder();
        for (int i = 2; i < args.size(); i++) {
          extraArgs.append(", ");
          extraArgs.append(args.get(i).toString());
        }
        fix = new SuggestedFix().replace(methodInvocationTree,
            String.format(
                "if (!(%s)) { throw new NullPointerException(String.format(%s%s); }",
                args.get(0), args.get(1), extraArgs.toString()));
      }
    }

    return new Description(methodInvocationTree.getArguments().get(0), diagnosticMessage, fix);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    public DescribingMatcher<MethodInvocationTree> matcher = new PreconditionsCheckNotNullPrimitive1stArg();

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitMethodInvocation(node, visitorState);
    }
  }

}
