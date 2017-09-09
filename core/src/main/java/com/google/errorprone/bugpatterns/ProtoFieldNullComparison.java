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

import static com.google.errorprone.BugPattern.Category.PROTOBUF;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.instanceMethod;

import com.google.common.base.Predicate;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;

@BugPattern(
  name = "ProtoFieldNullComparison",
  summary = "Protobuf fields cannot be null",
  explanation =
      "This checker looks for comparisons of protocol buffer fields with null. "
          + "If a proto field is not specified, its field accessor will return a non-null default "
          + "value. Thus, the result of calling one of these accessors can never be null, and "
          + "comparisons like these often indicate a nearby error.\n\n"
          + "If you need to distinguish between an unset optional value and a default value, you "
          + "have two options.  In most cases, you can simply use the `hasField()` method. proto3 "
          + "however does not generate `hasField()` methods for scalar fields of type `string` or "
          + "`bytes`. In those cases you will need to wrap your field in "
          + "`google.protobuf.StringValue` or `google.protobuf.BytesValue`, respectively.",
  category = PROTOBUF,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class ProtoFieldNullComparison extends BugChecker implements BinaryTreeMatcher {

  private static final Predicate<MethodSymbol> NO_ARGS =
      new Predicate<MethodSymbol>() {
        @Override
        public boolean apply(MethodSymbol input) {
          return input.params().isEmpty();
        }
      };

  private static final String PROTO_SUPER_CLASS = "com.google.protobuf.GeneratedMessage";

  private static final Matcher<ExpressionTree> protoMessageReceiverMatcher =
      instanceMethod().onDescendantOf(PROTO_SUPER_CLASS);

  private static final String LIST_INTERFACE = "java.util.List";

  private static final Matcher<Tree> returnsListMatcher = Matchers.isSubtypeOf(LIST_INTERFACE);

  private static final Set<Kind> COMPARISON_OPERATORS =
      EnumSet.of(Kind.EQUAL_TO, Kind.NOT_EQUAL_TO);

  private static final Matcher<BinaryTree> MATCHER =
      new Matcher<BinaryTree>() {
        @Override
        public boolean matches(BinaryTree tree, VisitorState state) {
          if (!COMPARISON_OPERATORS.contains(tree.getKind())) {
            return false;
          }
          ExpressionTree leftOperand = tree.getLeftOperand();
          ExpressionTree rightOperand = tree.getRightOperand();
          return (isNull(rightOperand) && isProtoMessageGetInvocation(leftOperand, state))
              || (isNull(leftOperand) && isProtoMessageGetInvocation(rightOperand, state));
        }
      };

  private static boolean isNull(ExpressionTree tree) {
    return tree.getKind() == Kind.NULL_LITERAL;
  }

  private static boolean isProtoMessageGetInvocation(ExpressionTree tree, VisitorState state) {
    return (isGetMethodInvocation(tree, state) || isGetListMethodInvocation(tree, state))
        && receiverIsProtoMessage(tree, state);
  }

  private static boolean isFieldGetMethod(String methodName) {
    return methodName.startsWith("get");
  }

  private static String getMethodName(ExpressionTree tree) {
    MethodInvocationTree method = (MethodInvocationTree) tree;
    ExpressionTree expressionTree = method.getMethodSelect();
    JCFieldAccess access = (JCFieldAccess) expressionTree;
    return access.sym.getQualifiedName().toString();
  }

  private static boolean isGetListMethodInvocation(ExpressionTree tree, VisitorState state) {
    if (tree.getKind() == Tree.Kind.METHOD_INVOCATION) {
      MethodInvocationTree method = (MethodInvocationTree) tree;
      if (!method.getArguments().isEmpty()) {
        return false;
      }
      if (!returnsListMatcher.matches(method, state)) {
        return false;
      }
      ExpressionTree expressionTree = method.getMethodSelect();
      if (expressionTree instanceof JCFieldAccess) {
        JCFieldAccess access = (JCFieldAccess) expressionTree;
        String methodName = access.sym.getQualifiedName().toString();
        return isFieldGetMethod(methodName);
      }
      return true;
    }
    return false;
  }

  private static boolean isGetMethodInvocation(ExpressionTree tree, VisitorState state) {
    if (tree.getKind() == Tree.Kind.METHOD_INVOCATION) {
      MethodInvocationTree method = (MethodInvocationTree) tree;
      if (!method.getArguments().isEmpty()) {
        return false;
      }
      if (returnsListMatcher.matches(method, state)) {
        return false;
      }
      ExpressionTree expressionTree = method.getMethodSelect();
      if (expressionTree instanceof JCFieldAccess) {
        JCFieldAccess access = (JCFieldAccess) expressionTree;
        String methodName = access.sym.getQualifiedName().toString();
        return isFieldGetMethod(methodName);
      }
      return true;
    }
    return false;
  }

  private static boolean receiverIsProtoMessage(ExpressionTree tree, VisitorState state) {
    return protoMessageReceiverMatcher.matches(tree, state);
  }

  private static String replaceLast(String text, String pattern, String replacement) {
    StringBuilder builder = new StringBuilder(text);
    int lastIndexOf = builder.lastIndexOf(pattern);
    return builder.replace(lastIndexOf, lastIndexOf + pattern.length(), replacement).toString();
  }

  /**
   * Creates replacements for the following comparisons:
   * <pre>
   * proto.getField() == null --> !proto.hasField()
   * proto.getField() != null --> proto.hasField()
   * proto.getList() == null  --> proto.getList().isEmpty()
   * proto.getList() != null  --> !proto.getList().isEmpty()
   * <pre>
   * Also creates replacements for the Yoda style version of them.
   */
  @Nullable
  private static String createReplacement(BinaryTree tree, VisitorState state) {
    ExpressionTree leftOperand = tree.getLeftOperand();
    ExpressionTree rightOperand = tree.getRightOperand();
    ExpressionTree methodInvocation;
    if (isNull(leftOperand)) {
      methodInvocation = rightOperand;
    } else {
      methodInvocation = leftOperand;
    }
    if (isGetMethodInvocation(methodInvocation, state)) {
      String methodName = getMethodName(methodInvocation);
      String hasMethod = methodName.replaceFirst("get", "has");

      // proto3 does not generate has methods for scalar types, e.g. ByteString and String.
      // Do not provide a replacement in these cases.
      Set<MethodSymbol> hasMethods =
          ASTHelpers.findMatchingMethods(
              state.getName(hasMethod),
              NO_ARGS,
              ASTHelpers.getType(ASTHelpers.getReceiver(methodInvocation)),
              state.getTypes());
      if (hasMethods.isEmpty()) {
        return null;
      }
      String replacement = replaceLast(methodInvocation.toString(), methodName, hasMethod);
      replacement = tree.getKind() == Kind.EQUAL_TO ? "!" + replacement : replacement;
      return replacement;
    } else {
      String replacement = methodInvocation + ".isEmpty()";
      return tree.getKind() == Kind.EQUAL_TO ? replacement : "!" + replacement;
    }
  }

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    String replacement = createReplacement(tree, state);
    if (replacement == null) {
      return describeMatch(tree);
    } else {
      return describeMatch(tree, SuggestedFix.replace(tree, createReplacement(tree, state)));
    }
  }
}
