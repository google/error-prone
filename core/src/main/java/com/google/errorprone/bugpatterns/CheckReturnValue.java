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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingNode;
import static com.google.errorprone.matchers.Matchers.expressionStatement;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.nextStatement;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.enclosingPackage;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;

import com.google.common.base.Optional;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;

import javax.lang.model.element.ElementKind;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(
  name = "CheckReturnValue",
  altNames = {"ResultOfMethodCallIgnored", "ReturnValueIgnored"},
  summary = "Ignored return value of method that is annotated with @CheckReturnValue",
  category = JDK,
  severity = ERROR,
  maturity = MATURE
)
public class CheckReturnValue extends AbstractReturnValueIgnored
    implements MethodTreeMatcher, ClassTreeMatcher {

  private static Optional<Boolean> shouldCheckReturnValue(Symbol sym, VisitorState state) {
    if (hasAnnotation(sym, CanIgnoreReturnValue.class, state)) {
      return Optional.of(false);
    }
    if (hasAnnotation(sym, javax.annotation.CheckReturnValue.class, state)
        || hasAnnotationString(sym, "CheckReturnValue")) {
      return Optional.of(true);
    }
    return Optional.absent();
  }

  private static Optional<Boolean> checkEnclosingClasses(MethodSymbol method, VisitorState state) {
    Symbol enclosingClass = enclosingClass(method);
    while (enclosingClass instanceof ClassSymbol) {
      Optional<Boolean> result = shouldCheckReturnValue(enclosingClass, state);
      if (result.isPresent()) {
        return result;
      }
      enclosingClass = enclosingClass.owner;
    }
    return Optional.absent();
  }

  private static Optional<Boolean> checkPackage(MethodSymbol method, VisitorState state) {
    return shouldCheckReturnValue(enclosingPackage(method), state);
  }

  private static final Matcher<MethodInvocationTree> MATCHER =
      new Matcher<MethodInvocationTree>() {
        @Override
        public boolean matches(MethodInvocationTree tree, VisitorState state) {

          MethodSymbol method = ASTHelpers.getSymbol(tree);
          if (ASTHelpers.isVoidType(method.getReturnType(), state)) {
            return false;
          }

          if (mockitoInvocation(tree, state)) {
            return false;
          }

          // Allow unused return values in tests that check for thrown exceptions, e.g.:
          //
          // try {
          //   Foo.newFoo(-1);
          //   fail();
          // } catch (IllegalArgumentException expected) {
          // }
          //
          StatementTree statement =
              ASTHelpers.findEnclosingNode(state.getPath(), StatementTree.class);
          if (statement != null && EXPECTED_EXCEPTION_MATCHER.matches(statement, state)) {
            return false;
          }

          Optional<Boolean> result = shouldCheckReturnValue(method, state);
          if (result.isPresent()) {
            return result.get();
          }

          result = checkEnclosingClasses(method, state);
          if (result.isPresent()) {
            return result.get();
          }

          result = checkPackage(method, state);
          if (result.isPresent()) {
            return result.get();
          }

          return false;
        }
      };

  private static final Matcher<ExpressionTree> MOCKITO_MATCHER =
      anyOf(
          staticMethod().onClass("org.mockito.Mockito").named("verify"),
          instanceMethod().onDescendantOf("org.mockito.stubbing.Stubber").named("when"),
          instanceMethod().onDescendantOf("org.mockito.InOrder").named("verify"));

  /**
   * Return a matcher for method invocations in which the method being called has the
   * {@code @CheckReturnValue} annotation.
   */
  @Override
  public Matcher<MethodInvocationTree> specializedMatcher() {
    return MATCHER;
  }

  private static final String BOTH_ERROR =
      "@CheckReturnValue and @CanIgnoreReturnValue cannot both be applied to the same %s";

  /**
   * Validate {@link javax.annotation.CheckReturnValue} and {@link CanIgnoreReturnValue} usage on
   * methods.
   *
   * <p>The annotations should not both be appled to the same method.
   *
   * <p>The annotations should not be applied to void-returning methods. Doing so makes no sense,
   * because there is no return value to check.
   */
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol method = ASTHelpers.getSymbol(tree);

    boolean checkReturn = hasAnnotation(method, javax.annotation.CheckReturnValue.class, state)
        || hasAnnotationString(method, "CheckReturnValue");
    boolean canIgnore = hasAnnotation(method, CanIgnoreReturnValue.class, state);

    if (checkReturn && canIgnore) {
      return buildDescription(tree).setMessage(String.format(BOTH_ERROR, "method")).build();
    }

    String annotationToValidate;
    if (checkReturn) {
      annotationToValidate = javax.annotation.CheckReturnValue.class.getSimpleName();
    } else if (canIgnore) {
      annotationToValidate = CanIgnoreReturnValue.class.getSimpleName();
    } else {
      return Description.NO_MATCH;
    }
    if (method.getKind() != ElementKind.METHOD) {
      // skip contructors (which javac thinks are void-returning)
      return Description.NO_MATCH;
    }
    if (!ASTHelpers.isVoidType(method.getReturnType(), state)) {
      return Description.NO_MATCH;
    }
    String message =
        String.format("@%s may not be applied to void-returning methods", annotationToValidate);
    return buildDescription(tree).setMessage(message).build();
  }

  /**
   * Validate that at most one of {@link javax.annotation.CheckReturnValue} and
   * {@link CanIgnoreReturnValue} are applied to a class (or interface or enum).
   */
  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if ((hasAnnotation(tree, javax.annotation.CheckReturnValue.class, state)
         || hasAnnotationString(ASTHelpers.getSymbol(tree), "CheckReturnValue"))
        && hasAnnotation(tree, CanIgnoreReturnValue.class, state)) {
      return buildDescription(tree).setMessage(String.format(BOTH_ERROR, "class")).build();
    }
    return Description.NO_MATCH;
  }

  /**
   * Don't match the method that is invoked through {@code Mockito.verify(t)} or
   * {@code doReturn(val).when(t)}.
   */
  private static boolean mockitoInvocation(MethodInvocationTree tree, VisitorState state) {
    if (tree instanceof JCMethodInvocation
        && ((JCMethodInvocation) tree).getMethodSelect() instanceof JCFieldAccess) {
      ExpressionTree receiver = ASTHelpers.getReceiver(tree);
      if (MOCKITO_MATCHER.matches(receiver, state)) {
          return true;
      }
    }
    return false;
  }

  static final Matcher<ExpressionTree> FAIL_METHOD =
      anyOf(
          instanceMethod().onDescendantOf("com.google.common.truth.AbstractVerb").named("fail"),
          staticMethod().onClass("org.junit.Assert").named("fail"),
          staticMethod().onClass("junit.framework.Assert").named("fail"),
          staticMethod().onClass("junit.framework.TestCase").named("fail"));

  static final Matcher<StatementTree> EXPECTED_EXCEPTION_MATCHER =
      allOf(enclosingNode(kindIs(Kind.TRY)), nextStatement(expressionStatement(FAIL_METHOD)));

  /** Check if a symbol has an annotation by comparing its not fully qualified name. */
  private static boolean hasAnnotationString(Symbol sym, String annotation) {
    for (Attribute.Compound compound : sym.getAnnotationMirrors()) {
      String str = compound.type.toString();
      int index = str.lastIndexOf('.');
      if (index != -1) {
        str = str.substring(index + 1);
      }
      if (str.equals(annotation)) {
          return true;
      }
    }
    return false;
  }
}
