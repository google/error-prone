/*
 * Copyright 2025 The Error Prone Authors.
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
package com.google.errorprone.bugpatterns.time;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.VisitorState.memoize;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.enclosingPackage;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.BlockTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;

/** A BugPattern; see the summary. */
@BugPattern(
    severity = WARNING,
    summary =
        "Accessing the current time in a static initialiser captures the time at class loading,"
            + " which is rarely desirable.")
public final class TimeInStaticInitializer extends BugChecker
    implements BlockTreeMatcher, VariableTreeMatcher {
  @Override
  public Description matchBlock(BlockTree tree, VisitorState state) {
    if (tree.isStatic()) {
      scanForTimeAccess(state);
    }
    return NO_MATCH;
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    VarSymbol symbol = getSymbol(tree);
    if (symbol.isStatic()
        && tree.getInitializer() != null
        && !isSubtype(symbol.type, FLAG.get(state), state)) {
      scanForTimeAccess(state.withPath(new TreePath(state.getPath(), tree.getInitializer())));
    }
    return NO_MATCH;
  }

  private void scanForTimeAccess(VisitorState state) {
    if (state.errorProneOptions().isTestOnlyTarget()) {
      return;
    }
    new SuppressibleTreePathScanner<Void, Void>(state) {
      @Override
      public Void visitMethod(MethodTree node, Void unused) {
        return null;
      }

      @Override
      public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
        return null;
      }

      @Override
      public Void visitMemberReference(MemberReferenceTree node, Void unused) {
        return null;
      }

      @Override
      public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
        if (TIME_ACCESSORS.matches(node, state)) {
          state.reportMatch(describeMatch(node));
        }
        return super.visitMethodInvocation(node, null);
      }
    }.scan(state.getPath(), null);
  }

  private static final Matcher<ExpressionTree> TIME_ACCESSORS =
      anyOf(
          staticMethod()
              .onClass(
                  (t, s) -> {
                    PackageSymbol pkg = enclosingPackage(t.tsym);
                    return pkg != null && pkg.getQualifiedName().contentEquals("java.time");
                  })
              .named("now"),
          instanceMethod().onDescendantOf("java.time.InstantSource").named("instant"),
          instanceMethod().onDescendantOf("com.google.common.time.TimeSource").named("instant"));

  /**
   * As a heuristic, Flags seem fine, given those usually do want to capture something at start
   * time.
   */
  private static final Supplier<Type> FLAG =
      memoize(s -> s.getTypeFromString("com.google.common.flags.Flag"));
}
