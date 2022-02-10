/*
 * Copyright 2012 The Error Prone Authors.
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

package com.google.errorprone.matchers;

import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingNode;
import static com.google.errorprone.matchers.Matchers.expressionStatement;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isLastStatementInBlock;
import static com.google.errorprone.matchers.Matchers.isThrowingFunctionalInterface;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.methodCallInDeclarationOfThrowingRunnable;
import static com.google.errorprone.matchers.Matchers.nextStatement;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.parentNode;
import static com.google.errorprone.matchers.Matchers.previousStatement;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.findEnclosingNode;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isVoidType;

import com.google.common.base.Suppliers;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import java.util.function.Supplier;
import javax.lang.model.type.TypeKind;

/**
 * Matches expressions that invoke or reference a non-void method or constructor and which do not
 * use their return value and are not in a context where non-use of the return value is allowed.
 */
@CheckReturnValue
public final class UnusedReturnValueMatcher implements Matcher<ExpressionTree> {

  /** Gets an instance of this matcher. */
  public static UnusedReturnValueMatcher get(boolean allowInExceptionThrowers) {
    return new UnusedReturnValueMatcher(allowInExceptionThrowers);
  }

  private final Supplier<Matcher<ExpressionTree>> methodInvocationMatcher =
      Suppliers.memoize(
          () ->
              allOf(
                  parentNode(
                      anyOf(
                          UnusedReturnValueMatcher::isVoidReturningLambdaExpression,
                          kindIs(Kind.EXPRESSION_STATEMENT))),
                  not((t, s) -> isVoidType(getType(t), s)),
                  not(UnusedReturnValueMatcher::mockitoInvocation),
                  not((t, s) -> allowInExceptionThrowers() && expectedExceptionTest(t, s))));

  private final Supplier<Matcher<MemberReferenceTree>> memberReferenceTreeMatcher =
      Suppliers.memoize(
          () ->
              allOf(
                  UnusedReturnValueMatcher::isVoidReturningMethodReferenceExpression,
                  // Skip cases where the method we're referencing really does return void.
                  // We're only looking for cases where the referenced method does not return
                  // void, but it's being used on a void-returning functional interface.
                  not((t, s) -> isVoidReturningMethod(getSymbol(t), s)),
                  not(
                      (t, s) ->
                          allowInExceptionThrowers()
                              && isThrowingFunctionalInterface(ASTHelpers.getType(t), s))));

  private static boolean isVoidReturningMethod(MethodSymbol meth, VisitorState state) {
    // Constructors "return" void but produce a real non-void value.
    return !meth.isConstructor() && isVoidType(meth.getReturnType(), state);
  }

  private static boolean isVoidReturningMethodReferenceExpression(
      MemberReferenceTree tree, VisitorState state) {
    return functionalInterfaceReturnsExactlyVoid(ASTHelpers.getType(tree), state);
  }

  private static boolean isVoidReturningLambdaExpression(Tree tree, VisitorState state) {
    return tree instanceof LambdaExpressionTree
        && functionalInterfaceReturnsExactlyVoid(getType(tree), state);
  }

  /**
   * Checks that the return value of a functional interface is void. Note, we do not use
   * ASTHelpers.isVoidType here, return values of Void are actually type-checked. Only
   * void-returning functions silently ignore return values of any type.
   */
  private static boolean functionalInterfaceReturnsExactlyVoid(
      Type interfaceType, VisitorState state) {
    return state.getTypes().findDescriptorType(interfaceType).getReturnType().getKind()
        == TypeKind.VOID;
  }

  private final boolean allowInExceptionThrowers;

  private UnusedReturnValueMatcher(boolean allowInExceptionThrowers) {
    this.allowInExceptionThrowers = allowInExceptionThrowers;
  }

  private boolean allowInExceptionThrowers() {
    return allowInExceptionThrowers;
  }

  @Override
  public boolean matches(ExpressionTree tree, VisitorState state) {
    return tree instanceof MemberReferenceTree
        ? memberReferenceTreeMatcher.get().matches((MemberReferenceTree) tree, state)
        : methodInvocationMatcher.get().matches(tree, state);
  }

  private static final Matcher<ExpressionTree> FAIL_METHOD =
      anyOf(
          instanceMethod().onDescendantOf("com.google.common.truth.AbstractVerb").named("fail"),
          instanceMethod()
              .onDescendantOf("com.google.common.truth.StandardSubjectBuilder")
              .named("fail"),
          staticMethod().onClass("org.junit.Assert").named("fail"),
          staticMethod().onClass("junit.framework.Assert").named("fail"),
          staticMethod().onClass("junit.framework.TestCase").named("fail"));

  private static final Matcher<StatementTree> EXPECTED_EXCEPTION_MATCHER =
      anyOf(
          // expectedException.expect(Foo.class); me();
          allOf(
              isLastStatementInBlock(),
              previousStatement(
                  expressionStatement(
                      anyOf(instanceMethod().onExactClass("org.junit.rules.ExpectedException"))))),
          // try { me(); fail(); } catch (Throwable t) {}
          allOf(enclosingNode(kindIs(Kind.TRY)), nextStatement(expressionStatement(FAIL_METHOD))),
          // assertThrows(Throwable.class, () => { me(); })
          allOf(
              anyOf(isLastStatementInBlock(), parentNode(kindIs(Kind.LAMBDA_EXPRESSION))),
              // Within the context of a ThrowingRunnable/Executable:
              (t, s) -> methodCallInDeclarationOfThrowingRunnable(s)));

  /** Allow return values to be ignored in tests that expect an exception to be thrown. */
  public static boolean expectedExceptionTest(ExpressionTree tree, VisitorState state) {
    // Allow unused return values in tests that check for thrown exceptions, e.g.:
    //
    // try {
    //   Foo.newFoo(-1);
    //   fail();
    // } catch (IllegalArgumentException expected) {
    // }
    //
    StatementTree statement = findEnclosingNode(state.getPath(), StatementTree.class);
    return statement != null && EXPECTED_EXCEPTION_MATCHER.matches(statement, state);
  }

  private static final Matcher<ExpressionTree> MOCKITO_MATCHER =
      anyOf(
          staticMethod().onClass("org.mockito.Mockito").named("verify"),
          instanceMethod().onDescendantOf("org.mockito.stubbing.Stubber").named("when"),
          instanceMethod().onDescendantOf("org.mockito.InOrder").named("verify"));

  /**
   * Don't match the method that is invoked through {@code Mockito.verify(t)} or {@code
   * doReturn(val).when(t)}.
   */
  public static boolean mockitoInvocation(Tree tree, VisitorState state) {
    if (!(tree instanceof JCMethodInvocation)) {
      return false;
    }
    JCMethodInvocation invocation = (JCMethodInvocation) tree;
    if (!(invocation.getMethodSelect() instanceof JCFieldAccess)) {
      return false;
    }
    ExpressionTree receiver = getReceiver(invocation);
    return MOCKITO_MATCHER.matches(receiver, state);
  }
}
