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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.predicates.TypePredicates.isArray;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.IdentifierTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Type;

import javax.lang.model.type.TypeKind;

/**
 * @author adgar@google.com (Mike Edgar)
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
  name = "ArrayToString",
  summary = "Calling toString on an array does not provide useful information",
  explanation =
      "The toString method on an array will print its identity, such as [I@4488aabb. This "
          + "is almost never needed. Use Arrays.toString to print a human-readable array summary.",
  category = JDK,
  severity = ERROR,
  maturity = MATURE
)
public class ArrayToString extends BugChecker
    implements MethodInvocationTreeMatcher, IdentifierTreeMatcher, MemberSelectTreeMatcher {

  private static final Matcher<ExpressionTree> GET_STACK_TRACE_MATCHER =
      instanceMethod().onDescendantOf("java.lang.Throwable").named("getStackTrace");

  private static final Matcher<ExpressionTree> ARRAY_TO_STRING =
      instanceMethod().onClass(isArray()).named("toString");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree methodTree, VisitorState state) {
    if (!ARRAY_TO_STRING.matches(methodTree, state)) {
      return checkImplicitToString(methodTree, state);
    }

    ExpressionTree receiverTree = ASTHelpers.getReceiver(methodTree);
    if (GET_STACK_TRACE_MATCHER.matches(receiverTree, state)) {
      // If the array is the result of calling e.getStackTrace(), replace
      // e.getStackTrace().toString() with Guava's Throwables.getStackTraceAsString(e).
      String throwable = state.getSourceForNode(ASTHelpers.getReceiver(receiverTree));
      Fix fix =
          SuggestedFix.builder()
              .replace(methodTree, "Throwables.getStackTraceAsString(" + throwable + ")")
              .addImport("com.google.common.base.Throwables")
              .build();
      return describeMatch(methodTree, fix);
    }

    return buildFix(methodTree, receiverTree, state);
  }

  @Override
  public Description matchIdentifier(IdentifierTree tree, VisitorState state) {
    return checkImplicitToString(tree, state);
  }

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    return checkImplicitToString(tree, state);
  }

  private static final Matcher<Tree> PRINT_STRING_METHOD =
      toType(
          ExpressionTree.class,
          anyOf(
              instanceMethod()
                  .onDescendantOf("java.io.PrintStream")
                  .withSignature("print(java.lang.Object)"),
              instanceMethod()
                  .onDescendantOf("java.io.PrintStream")
                  .withSignature("println(java.lang.Object)"),
              instanceMethod()
                  .onDescendantOf("java.lang.StringBuilder")
                  .withSignature("append(java.lang.Object)")));

  private static final Matcher<Tree> TO_STRING_METHOD =
      toType(
          ExpressionTree.class,
          staticMethod().onClass("java.lang.String").withSignature("valueOf(java.lang.Object)"));

  /**
   * Tests if the given expression evaluates to an array, and implicitly converted to a string
   * through string concatenation or calling a method that converts its arguments to strings.
   */
  private Description checkImplicitToString(Tree tree, VisitorState state) {
    Type type = ASTHelpers.getType(tree);
    if (type == null || type.getKind() != TypeKind.ARRAY) {
      return Description.NO_MATCH;
    }
    // is the enclosing expression string concat?
    Tree parent = state.getPath().getParentPath().getLeaf();
    if ((parent.getKind() == Kind.PLUS || parent.getKind() == Kind.PLUS_ASSIGNMENT)
        && state.getTypes().isSameType(ASTHelpers.getType(parent), state.getSymtab().stringType)) {
      return buildFix(tree, tree, state);
    }
    // if the enclosing method is print() or println(), wrap the expression in Arrays.toString()
    // print(theArray) -> print(Arrays.toString(theArray))
    if (PRINT_STRING_METHOD.matches(parent, state)) {
      return buildFix(tree, tree, state);
    }
    // if the enclosing method is String.valueOf(), replace the parent with Arrays.toString()
    // String.valueOf(theArray) -> Arrays.toString(theArray)
    if (TO_STRING_METHOD.matches(parent, state)) {
      return buildFix(parent, tree, state);
    }
    return Description.NO_MATCH;
  }

  private Description buildFix(Tree replace, Tree with, VisitorState state) {
    return describeMatch(
        replace,
        SuggestedFix.builder()
            .replace(replace, "Arrays.toString(" + state.getSourceForNode(with) + ")")
            .addImport("java.util.Arrays")
            .build());
  }
}
