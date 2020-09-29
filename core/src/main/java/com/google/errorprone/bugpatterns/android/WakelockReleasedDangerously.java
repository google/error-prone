/*
 * Copyright 2017 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.android;

import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.findEnclosingNode;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LambdaExpressionTree.BodyKind;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TryTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.UnionClassType;
import com.sun.tools.javac.code.Types;

/** @author epmjohnston@google.com */
@BugPattern(
    name = "WakelockReleasedDangerously",
    tags = StandardTags.FRAGILE_CODE,
    summary =
        "A wakelock acquired with a timeout may be released by the system before calling"
            + " `release`, even after checking `isHeld()`. If so, it will throw a RuntimeException."
            + " Please wrap in a try/catch block.",
    severity = SeverityLevel.WARNING)
public class WakelockReleasedDangerously extends BugChecker implements MethodInvocationTreeMatcher {

  private static final String WAKELOCK_CLASS_NAME = "android.os.PowerManager.WakeLock";
  private static final Matcher<ExpressionTree> RELEASE =
      MethodMatchers.instanceMethod().onExactClass(WAKELOCK_CLASS_NAME).named("release");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!state.isAndroidCompatible()) {
      return Description.NO_MATCH;
    }
    // Match on calls to any override of WakeLock.release().
    if (!RELEASE.matches(tree, state)) {
      return NO_MATCH;
    }

    // Ok if surrounded in try/catch block that catches RuntimeException.
    TryTree enclosingTry = findEnclosingNode(state.getPath(), TryTree.class);
    if (enclosingTry != null
        && tryCatchesException(enclosingTry, state.getSymtab().runtimeExceptionType, state)) {
      return NO_MATCH;
    }

    // Ok if WakeLock not in danger of unexpected exception.
    // Also, can't perform analysis if WakeLock symbol not found.
    Symbol wakelockSymbol = getSymbol(getReceiver(tree));
    if (wakelockSymbol == null || !wakelockMayThrow(wakelockSymbol, state)) {
      return NO_MATCH;
    }

    Tree releaseStatement = state.getPath().getParentPath().getLeaf();
    return describeMatch(releaseStatement, getFix(releaseStatement, wakelockSymbol, state));
  }

  private SuggestedFix getFix(Tree releaseStatement, Symbol wakelockSymbol, VisitorState state) {
    // Wrap the release call line in a try/catch(RuntimeException) block.
    String before = "\ntry {\n";
    String after =
        "\n} catch (RuntimeException unused) {\n"
            + "// Ignore: already released by timeout.\n"
            + "// TODO: Log this exception.\n"
            + "}\n";

    // Lambda expressions are special. If the release call is in a one-expression lambda,
    // only wrap body (not args) and convert to block lambda.
    if (releaseStatement.getKind() == Kind.LAMBDA_EXPRESSION) {
      LambdaExpressionTree enclosingLambda = (LambdaExpressionTree) releaseStatement;
      if (enclosingLambda.getBodyKind() == BodyKind.EXPRESSION) {
        releaseStatement = enclosingLambda.getBody();
        before = "{" + before;
        after = ";" + after + "}";
      }
    }

    // Remove `if (wakelock.isHeld())` check.
    // TODO(epmjohnston): can avoid this if no isHeld check in class (check call map).
    IfTree enclosingIfHeld = findEnclosingNode(state.getPath(), IfTree.class);
    if (enclosingIfHeld != null) {
      ExpressionTree condition = ASTHelpers.stripParentheses(enclosingIfHeld.getCondition());
      if (enclosingIfHeld.getElseStatement() == null
          && instanceMethod()
              .onExactClass(WAKELOCK_CLASS_NAME)
              .named("isHeld")
              .matches(condition, state)
          && wakelockSymbol.equals(getSymbol(getReceiver(condition)))) {
        String ifBody = state.getSourceForNode(enclosingIfHeld.getThenStatement()).trim();
        // Remove leading and trailing `{}`
        ifBody = ifBody.startsWith("{") ? ifBody.substring(1) : ifBody;
        ifBody = ifBody.endsWith("}") ? ifBody.substring(0, ifBody.length() - 1) : ifBody;
        ifBody = ifBody.trim();
        String releaseStatementSource = state.getSourceForNode(releaseStatement);
        return SuggestedFix.replace(
            enclosingIfHeld,
            ifBody.replace(releaseStatementSource, before + releaseStatementSource + after));
      }
    }
    return SuggestedFix.builder()
        .prefixWith(releaseStatement, before)
        .postfixWith(releaseStatement, after)
        .build();
  }

  /** Return whether the given try-tree will catch the given exception type. */
  private boolean tryCatchesException(TryTree tryTree, Type exceptionToCatch, VisitorState state) {
    Types types = state.getTypes();
    return tryTree.getCatches().stream()
        .anyMatch(
            (CatchTree catchClause) -> {
              Type catchesException = getType(catchClause.getParameter().getType());
              // Examine all alternative types of a union type.
              if (catchesException != null && catchesException.isUnion()) {
                return Streams.stream(((UnionClassType) catchesException).getAlternativeTypes())
                    .anyMatch(caught -> types.isSuperType(caught, exceptionToCatch));
              }
              // Simple type, just check superclass.
              return types.isSuperType(catchesException, exceptionToCatch);
            });
  }

  /**
   * Whether the given WakeLock may throw an unexpected RuntimeException when released.
   *
   * <p>Returns true if: 1) the given WakeLock was acquired with timeout, and 2) the given WakeLock
   * is reference counted.
   */
  private boolean wakelockMayThrow(Symbol wakelockSymbol, VisitorState state) {
    ClassTree enclosingClass = getTopLevelClass(state);
    ImmutableMultimap<String, MethodInvocationTree> map =
        methodCallsForSymbol(wakelockSymbol, enclosingClass);
    // Was acquired with timeout.
    return map.get("acquire").stream().anyMatch(m -> m.getArguments().size() == 1)
        // Is reference counted, i.e., referenceCounted not explicitly set to false.
        && map.get("setReferenceCounted").stream()
            .noneMatch(
                m -> Boolean.FALSE.equals(constValue(m.getArguments().get(0), Boolean.class)));
  }

  private ClassTree getTopLevelClass(VisitorState state) {
    return (ClassTree)
        Streams.findLast(
                Streams.stream(state.getPath().iterator())
                    .filter((Tree t) -> t.getKind() == Kind.CLASS))
            .orElseThrow(() -> new IllegalArgumentException("No enclosing class found"));
  }

  /**
   * Finds all method invocations on the given symbol, and constructs a map of the called method's
   * name, to the {@link MethodInvocationTree} in which it was called.
   */
  private ImmutableMultimap<String, MethodInvocationTree> methodCallsForSymbol(
      Symbol sym, ClassTree classTree) {
    ImmutableMultimap.Builder<String, MethodInvocationTree> methodMap = ImmutableMultimap.builder();
    // Populate map builder with names of method called : the tree in which it is called.
    classTree.accept(
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitMethodInvocation(MethodInvocationTree callTree, Void unused) {
            if (sym.equals(getSymbol(getReceiver(callTree)))) {
              MethodSymbol methodSymbol = getSymbol(callTree);
              if (methodSymbol != null) {
                methodMap.put(methodSymbol.getSimpleName().toString(), callTree);
              }
            }
            return super.visitMethodInvocation(callTree, unused);
          }
        },
        null);
    return methodMap.build();
  }
}
