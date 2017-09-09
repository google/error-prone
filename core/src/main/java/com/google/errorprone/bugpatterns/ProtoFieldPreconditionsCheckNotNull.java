/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.parentNode;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;

/** @author awturner@google.com (Andy Turner) */
@BugPattern(
  name = "ProtoFieldPreconditionsCheckNotNull",
  summary = "Protobuf fields cannot be null, so this check is redundant",
  explanation =
      "This checker looks for comparisons of protocol buffer fields with null "
          + "via the com.google.common.base.Preconditions.checkNotNull method. "
          + "If a proto field is not specified, its field accessor will return a non-null default "
          + "value. Thus, the result of calling one of these accessors can never be null, and "
          + "comparisons like these often indicate a nearby error.\n\n"
          + "If you meant to check whether an optional field has been set, you should use the "
          + "hasField() method instead.",
  category = GUAVA,
  severity = WARNING,
  tags = StandardTags.LIKELY_ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class ProtoFieldPreconditionsCheckNotNull extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final String PROTO_SUPER_CLASS = "com.google.protobuf.GeneratedMessage";

  private static final Matcher<ExpressionTree> protoMessageReceiverMatcher =
      instanceMethod().onDescendantOf(PROTO_SUPER_CLASS);

  private static final String LIST_INTERFACE = "java.util.List";

  private static final Matcher<Tree> returnsListMatcher = Matchers.isSubtypeOf(LIST_INTERFACE);

  private static final Matcher<ExpressionTree> PROTO_MESSAGE_INVOCATION_MATCHER =
      new Matcher<ExpressionTree>() {
        @Override
        public boolean matches(ExpressionTree tree, VisitorState state) {
          return isProtoMessageGetInvocation(tree, state);
        }
      };

  @SuppressWarnings({"unchecked"})
  private static final Matcher<MethodInvocationTree> CHECK_NOT_NULL_MATCHER =
      allOf(
          staticMethod().onClass("com.google.common.base.Preconditions").named("checkNotNull"),
          argument(
              0,
              Matchers.<ExpressionTree>allOf(
                  Matchers.<ExpressionTree>kindIs(Kind.METHOD_INVOCATION),
                  PROTO_MESSAGE_INVOCATION_MATCHER)));

  private static boolean isProtoMessageGetInvocation(ExpressionTree tree, VisitorState state) {
    return (isGetMethodInvocation(tree, state) || isGetListMethodInvocation(tree, state))
        && receiverIsProtoMessage(tree, state);
  }

  private static boolean isFieldGetMethod(String methodName) {
    return methodName.startsWith("get");
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
    return protoMessageReceiverMatcher.matches(((MethodInvocationTree) tree), state);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!CHECK_NOT_NULL_MATCHER.matches(tree, state) || tree.getArguments().isEmpty()) {
      return Description.NO_MATCH;
    }

    Fix fix;
    if (parentNode(Matchers.<MethodInvocationTree>kindIs(Kind.EXPRESSION_STATEMENT))
        .matches(tree, state)) {
      fix = SuggestedFix.delete(state.getPath().getParentPath().getLeaf());
    } else {
      fix = SuggestedFix.replace(tree, tree.getArguments().get(0).toString());
    }
    return describeMatch(tree, fix);
  }
}
