/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isBoxedPrimitiveType;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.SynchronizedTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import javax.lang.model.element.ElementKind;

/** Detects locks on boxed primitives. */
@BugPattern(
    name = "LockOnBoxedPrimitive",
    summary =
        "It is dangerous to use a boxed primitive as a lock as it can unintentionally lead to"
            + " sharing a lock with another piece of code.",
    severity = SeverityLevel.WARNING)
public class LockOnBoxedPrimitive extends BugChecker
    implements SynchronizedTreeMatcher, MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> LOCKING_METHOD =
      anyOf(
          instanceMethod()
              .anyClass()
              .named("wait")
              .withParametersOfType(ImmutableList.of(Suppliers.LONG_TYPE)),
          instanceMethod()
              .anyClass()
              .named("wait")
              .withParametersOfType(ImmutableList.of(Suppliers.LONG_TYPE, Suppliers.INT_TYPE)),
          instanceMethod().anyClass().namedAnyOf("wait", "notify", "notifyAll").withNoParameters());

  private static final Matcher<ExpressionTree> BOXED_PRIMITIVE = isBoxedPrimitiveType();

  @Override
  public Description matchSynchronized(SynchronizedTree tree, VisitorState state) {
    ExpressionTree locked = stripParentheses(tree.getExpression());
    if (!isDefinitelyBoxedPrimitive(locked, state)) {
      return NO_MATCH;
    }
    return describeMatch(tree, createFix(locked, state));
  }

  private SuggestedFix createFix(ExpressionTree locked, VisitorState state) {
    Symbol lock = getSymbol(locked);
    if (lock == null) {
      return SuggestedFix.emptyFix();
    }
    if (!lock.getKind().equals(ElementKind.FIELD)) {
      return SuggestedFix.emptyFix();
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    String lockName = lock.getSimpleName() + "Lock";
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitVariable(VariableTree node, Void unused) {
        VarSymbol sym = getSymbol(node);
        if (lock.equals(sym)) {
          String unboxedType =
              SuggestedFixes.qualifyType(state, fix, state.getTypes().unboxedType(getType(node)));
          fix.prefixWith(
                  node,
                  String.format(
                      "private final Object %s = new Object();\n@GuardedBy(\"%s\")",
                      lockName, lockName))
              .replace(node.getType(), unboxedType)
              .addImport("com.google.errorprone.annotations.concurrent.GuardedBy");
        }
        return super.visitVariable(node, null);
      }

      @Override
      public Void visitSynchronized(SynchronizedTree node, Void aVoid) {
        ExpressionTree expression = stripParentheses(node.getExpression());
        if (lock.equals(getSymbol(expression))) {
          fix.replace(expression, lockName);
        }
        return super.visitSynchronized(node, aVoid);
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return fix.build();
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (LOCKING_METHOD.matches(tree, state)
        && isDefinitelyBoxedPrimitive(ASTHelpers.getReceiver(tree), state)) {
      return describeMatch(tree.getMethodSelect());
    }
    return NO_MATCH;
  }

  /** Returns true if the expression tree is definitely referring to a boxed primitive. */
  private static boolean isDefinitelyBoxedPrimitive(ExpressionTree tree, VisitorState state) {
    return BOXED_PRIMITIVE.matches(tree, state);
  }
}
