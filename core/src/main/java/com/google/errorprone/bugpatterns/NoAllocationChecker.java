/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.matchers.Matchers.anything;
import static com.google.errorprone.matchers.Matchers.assignment;
import static com.google.errorprone.matchers.Matchers.binaryTree;
import static com.google.errorprone.matchers.Matchers.compoundAssignment;
import static com.google.errorprone.matchers.Matchers.enclosingMethod;
import static com.google.errorprone.matchers.Matchers.enhancedForLoop;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.isArrayType;
import static com.google.errorprone.matchers.Matchers.isPrimitiveArrayType;
import static com.google.errorprone.matchers.Matchers.isPrimitiveType;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.methodReturnsNonPrimitiveType;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.typeCast;
import static com.google.errorprone.matchers.Matchers.variableInitializer;
import static com.google.errorprone.matchers.Matchers.variableType;
import static com.sun.source.tree.Tree.Kind.AND_ASSIGNMENT;
import static com.sun.source.tree.Tree.Kind.DIVIDE_ASSIGNMENT;
import static com.sun.source.tree.Tree.Kind.LEFT_SHIFT_ASSIGNMENT;
import static com.sun.source.tree.Tree.Kind.METHOD;
import static com.sun.source.tree.Tree.Kind.MINUS_ASSIGNMENT;
import static com.sun.source.tree.Tree.Kind.MULTIPLY_ASSIGNMENT;
import static com.sun.source.tree.Tree.Kind.OR_ASSIGNMENT;
import static com.sun.source.tree.Tree.Kind.PLUS;
import static com.sun.source.tree.Tree.Kind.PLUS_ASSIGNMENT;
import static com.sun.source.tree.Tree.Kind.POSTFIX_DECREMENT;
import static com.sun.source.tree.Tree.Kind.POSTFIX_INCREMENT;
import static com.sun.source.tree.Tree.Kind.PREFIX_DECREMENT;
import static com.sun.source.tree.Tree.Kind.PREFIX_INCREMENT;
import static com.sun.source.tree.Tree.Kind.REMAINDER_ASSIGNMENT;
import static com.sun.source.tree.Tree.Kind.RIGHT_SHIFT_ASSIGNMENT;
import static com.sun.source.tree.Tree.Kind.THROW;
import static com.sun.source.tree.Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT;
import static com.sun.source.tree.Tree.Kind.XOR_ASSIGNMENT;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.NoAllocation;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.CompoundAssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.EnhancedForLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewArrayTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.TypeCastTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.UnaryTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Checks methods annotated with @NoAllocation to ensure they really do not allocate.
 *
 * <ol>
 *   <li>Calls to new are disallowed.
 *   <li>Methods statically determined to be reachable from this method must also be annotated
 *       with @NoAllocation.
 *   <li>Autoboxing is disallowed.
 *   <li>String concatenation and conversions are disallowed.
 *   <li>To make it easier to use exceptions, allocations are always allowed within a throw
 *       statement. (But not in the methods of nested classes if they are annotated with
 *       @NoAllocation.)
 *   <li>The check is done at the source level. The compiler or runtime may perform optimizations
 *       or transformations that add or remove allocations in a way not visible to this check.
 * </ol>
 */
@BugPattern(name = "NoAllocation",
    summary = "@NoAllocation was specified on this method, but something was found that would"
      + " trigger an allocation",
    explanation = "Like many other languages, Java provides automatic memory management. In Java,"
      + " this feature incurs an runtime cost, and can also lead to unpredictable execution pauses."
      + " In most cases, this is a reasonable tradeoff, but sometimes the loss of performance or"
      + " predictability is unacceptable. Examples include pause-sensitive user interface handlers,"
      + " high query rate server response handlers, or other soft-realtime applications.\n\n"
      + "In these situations, you can annotate a few carefully written methods with"
      + " @NoAllocation. Methods with this annotation will avoid allocations in most cases,"
      + " reducing pressure on the garbage collector. Note that allocations may still occur in"
      + " methods with @NoAllocation if the compiler or runtime system inserts them.\n\n"
      + "To ease the use of exceptions, allocations are allowed if they occur within a throw"
      + " statement. But if the throw statement contains a nested class with methods annotated"
      + " with @NoAllocation, those methods will be disallowed from allocating.",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class NoAllocationChecker extends BugChecker
    implements AssignmentTreeMatcher, BinaryTreeMatcher, CompoundAssignmentTreeMatcher,
    EnhancedForLoopTreeMatcher, MethodInvocationTreeMatcher, NewArrayTreeMatcher,
    NewClassTreeMatcher, ReturnTreeMatcher, TypeCastTreeMatcher, UnaryTreeMatcher,
    VariableTreeMatcher {

  private static final String COMMON_MESSAGE_SUFFIX =
      "is disallowed in methods annotated with @NoAllocation";

  private static Matcher<MethodTree> noAllocationMethodMatcher =
      hasAnnotation(NoAllocation.class.getName());

  private static Matcher<MethodInvocationTree> noAllocationMethodInvocationMatcher =
      hasAnnotation(NoAllocation.class.getName());

  private static Matcher<ExpressionTree> anyExpression = anything();

  private static Matcher<StatementTree> anyStatement = anything();

  private static Matcher<VariableTree> anyVariable = anything();

  private static Matcher<ExpressionTree> isString = isSameType("java.lang.String");

  private static Matcher<ExpressionTree> arrayExpression = isArrayType();

  private static Matcher<ExpressionTree> primitiveExpression = isPrimitiveType();

  private static Matcher<ExpressionTree> primitiveArrayExpression = isPrimitiveArrayType();

  private static final Set<Kind> ALL_COMPOUND_OPERATORS =
      Collections.unmodifiableSet(
          EnumSet.of(
              AND_ASSIGNMENT,
              DIVIDE_ASSIGNMENT,
              LEFT_SHIFT_ASSIGNMENT,
              MINUS_ASSIGNMENT,
              MULTIPLY_ASSIGNMENT,
              OR_ASSIGNMENT,
              PLUS_ASSIGNMENT,
              REMAINDER_ASSIGNMENT,
              RIGHT_SHIFT_ASSIGNMENT,
              UNSIGNED_RIGHT_SHIFT_ASSIGNMENT,
              XOR_ASSIGNMENT));

  /**
   * Matches if a Tree has a ThrowTree before any MethodTree in its hierarchy. We don't want
   * the throw to nullify any @NoAnnotation in a method in an anonymous class below it.
   */
  private static Matcher<Tree> withinThrow = new Matcher<Tree>() {
    @Override
    public boolean matches(Tree tree, VisitorState state) {
      // TODO(user): Make this accept statements in a block that definitely will lead to a throw.
      TreePath path = state.getPath().getParentPath();
      while (path != null) {
        Tree node = path.getLeaf();
        state = state.withPath(path);
        Kind k = node.getKind();
        if (k == METHOD) {
          // We've gotten to the top of the method without finding a throw.
          return false;
        }
        if (k == THROW) {
          // We've found a throw.
          return true;
        }
        path = path.getParentPath();
      }
      return false;
    }
  };

  /**
   * Matches a new array statement if the enclosing method is annotated with @NoAllocation.
   */
  private static Matcher<NewArrayTree> newArrayMatcher =
      allOf(
        not(withinThrow),
        enclosingMethod(noAllocationMethodMatcher)
      );

  /**
   * Matches a new statement if the enclosing method is annotated with @NoAllocation.
   */
  private static Matcher<NewClassTree> newClassMatcher =
      allOf(
        not(withinThrow),
        enclosingMethod(noAllocationMethodMatcher)
      );

  /**
   * Matches if a method without @NoAllocation is invoked from a method with @NoAllocation.
   */
  private static Matcher<MethodInvocationTree> methodMatcher =
      allOf(
        not(withinThrow),
        enclosingMethod(noAllocationMethodMatcher),
        not(noAllocationMethodInvocationMatcher)
      );

  /**
   * Matches string concatenation. Includes all string conversions.
   */
  private static Matcher<BinaryTree> stringConcatenationMatcher =
      allOf(
        not(withinThrow),
        enclosingMethod(noAllocationMethodMatcher),
        kindIs(PLUS),
        binaryTree(anyExpression, isString)
      );

  /**
   * Matches string and boxing compound assignment.
   */
  private static Matcher<CompoundAssignmentTree> compoundAssignmentMatcher =
      allOf(
          not(withinThrow),
          enclosingMethod(noAllocationMethodMatcher),
          anyOf(
              compoundAssignment(PLUS_ASSIGNMENT, isString, anyExpression),
              compoundAssignment(ALL_COMPOUND_OPERATORS, not(primitiveExpression), anyExpression)));

  /**
   * Matches if foreach is used on a non-array or if boxing occurs with an array.
   */
  private static Matcher<EnhancedForLoopTree> foreachMatcher =
      allOf(
        not(withinThrow),
        enclosingMethod(noAllocationMethodMatcher),
        anyOf(
          not(enhancedForLoop(anyVariable, arrayExpression, anyStatement)),
          enhancedForLoop(variableType(not(isPrimitiveType())), primitiveArrayExpression,
            anyStatement)
        )
      );

  /**
   * Matches boxing assignment.
   */
  private static Matcher<AssignmentTree> boxingAssignment =
      allOf(
        not(withinThrow),
        enclosingMethod(noAllocationMethodMatcher),
        assignment(not(primitiveExpression), primitiveExpression)
      );

  /**
   * Matches boxing during variable initialization.
   */
  private static Matcher<VariableTree> boxingInitialization =
      allOf(
        not(withinThrow),
        enclosingMethod(noAllocationMethodMatcher),
        variableInitializer(primitiveExpression),
        variableType(not(isPrimitiveType()))
      );

  /**
   * Matches boxing by explicit cast.
   */
  private static Matcher<TypeCastTree> boxingCast =
      allOf(
        not(withinThrow),
        enclosingMethod(noAllocationMethodMatcher),
        typeCast(not(isPrimitiveType()), primitiveExpression)
      );

  /**
   * Matches boxing by return.
   */
  private static Matcher<ReturnTree> boxingReturn = new Matcher<ReturnTree>() {
    @Override
    public boolean matches(ReturnTree tree, VisitorState state) {
      return allOf(
        not(withinThrow),
        enclosingMethod(allOf(noAllocationMethodMatcher, methodReturnsNonPrimitiveType())),
        isPrimitiveType()
      ).matches(tree.getExpression(), state);
    }
  };

  /**
   * Matches boxing by method invocation, including varargs.
   */
  private static Matcher<MethodInvocationTree> boxingInvocation =
      new Matcher<MethodInvocationTree>() {
    @Override
    public boolean matches(MethodInvocationTree tree, VisitorState state) {
      if (!enclosingMethod(noAllocationMethodMatcher).matches(tree, state)) {
        return false;
      }

      // Get the arguments.
      JCMethodInvocation methodInvocation = (JCMethodInvocation) tree;
      List<JCExpression> arguments = methodInvocation.getArguments();

      // Get the parameters.
      MethodSymbol methodSymbol = ASTHelpers.getSymbol(tree);
      List<VarSymbol> params = methodSymbol.getParameters();

      // If there is a length mismatch, this implies varargs boxing.
      if (arguments.size() != params.size()) {
        return true;
      }

      // Check for boxing at each argument.
      int numArgs = arguments.size();
      int i = 0;
      Iterator<JCExpression> argument = arguments.iterator();
      Iterator<VarSymbol> param = params.iterator();
      while (param.hasNext() && argument.hasNext()) {
        JCExpression a = argument.next();
        VarSymbol p = param.next();

        if (a.type.isPrimitive() && !p.type.isPrimitive()) {
          // Boxing occurs here.
          return true;
        }

        // Check last parameter. If unassignable, this implies varargs boxing.
        if (i == numArgs - 1) {
          if (!state.getTypes().isAssignable(a.type, p.type)) {
            return true;
          }
        }
        i++;
      }

      return false;
    }
  };

  /**
   * Matches boxing by unary operator.
   */
  private static Matcher<UnaryTree> boxingUnary = new Matcher<UnaryTree>() {
    @Override
    public boolean matches(UnaryTree tree, VisitorState state) {
      return allOf(
        not(withinThrow),
        enclosingMethod(noAllocationMethodMatcher),
        anyOf(
          kindIs(POSTFIX_DECREMENT),
          kindIs(POSTFIX_INCREMENT),
          kindIs(PREFIX_DECREMENT),
          kindIs(PREFIX_INCREMENT)
        )
      ).matches(tree, state)
      && not(isPrimitiveType()).matches(tree, state);
    }
  };

  @Override
  public Description matchNewArray(NewArrayTree tree, VisitorState state) {
    if (!newArrayMatcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage("Allocating a new array with \"new\" or \"{ ... }\" " + COMMON_MESSAGE_SUFFIX)
        .build();
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!newClassMatcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage("Constructing a new object " + COMMON_MESSAGE_SUFFIX)
        .build();
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!methodMatcher.matches(tree, state) && !boxingInvocation.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage("Calling a method that is not annotated with @NoAllocation, calling a varargs"
          + " method without exactly matching the signature, or passing a primitive value as"
          + " non-primitive method argument " + COMMON_MESSAGE_SUFFIX)
        .build();
  }

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    if (!stringConcatenationMatcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage("String concatenation allocates a new String, which " + COMMON_MESSAGE_SUFFIX)
        .build();
  }

  @Override
  public Description matchCompoundAssignment(CompoundAssignmentTree tree, VisitorState state) {
    if (!compoundAssignmentMatcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage("Compound assignment to a String or boxed primitive allocates a new object,"
          + " which " + COMMON_MESSAGE_SUFFIX)
        .build();
  }

  @Override
  public Description matchEnhancedForLoop(EnhancedForLoopTree tree, VisitorState state) {
    if (!foreachMatcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage("Iterating over a Collection or iterating over a primitive array using a"
          + " non-primitive element type will trigger allocation, which " + COMMON_MESSAGE_SUFFIX)
        .build();
  }

  @Override
  public Description matchAssignment(AssignmentTree tree, VisitorState state) {
    if (!boxingAssignment.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage("Assigning a primitive value to a non-primitive variable or array element"
          + " will autobox the value, which " + COMMON_MESSAGE_SUFFIX)
        .build();
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (!boxingInitialization.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage("Initializing a non-primitive variable with a primitive value will autobox the"
          + " value, which " + COMMON_MESSAGE_SUFFIX)
        .build();
  }

  @Override
  public Description matchTypeCast(TypeCastTree tree, VisitorState state) {
    if (!boxingCast.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage("Casting a primitive value to a non-primitive type will autobox the value,"
          + " which " + COMMON_MESSAGE_SUFFIX)
        .build();
  }

  @Override
  public Description matchReturn(ReturnTree tree, VisitorState state) {
    if (!boxingReturn.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage("Returning a primitive value from a method with a non-primitive return type"
          + " will autobox the value, which " + COMMON_MESSAGE_SUFFIX)
        .build();
  }

  @Override
  public Description matchUnary(UnaryTree tree, VisitorState state) {
    if (!boxingUnary.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage("Pre- and post- increment/decrement operations on a non-primitive variable or"
          + " array element will autobox the result, which " + COMMON_MESSAGE_SUFFIX)
        .build();
  }
}
