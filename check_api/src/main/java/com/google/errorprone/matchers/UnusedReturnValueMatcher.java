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
import static com.google.errorprone.matchers.Matchers.enclosingMethod;
import static com.google.errorprone.matchers.Matchers.enclosingNode;
import static com.google.errorprone.matchers.Matchers.expressionStatement;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isLastStatementInBlock;
import static com.google.errorprone.matchers.Matchers.isThrowingFunctionalInterface;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.methodCallInDeclarationOfThrowingRunnable;
import static com.google.errorprone.matchers.Matchers.nextStatement;
import static com.google.errorprone.matchers.Matchers.parentNode;
import static com.google.errorprone.matchers.Matchers.previousStatement;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.findEnclosingNode;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getResultType;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isVoidType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import java.util.stream.Stream;
import javax.lang.model.type.TypeKind;

/**
 * Matches expressions that invoke or reference a non-void method or constructor and which do not
 * use their return value and are not in a context where non-use of the return value is allowed.
 */
@CheckReturnValue
public final class UnusedReturnValueMatcher implements Matcher<ExpressionTree> {

  private static final ImmutableMap<AllowReason, Matcher<ExpressionTree>> ALLOW_MATCHERS =
      ImmutableMap.of(
          AllowReason.MOCKING_CALL, UnusedReturnValueMatcher::mockitoInvocation,
          AllowReason.EXCEPTION_TESTING, UnusedReturnValueMatcher::exceptionTesting,
          AllowReason.RETURNS_JAVA_LANG_VOID, UnusedReturnValueMatcher::returnsJavaLangVoid);

  private static final ImmutableSet<AllowReason> DISALLOW_EXCEPTION_TESTING =
      Sets.immutableEnumSet(
          Sets.filter(ALLOW_MATCHERS.keySet(), k -> !k.equals(AllowReason.EXCEPTION_TESTING)));

  /** Gets an instance of this matcher. */
  public static UnusedReturnValueMatcher get(boolean allowInExceptionThrowers) {
    return new UnusedReturnValueMatcher(
        allowInExceptionThrowers ? ALLOW_MATCHERS.keySet() : DISALLOW_EXCEPTION_TESTING);
  }

  private final ImmutableSet<AllowReason> validAllowReasons;

  private UnusedReturnValueMatcher(ImmutableSet<AllowReason> validAllowReasons) {
    this.validAllowReasons = validAllowReasons;
  }

  @Override
  public boolean matches(ExpressionTree tree, VisitorState state) {
    return isReturnValueUnused(tree, state) && !isAllowed(tree, state);
  }

  private static boolean isVoidMethod(MethodSymbol symbol) {
    return !symbol.isConstructor() && isVoid(symbol.getReturnType());
  }

  private static boolean isVoid(Type type) {
    return type.getKind() == TypeKind.VOID;
  }

  private static boolean implementsVoidMethod(ExpressionTree tree, VisitorState state) {
    return isVoid(state.getTypes().findDescriptorType(getType(tree)).getReturnType());
  }

  /**
   * Returns {@code true} if and only if the given {@code tree} is an invocation of or reference to
   * a constructor or non-{@code void} method for which the return value is considered unused.
   */
  public static boolean isReturnValueUnused(ExpressionTree tree, VisitorState state) {
    Symbol sym = getSymbol(tree);
    if (!(sym instanceof MethodSymbol) || isVoidMethod((MethodSymbol) sym)) {
      return false;
    }
    if (tree instanceof MemberReferenceTree) {
      // Runnable r = foo::getBar;
      return implementsVoidMethod(tree, state);
    }
    Tree parent = state.getPath().getParentPath().getLeaf();
    return parent instanceof LambdaExpressionTree
        // Runnable r = () -> foo.getBar();
        ? implementsVoidMethod((LambdaExpressionTree) parent, state)
        // foo.getBar();
        : parent.getKind() == Kind.EXPRESSION_STATEMENT;
  }

  /**
   * Returns {@code true} if the given expression is allowed to have an unused return value based on
   * its context.
   */
  public boolean isAllowed(ExpressionTree tree, VisitorState state) {
    return getAllowReasons(tree, state).findAny().isPresent();
  }

  /**
   * Returns a stream of reasons the given expression is allowed to have an unused return value
   * based on its context.
   */
  public Stream<AllowReason> getAllowReasons(ExpressionTree tree, VisitorState state) {
    return validAllowReasons.stream()
        .filter(reason -> ALLOW_MATCHERS.get(reason).matches(tree, state));
  }

  private static boolean returnsJavaLangVoid(ExpressionTree tree, VisitorState state) {
    return tree instanceof MemberReferenceTree
        ? returnsJavaLangVoid((MemberReferenceTree) tree, state)
        : isVoidType(getResultType(tree), state);
  }

  private static boolean returnsJavaLangVoid(MemberReferenceTree tree, VisitorState state) {
    if (tree.getMode() == ReferenceMode.NEW) {
      // constructors can't return java.lang.Void
      return false;
    }

    // We need to do this to get the correct return type for things like future::get when future
    // is a Future<Void>.
    // - The Type of the method reference is the functional interface type it's implementing.
    // - The Symbol is the declared method symbol, i.e. V get().
    // So we resolve the symbol (V get()) as a member of the qualifier type (Future<Void>) to get
    // the method type (Void get()) and then look at the return type of that.
    Type type =
        state.getTypes().memberType(getType(tree.getQualifierExpression()), getSymbol(tree));
    // TODO(cgdecker): There are probably other types than MethodType that we could resolve here
    return type instanceof MethodType && isVoidType(type.getReturnType(), state);
  }

  private static boolean exceptionTesting(ExpressionTree tree, VisitorState state) {
    return tree instanceof MemberReferenceTree
        ? isThrowingFunctionalInterface(getType(tree), state)
        : expectedExceptionTest(state);
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
              (t, s) -> methodCallInDeclarationOfThrowingRunnable(s)),
          // @Test(expected = FooException.class) void bah() { me(); }
          allOf(
              UnusedReturnValueMatcher::isOnlyStatementInBlock,
              enclosingMethod(UnusedReturnValueMatcher::isTestExpectedExceptionMethod)));

  private static boolean isTestExpectedExceptionMethod(MethodTree tree, VisitorState state) {
    if (!JUnitMatchers.wouldRunInJUnit4.matches(tree, state)) {
      return false;
    }

    return getSymbol(tree).getAnnotationMirrors().stream()
        .filter(am -> am.type.tsym.getQualifiedName().contentEquals("org.junit.Test"))
        .findFirst()
        .flatMap(testAm -> MoreAnnotations.getAnnotationValue(testAm, "expected"))
        .flatMap(MoreAnnotations::asTypeValue)
        .filter(tv -> !tv.toString().equals("org.junit.Test.None"))
        .isPresent();
  }

  private static boolean isOnlyStatementInBlock(StatementTree t, VisitorState s) {
    BlockTree parentBlock = ASTHelpers.findEnclosingNode(s.getPath(), BlockTree.class);
    return parentBlock != null
        && parentBlock.getStatements().size() == 1
        && parentBlock.getStatements().get(0) == t;
  }

  /** Allow return values to be ignored in tests that expect an exception to be thrown. */
  public static boolean expectedExceptionTest(VisitorState state) {
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

  /**
   * Enumeration of known reasons that an unused return value may be allowed because of the context
   * in which the method is used. Suppression is not considered here; these are reasons that don't
   * have anything to do with specific checkers.
   */
  public enum AllowReason {
    /**
     * The context is one in which the method is probably being called to test for an exception it
     * throws.
     */
    EXCEPTION_TESTING,
    /** The context is a mocking call such as in {@code verify(foo).getBar();}. */
    MOCKING_CALL,
    /** The method returns {@code java.lang.Void} at this use-site. */
    RETURNS_JAVA_LANG_VOID
  }
}
