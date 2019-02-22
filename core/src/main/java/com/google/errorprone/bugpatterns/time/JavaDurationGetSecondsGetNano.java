/*
 * Copyright 2018 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessageLite;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This checker warns about calls to {@code duration.getNano()} without a corresponding "nearby"
 * call to {@code duration.getSeconds()}.
 *
 * @author kak@google.com (Kurt Alfred Kluever)
 */
@BugPattern(
    name = "JavaDurationGetSecondsGetNano",
    summary =
        "duration.getNano() only accesses the underlying nanosecond adjustment from the whole "
            + "second.",
    explanation =
        "If you call duration.getNano(), you must also call duration.getSeconds() in 'nearby' code."
            + " If you are trying to convert this duration to nanoseconds, you probably meant to"
            + " use duration.toNanos() instead.",
    severity = WARNING)
public final class JavaDurationGetSecondsGetNano extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> GET_SECONDS =
      instanceMethod().onExactClass("java.time.Duration").named("getSeconds");
  private static final Matcher<ExpressionTree> GET_NANO =
      allOf(
          instanceMethod().onExactClass("java.time.Duration").named("getNano"),
          Matchers.not(Matchers.packageStartsWith("java.")));

  // lifted from com.google.devtools.javatools.refactory.refaster.cleanups.proto.ProtoMatchers
  private static final Matcher<ExpressionTree> IS_IMMUTABLE_PROTO_GETTER =
      instanceMethod()
          .onDescendantOfAny(GeneratedMessage.class.getName(), GeneratedMessageLite.class.getName())
          .withNameMatching(Pattern.compile("get(?!CachedSize$|SerializedSize$).+"))
          .withParameters();

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (GET_NANO.matches(tree, state)) {
      if (!containsGetSecondsCallInNearbyCode(
          tree, state, GET_SECONDS, /*checkProtoChains=*/ false)) {
        return describeMatch(tree);
      }
    }
    return Description.NO_MATCH;
  }

  /**
   * Returns whether or not there is a "nearby" {@code getSeconds()} method call.
   *
   * @param nanoTree the tree that contains the {@code getNanos()} method call
   * @param state the visitor state of this matcher
   * @param secondsMatcher the matcher that matches the appropriate {@code getSeconds()} call
   * @param checkProtoChains whether or not to check a chain of protobuf getter methods for equality
   *     (e.g., {@code a.getB().getC().getSeconds()} and {@code a.getB().getC().getNanos()}
   */
  static boolean containsGetSecondsCallInNearbyCode(
      MethodInvocationTree nanoTree,
      VisitorState state,
      Matcher<ExpressionTree> secondsMatcher,
      boolean checkProtoChains) {
    ExpressionTree getNanoReceiver = ASTHelpers.getReceiver(nanoTree);
    TreeScanner<Boolean, Void> scanner =
        new TreeScanner<Boolean, Void>() {
          @Override
          public Boolean reduce(Boolean r1, Boolean r2) {
            return (r1 == null ? false : r1) || (r2 == null ? false : r2);
          }

          @Override
          public Boolean visitLambdaExpression(LambdaExpressionTree node, Void unused) {
            return false;
          }

          @Override
          public Boolean visitMethodInvocation(MethodInvocationTree tree, Void unused) {
            if (super.visitMethodInvocation(tree, unused)) {
              return true;
            }
            if (tree != null && secondsMatcher.matches(tree, state)) {
              ExpressionTree getSecondsReceiver = ASTHelpers.getReceiver(tree);
              if (getSecondsReceiver != null) {
                // if the methods are being invoked directly on the same variable...
                if (getNanoReceiver != null
                    && getSecondsReceiver != null
                    && ASTHelpers.sameVariable(getNanoReceiver, getSecondsReceiver)) {
                  return true;
                }
                if (!checkProtoChains) {
                  return false;
                }
                // now check if the root variables of the invocations are the same...
                ExpressionTree treeRootAssignable = ASTHelpers.getRootAssignable(tree);
                ExpressionTree nanoTreeRootAssignable = ASTHelpers.getRootAssignable(nanoTree);
                if (treeRootAssignable != null
                    && nanoTreeRootAssignable != null
                    && ASTHelpers.sameVariable(treeRootAssignable, nanoTreeRootAssignable)) {

                  // build up a list of method invocations for both invocations

                  List<Symbol> secondsChain = new ArrayList<>();
                  boolean allProtoGettersForSeconds = buildChain(tree, state, secondsChain);

                  List<Symbol> nanosChain = new ArrayList<>();
                  boolean allProtoGettersForNanos = buildChain(nanoTree, state, nanosChain);

                  // if we saw a non protobuf getter in the chain, we have to return false
                  if (!allProtoGettersForSeconds || !allProtoGettersForNanos) {
                    return false;
                  }

                  if (secondsChain.equals(nanosChain)) {
                    return true;
                  }
                }
              }
            }
            return false;
          }
        };
    ImmutableList<Tree> treesToScan = getNearbyTreesToScan(state);
    return treesToScan.isEmpty() ? false : scanner.scan(treesToScan, null);
  }

  // passing in output variables? what is this C++?
  private static boolean buildChain(ExpressionTree expr, VisitorState state, List<Symbol> chain) {
    while (expr instanceof JCMethodInvocation) {
      expr = ((JCMethodInvocation) expr).getMethodSelect();
      // if the method isn't an immutable protobuf getter, return false
      if (!IS_IMMUTABLE_PROTO_GETTER.matches(expr, state)) {
        return false;
      }
      if (expr instanceof JCFieldAccess) {
        expr = ((JCFieldAccess) expr).getExpression();
      }
      chain.add(ASTHelpers.getSymbol(expr));
    }
    return true;
  }

  private static ImmutableList<Tree> getNearbyTreesToScan(VisitorState state) {
    for (Tree parent : state.getPath()) {
      // if we get all the way up to the class tree, then _only_ scan the other class-level fields
      if (parent.getKind() == Tree.Kind.CLASS) {
        ImmutableList.Builder<Tree> treesToScan = ImmutableList.builder();
        for (Tree member : ((ClassTree) parent).getMembers()) {
          if (member instanceof VariableTree) {
            ExpressionTree expressionTree = ((VariableTree) member).getInitializer();
            if (expressionTree != null) {
              treesToScan.add(expressionTree);
            }
          }
        }
        return treesToScan.build();
      }
      // if we reach a block tree, then _only_ scan that block
      if (parent.getKind() == Tree.Kind.BLOCK) {
        return ImmutableList.of(parent);
      }
      // if we reach a lambda tree, then don't scan anything since we don't know where/when that
      // lambda will actually be executed
      if (parent.getKind() == Tree.Kind.LAMBDA_EXPRESSION) {
        return ImmutableList.of();
      }
    }
    return ImmutableList.of();
  }
}
