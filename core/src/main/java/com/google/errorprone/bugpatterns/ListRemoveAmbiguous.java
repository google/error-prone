/*
 * Copyright 2026 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;
import static com.google.errorprone.util.ASTHelpers.getReceiverType;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Commented;
import com.google.errorprone.util.Comments;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Ambiguous call to List.remove; clarify if index-based or value-based removal was intended"
            + " by adding a comment",
    severity = WARNING)
public class ListRemoveAmbiguous extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> LIST_REMOVE_INDEX =
      instanceMethod().onDescendantOf("java.util.List").named("remove").withParameters("int");

  private static final Matcher<ExpressionTree> LIST_REMOVE_OBJECT =
      instanceMethod()
          .onDescendantOf("java.util.List")
          .named("remove")
          .withParameters("java.lang.Object");

  private static final Matcher<ExpressionTree> EXPLICIT_BOXING =
      staticMethod().onClass("java.lang.Integer").named("valueOf");
  private static final Matcher<ExpressionTree> EXPLICIT_UNBOXING =
      instanceMethod().onDescendantOf("java.lang.Integer").named("intValue");
  private static final Supplier<Type> JAVA_LANG_NUMBER = typeFromString("java.lang.Number");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    boolean isIndexCall = LIST_REMOVE_INDEX.matches(tree, state);
    boolean isObjectCall = LIST_REMOVE_OBJECT.matches(tree, state);
    if (!isIndexCall && !isObjectCall) {
      return NO_MATCH;
    }
    ExpressionTree arg = getOnlyElement(tree.getArguments());
    String expectedComment = isIndexCall ? "index" : "element";

    if (hasCorrectComment(tree, expectedComment, state)
        || EXPLICIT_BOXING.matches(arg, state)
        || EXPLICIT_UNBOXING.matches(arg, state)) {
      return NO_MATCH;
    }

    Type argType = getType(arg);
    Type receiverType;
    ExpressionTree receiver = ASTHelpers.getReceiver(tree);
    if (receiver == null) {
      ClassTree enclosingClass = ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class);
      if (enclosingClass == null) {
        return NO_MATCH;
      }
      receiverType = ASTHelpers.getType(enclosingClass);
    } else {
      receiverType = getReceiverType(tree);
    }
    if (receiverType == null) {
      return NO_MATCH;
    }
    Type intType = state.getSymtab().intType;
    if (!isSameType(state.getTypes().unboxedTypeOrType(argType), intType, state)) {
      return NO_MATCH;
    }
    Type listType = state.getSymtab().listType;
    if (listType == null) {
      return NO_MATCH;
    }
    Type superType = state.getTypes().asSuper(receiverType, listType.tsym);
    if (superType == null || superType.getTypeArguments().isEmpty()) {
      return NO_MATCH;
    }
    Type elementType = getOnlyElement(superType.getTypeArguments());
    Type integerType = state.getTypes().boxedClass(intType).type;
    Type numberType = JAVA_LANG_NUMBER.get(state);
    if (!isSameType(elementType, integerType, state)
        && !isSameType(elementType, numberType, state)) {
      return NO_MATCH;
    }

    Description.Builder builder = buildDescription(tree);
    if (state.getTypes().isSameType(argType, intType)) {
      SuggestedFix sequencedFix = getSequencedFix(tree, arg, state);
      if (sequencedFix != null) {
        builder.addFix(sequencedFix);
      } else {
        builder.addFix(SuggestedFix.prefixWith(arg, "/* index */ "));
      }
    } else {
      builder.addFix(SuggestedFix.prefixWith(arg, "/* element */ "));
    }

    return builder.build();
  }

  private static boolean hasCorrectComment(
      MethodInvocationTree tree, String expectedComment, VisitorState state) {
    ImmutableList<Commented> commentedArgs = Comments.findCommentsForArguments(tree, state);
    if (commentedArgs.isEmpty()) {
      return false;
    }
    Commented argCommented = commentedArgs.get(0);
    return argCommented.beforeComments().stream()
        .map(Comments::getTextFromComment)
        .map(String::trim)
        .anyMatch(text -> text.equals(expectedComment));
  }

  private static final Matcher<ExpressionTree> SIZE_METHOD =
      instanceMethod()
          .onDescendantOf("java.util.List")
          .named("size")
          .withParameters(ImmutableList.of());

  private static @Nullable SuggestedFix getSequencedFix(
      MethodInvocationTree tree, ExpressionTree arg, VisitorState state) {
    ExpressionTree receiver = ASTHelpers.getReceiver(tree);

    if (Objects.equals(ASTHelpers.constValue(arg, Integer.class), 0)) {
      return SuggestedFix.replace(
          tree, (receiver == null ? "" : state.getSourceForNode(receiver) + ".") + "removeFirst()");
    }

    if (isSizeMinusOne(arg, receiver, state)) {
      return SuggestedFix.replace(
          tree, (receiver == null ? "" : state.getSourceForNode(receiver) + ".") + "removeLast()");
    }

    return null;
  }

  private static boolean isSizeMinusOne(
      ExpressionTree arg, @Nullable ExpressionTree removeReceiver, VisitorState state) {
    arg = ASTHelpers.stripParentheses(arg);
    if (!(arg instanceof BinaryTree binaryTree)) {
      return false;
    }
    if (binaryTree.getKind() != Tree.Kind.MINUS) {
      return false;
    }
    ExpressionTree left = ASTHelpers.stripParentheses(binaryTree.getLeftOperand());
    ExpressionTree right = ASTHelpers.stripParentheses(binaryTree.getRightOperand());
    if (!Objects.equals(ASTHelpers.constValue(right, Integer.class), 1)) {
      return false;
    }
    if (!SIZE_METHOD.matches(left, state)) {
      return false;
    }
    ExpressionTree sizeReceiver = ASTHelpers.getReceiver(left);
    return sameReceiver(removeReceiver, sizeReceiver, state);
  }

  private static boolean sameReceiver(
      @Nullable ExpressionTree rx1, @Nullable ExpressionTree rx2, VisitorState state) {
    if (rx1 == null || rx2 == null) {
      return rx1 == null && rx2 == null;
    }
    rx1 = ASTHelpers.stripParentheses(rx1);
    rx2 = ASTHelpers.stripParentheses(rx2);
    if ((rx1 instanceof IdentifierTree || rx1 instanceof MemberSelectTree)
        && (rx2 instanceof IdentifierTree || rx2 instanceof MemberSelectTree)) {
      return ASTHelpers.sameVariable(rx1, rx2);
    }
    return state.getSourceForNode(rx1).equals(state.getSourceForNode(rx2));
  }
}
