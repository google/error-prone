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

import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.contains;
import static com.google.errorprone.matchers.Matchers.enclosingMethod;
import static com.google.errorprone.matchers.Matchers.enclosingNode;
import static com.google.errorprone.matchers.Matchers.expressionStatement;
import static com.google.errorprone.matchers.Matchers.isLastStatementInBlock;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.methodInvocation;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.nextStatement;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.parentNode;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import java.util.regex.Pattern;

/**
 * An abstract base class to match method invocations in which the return value is not used.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public abstract class AbstractReturnValueIgnored extends BugChecker
    implements MethodInvocationTreeMatcher {

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    if (allOf(
            parentNode(Matchers.<MethodInvocationTree>kindIs(Kind.EXPRESSION_STATEMENT)),
            not(
                methodSelect(
                    Matchers.<ExpressionTree>allOf(
                        kindIs(Kind.IDENTIFIER), identifierHasName("super")))),
            not((t, s) -> ASTHelpers.isVoidType(ASTHelpers.getType(t), s)),
            not(AbstractReturnValueIgnored::expectedExceptionTest),
            specializedMatcher())
        .matches(methodInvocationTree, state)) {
      return describe(methodInvocationTree, state);
    }
    return Description.NO_MATCH;
  }

  /**
   * Match whatever additional conditions concrete subclasses want to match (a list of known
   * side-effect-free methods, has a @CheckReturnValue annotation, etc.).
   */
  public abstract Matcher<? super MethodInvocationTree> specializedMatcher();

  private static Matcher<ExpressionTree> identifierHasName(final String name) {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree item, VisitorState state) {
        return ((IdentifierTree) item).getName().contentEquals(name);
      }
    };
  }

  /**
   * Fixes the error by assigning the result of the call to the receiver reference, or deleting the
   * method call.
   */
  public Description describe(MethodInvocationTree methodInvocationTree, VisitorState state) {
    // Find the root of the field access chain, i.e. a.intern().trim() ==> a.
    ExpressionTree identifierExpr = ASTHelpers.getRootAssignable(methodInvocationTree);
    String identifierStr = null;
    Type identifierType = null;
    if (identifierExpr != null) {
      identifierStr = identifierExpr.toString();
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
    if (identifierStr != null
        && !"this".equals(identifierStr)
        && returnType != null
        && state.getTypes().isAssignable(returnType, identifierType)) {
      // Fix by assigning the assigning the result of the call to the root receiver reference.
      fix = SuggestedFix.prefixWith(methodInvocationTree, identifierStr + " = ");
    } else {
      // Unclear what the programmer intended.  Delete since we don't know what else to do.
      Tree parent = state.getPath().getParentPath().getLeaf();
      fix = SuggestedFix.delete(parent);
    }
    return describeMatch(methodInvocationTree, fix);
  }

  /** Allow return values to be ignored in tests that expect an exception to be thrown. */
  static boolean expectedExceptionTest(Tree tree, VisitorState state) {
    if (mockitoInvocation(tree, state)) {
      return true;
    }

    // Allow unused return values in tests that check for thrown exceptions, e.g.:
    //
    // try {
    //   Foo.newFoo(-1);
    //   fail();
    // } catch (IllegalArgumentException expected) {
    // }
    //
    StatementTree statement = ASTHelpers.findEnclosingNode(state.getPath(), StatementTree.class);
    if (statement != null && EXPECTED_EXCEPTION_MATCHER.matches(statement, state)) {
      return true;
    }
    return false;
  }

  private static final Matcher<ExpressionTree> FAIL_METHOD =
      anyOf(
          instanceMethod().onDescendantOf("com.google.common.truth.AbstractVerb").named("fail"),
          staticMethod().onClass("org.junit.Assert").named("fail"),
          staticMethod().onClass("junit.framework.Assert").named("fail"),
          staticMethod().onClass("junit.framework.TestCase").named("fail"));

  private static final Matcher<ExpressionTree> EXPECT_THROWS =
      anyOf(
          // JUnit 4
          staticMethod().onClass("org.junit.Assert").named("assertThrows"),
          staticMethod().onClass("org.junit.Assert").named("expectThrows"),
          // JUnit 5
          staticMethod().onClass("org.junit.jupiter.api.Assertions").named("assertThrows"),
          staticMethod().onClass("org.junit.jupiter.api.Assertions").named("expectThrows"));

  private static final Matcher<StatementTree> EXPECTED_EXCEPTION_MATCHER =
      anyOf(
          // expectedException.expect(Foo.class); me();
          allOf(
              isLastStatementInBlock(),
              enclosingMethod(
                  contains(
                      ExpressionTree.class,
                      methodInvocation(
                          instanceMethod()
                              .onDescendantOf("org.junit.rules.TestRule")
                              .withNameMatching(Pattern.compile("expect(Message|Cause)?")))))),
          // try { me(); fail(); } catch (Throwable t) {}
          allOf(enclosingNode(kindIs(Kind.TRY)), nextStatement(expressionStatement(FAIL_METHOD))),
          // assertThrows(Throwable.class, () => { me(); })
          allOf(
              isLastStatementInBlock(),
              enclosingNode(
                  // Extra kindIs is needed as methodInvocation will cast each parent node to
                  // ExpressionTree.
                  allOf(kindIs(Kind.METHOD_INVOCATION), methodInvocation(EXPECT_THROWS)))));

  private static final Matcher<ExpressionTree> MOCKITO_MATCHER =
      anyOf(
          staticMethod().onClass("org.mockito.Mockito").named("verify"),
          instanceMethod().onDescendantOf("org.mockito.stubbing.Stubber").named("when"),
          instanceMethod().onDescendantOf("org.mockito.InOrder").named("verify"));

  /**
   * Don't match the method that is invoked through {@code Mockito.verify(t)} or {@code
   * doReturn(val).when(t)}.
   */
  private static boolean mockitoInvocation(Tree tree, VisitorState state) {
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
