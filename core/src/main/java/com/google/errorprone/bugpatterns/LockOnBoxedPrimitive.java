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

import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.anything;
import static com.google.errorprone.matchers.Matchers.assignment;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isBoxedPrimitiveType;
import static com.google.errorprone.matchers.Matchers.isPrimitiveOrBoxedPrimitiveType;
import static com.google.errorprone.matchers.Matchers.variableInitializer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.SynchronizedTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.Optional;

/** Detects locks on boxed primitives. */
@BugPattern(
    name = "LockOnBoxedPrimitive",
    summary =
        "It is dangerous to use a boxed primitive as a lock as it can unintentionally lead to"
            + " sharing a lock with another piece of code.",
    explanation =
        "Instances of boxed primitive types may be cached by the standard library `valueOf`"
            + " method. This method is used for autoboxing. This means that using a boxed"
            + " primitive as a lock can result in unintentionally sharing a lock with another"
            + " piece of code.",
    severity = SeverityLevel.WARNING)
public class LockOnBoxedPrimitive extends BugChecker
    implements CompilationUnitTreeMatcher, SynchronizedTreeMatcher, MethodInvocationTreeMatcher {

  private static final Matcher<AssignmentTree> PRIMITIVE_TO_OBJECT_ASSIGNMENT =
      assignment(anything(), isPrimitiveOrBoxedPrimitiveType());
  private static final Matcher<VariableTree> PRIMITIVE_TO_OBJECT_INITIALIZER =
      allOf(anything(), variableInitializer(isPrimitiveOrBoxedPrimitiveType()));

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
          instanceMethod().anyClass().namedAnyOf("wait", "notify", "notifyAll").withParameters());

  private static final Matcher<ExpressionTree> BOXED_PRIMITIVE = isBoxedPrimitiveType();

  private ImmutableSet<Symbol> knownBoxedVariables;

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    // Update the known boxed variables for this compilation unit.
    knownBoxedVariables = getKnownEncapsulatedBoxedObjects(tree, state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchSynchronized(SynchronizedTree tree, VisitorState state) {
    if (isDefinitelyBoxedPrimitive(tree.getExpression(), state)) {
      return describeMatch(tree);
    }

    return Description.NO_MATCH;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (LOCKING_METHOD.matches(tree, state)
        && isDefinitelyBoxedPrimitive(ASTHelpers.getReceiver(tree), state)) {
      return describeMatch(tree.getMethodSelect());
    }

    return Description.NO_MATCH;
  }

  /**
   * Returns true if the expression tree is definitely referring to a boxed primitive. This is the
   * case when the type is a boxed primitive, or the expression refers to a final variable that was
   * initialized with a boxed primitive.
   */
  private boolean isDefinitelyBoxedPrimitive(ExpressionTree tree, VisitorState state) {
    ExpressionTree stripped = ASTHelpers.stripParentheses(tree);

    return BOXED_PRIMITIVE.matches(stripped, state) || isKnownBoxedSymbol(stripped);
  }

  private boolean isKnownBoxedSymbol(ExpressionTree tree) {
    return knownBoxedVariables.contains(ASTHelpers.getSymbol(tree));
  }

  /**
   * Searches the compilationUnitTree for any variable of type Object that is known to hold a boxed
   * primitive at some point in time.
   */
  private static ImmutableSet<Symbol> getKnownEncapsulatedBoxedObjects(
      CompilationUnitTree compilationUnitTree, VisitorState state) {
    ImmutableSet.Builder<Symbol> knownBoxedVariables = ImmutableSet.builder();

    new TreeScanner<Void, Void>() {
      @Override
      public Void visitAssignment(AssignmentTree assignmentTree, Void unused) {
        if (PRIMITIVE_TO_OBJECT_ASSIGNMENT.matches(assignmentTree, state)) {
          Optional.ofNullable(ASTHelpers.getSymbol(assignmentTree.getVariable()))
              .ifPresent(knownBoxedVariables::add);
        }
        return super.visitAssignment(assignmentTree, null);
      }

      @Override
      public Void visitVariable(VariableTree variableTree, Void unused) {
        if (PRIMITIVE_TO_OBJECT_INITIALIZER.matches(variableTree, state)) {
          Optional.ofNullable(ASTHelpers.getSymbol(variableTree))
              .ifPresent(knownBoxedVariables::add);
        }
        return super.visitVariable(variableTree, null);
      }
    }.scan(compilationUnitTree, null);

    return knownBoxedVariables.build();
  }
}
