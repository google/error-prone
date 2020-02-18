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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingNode;
import static com.google.errorprone.matchers.Matchers.expressionStatement;
import static com.google.errorprone.matchers.Matchers.isLastStatementInBlock;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.nextStatement;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.parentNode;
import static com.google.errorprone.matchers.Matchers.previousStatement;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isVoidType;

import com.google.common.base.Suppliers;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import javax.lang.model.type.TypeKind;

/**
 * An abstract base class to match method invocations in which the return value is not used.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public abstract class AbstractReturnValueIgnored extends BugChecker
    implements MethodInvocationTreeMatcher, MemberReferenceTreeMatcher {
  private final java.util.function.Supplier<Matcher<ExpressionTree>> methodInvocationMatcher =
      Suppliers.memoize(
          () ->
              allOf(
                  parentNode(
                      anyOf(
                          AbstractReturnValueIgnored::isVoidReturningLambdaExpression,
                          kindIs(Kind.EXPRESSION_STATEMENT))),
                  not((t, s) -> isVoidType(getType(t), s)),
                  specializedMatcher(),
                  not(AbstractReturnValueIgnored::mockitoInvocation),
                  not((t, s) -> allowInExceptionThrowers() && expectedExceptionTest(t, s))));

  private final java.util.function.Supplier<Matcher<MemberReferenceTree>>
      memberReferenceTreeMatcher =
          Suppliers.memoize(
              () ->
                  allOf(
                      (t, s) -> t.getMode() == ReferenceMode.INVOKE,
                      AbstractReturnValueIgnored::isVoidReturningMethodReferenceExpression,
                      // Skip cases where the method we're referencing really does return void.
                      // We're only looking for cases where the referenced method does not return
                      // void, but it's being used on a void-returning functional interface.
                      not((t, s) -> isVoidType(getSymbol(t).getReturnType(), s)),
                      not(
                          (t, s) ->
                              allowInExceptionThrowers()
                                  && Matchers.isThrowingFunctionalInterface(
                                      ASTHelpers.getType(t), s)),
                      specializedMatcher()));

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    return methodInvocationMatcher.get().matches(methodInvocationTree, state)
        ? describeReturnValueIgnored(methodInvocationTree, state)
        : NO_MATCH;
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    return memberReferenceTreeMatcher.get().matches(tree, state)
        ? describeReturnValueIgnored(tree, state)
        : NO_MATCH;
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

  /**
   * Match whatever additional conditions concrete subclasses want to match (a list of known
   * side-effect-free methods, has a @CheckReturnValue annotation, etc.).
   */
  protected abstract Matcher<? super ExpressionTree> specializedMatcher();

  /**
   * Override this to return false to forbid discarding return values in testers that are testing
   * whether an exception is thrown.
   */
  protected boolean allowInExceptionThrowers() {
    return true;
  }

  /**
   * Fixes the error by assigning the result of the call to the receiver reference, or deleting the
   * method call. Subclasses may override if they prefer a different description.
   */
  protected Description describeReturnValueIgnored(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    // Find the root of the field access chain, i.e. a.intern().trim() ==> a.
    ExpressionTree identifierExpr = ASTHelpers.getRootAssignable(methodInvocationTree);
    Type identifierType = null;
    if (identifierExpr != null) {
      if (identifierExpr instanceof JCIdent) {
        identifierType = ((JCIdent) identifierExpr).sym.type;
      } else if (identifierExpr instanceof JCFieldAccess) {
        identifierType = ((JCFieldAccess) identifierExpr).sym.type;
      } else {
        throw new IllegalStateException("Expected a JCIdent or a JCFieldAccess");
      }
    }

    Type returnType =
        ASTHelpers.getReturnType(((JCMethodInvocation) methodInvocationTree).getMethodSelect());

    Fix fix;
    Symbol symbol = getSymbol(identifierExpr);
    if (identifierExpr != null
        && symbol != null
        && !symbol.name.contentEquals("this")
        && returnType != null
        && state.getTypes().isAssignable(returnType, identifierType)) {
      // Fix by assigning the assigning the result of the call to the root receiver reference.
      fix =
          SuggestedFix.prefixWith(
              methodInvocationTree, state.getSourceForNode(identifierExpr) + " = ");
    } else {
      // Unclear what the programmer intended.  Delete since we don't know what else to do.
      Tree parent = state.getPath().getParentPath().getLeaf();
      fix = SuggestedFix.delete(parent);
    }
    return describeMatch(methodInvocationTree, fix);
  }

  /**
   * Uses the default description for results ignored via a method reference. Subclasses may
   * override if they prefer a different description.
   */
  protected Description describeReturnValueIgnored(
      MemberReferenceTree memberReferenceTree, VisitorState state) {
    return describeMatch(memberReferenceTree);
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
                      anyOf(
                          instanceMethod().onExactClass("org.junit.rules.ExpectedException")
                          )))),
          // try { me(); fail(); } catch (Throwable t) {}
          allOf(enclosingNode(kindIs(Kind.TRY)), nextStatement(expressionStatement(FAIL_METHOD))),
          // assertThrows(Throwable.class, () => { me(); })
          allOf(
              anyOf(isLastStatementInBlock(), parentNode(kindIs(Kind.LAMBDA_EXPRESSION))),
              // Within the context of a ThrowingRunnable/Executable:
              (t, s) -> Matchers.methodCallInDeclarationOfThrowingRunnable(s)));

  /** Allow return values to be ignored in tests that expect an exception to be thrown. */
  static boolean expectedExceptionTest(Tree tree, VisitorState state) {
    // Allow unused return values in tests that check for thrown exceptions, e.g.:
    //
    // try {
    //   Foo.newFoo(-1);
    //   fail();
    // } catch (IllegalArgumentException expected) {
    // }
    //
    StatementTree statement = ASTHelpers.findEnclosingNode(state.getPath(), StatementTree.class);
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
  static boolean mockitoInvocation(Tree tree, VisitorState state) {
    if (!(tree instanceof JCMethodInvocation)) {
      return false;
    }
    JCMethodInvocation invocation = (JCMethodInvocation) tree;
    if (!(invocation.getMethodSelect() instanceof JCFieldAccess)) {
      return false;
    }
    ExpressionTree receiver = ASTHelpers.getReceiver(invocation);
    return MOCKITO_MATCHER.matches(receiver, state);
  }
}
