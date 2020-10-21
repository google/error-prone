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

import static com.google.common.collect.Multimaps.toMultimap;
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Streams;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
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
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import javax.lang.model.type.TypeKind;

/**
 * An abstract base class to match method invocations in which the return value is not used.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public abstract class AbstractReturnValueIgnored extends BugChecker
    implements MethodInvocationTreeMatcher, MemberReferenceTreeMatcher, ReturnTreeMatcher {
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
    Description description =
        methodInvocationMatcher.get().matches(methodInvocationTree, state)
            ? describeReturnValueIgnored(methodInvocationTree, state)
            : NO_MATCH;
    if (!description.equals(NO_MATCH)) {
      return description;
    }
    return checkLostType(methodInvocationTree, state);
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    Description description =
        memberReferenceTreeMatcher.get().matches(tree, state)
            ? describeReturnValueIgnored(tree, state)
            : NO_MATCH;
    if (!description.equals(NO_MATCH)) {
      return description;
    }
    if (allOf(
            (t, s) -> t.getMode() == ReferenceMode.INVOKE,
            AbstractReturnValueIgnored::isObjectReturningMethodReferenceExpression,
            not((t, s) -> isExemptedInterfaceType(ASTHelpers.getType(t), s)),
            not((t, s) -> Matchers.isThrowingFunctionalInterface(ASTHelpers.getType(t), s)),
            specializedMatcher())
        .matches(tree, state)) {
      return describeMatch(tree);
    }
    return description;
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

  /** Check for occurrences of this type being lost, i.e. cast to {@link Object}. */
  protected Optional<Type> lostType(VisitorState state) {
    return Optional.empty();
  }

  protected String lostTypeMessage(String returnedType, String declaredReturnType) {
    return String.format(
        "Returning %s from method that returns %s.", returnedType, declaredReturnType);
  }

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

  private Description checkLostType(MethodInvocationTree tree, VisitorState state) {
    Optional<Type> optionalType = lostType(state);
    if (!optionalType.isPresent()) {
      return Description.NO_MATCH;
    }

    Type lostType = optionalType.get();

    MethodSymbol sym = ASTHelpers.getSymbol(tree);
    Type returnType = ASTHelpers.getResultType(tree);
    Type returnedFutureType = state.getTypes().asSuper(returnType, lostType.tsym);
    if (returnedFutureType != null
        && !returnedFutureType.hasTag(TypeTag.ERROR) // work around error-prone#996
        && !returnedFutureType.isRaw()) {
      if (ASTHelpers.isSubtype(
          ASTHelpers.getUpperBound(returnedFutureType.getTypeArguments().get(0), state.getTypes()),
          lostType,
          state)) {
        return buildDescription(tree)
            .setMessage(String.format("Method returns a nested type, %s", returnType))
            .build();
      }

      // The type variable that determines the generic on the returned type was not an instance of
      // that type.
      // However, many methods (like guava's Futures.transform) have signatures like this:
      // Future<O> do(SomeObject<? extends O>). If O resolves to java.lang.Object or ?, then a
      // SomeObject<Future> is a valid parameter to pass, but results in a nested future.
      Type methodReturnType = sym.getReturnType();
      List<TypeVariableSymbol> typeParameters = sym.getTypeParameters();
      Set<TypeVariableSymbol> returnTypeChoosing = new HashSet<>();
      // For each type variable on the method, see if we can reach the type declared as the param
      // of the returned type, by traversing its type bounds. If we can reach it, we know that if
      // an argument is passed to an invocation of this method where the type variable is a subtype
      // of type, that means that a nested type is being returned.
      for (TypeVariableSymbol tvs : typeParameters) {
        Queue<TypeVariableSymbol> queue = new ArrayDeque<>();
        queue.add(tvs);
        while (!queue.isEmpty()) {
          TypeVariableSymbol currentTypeParam = queue.poll();
          for (Type typeParam : methodReturnType.getTypeArguments()) {
            if (typeParam.tsym == currentTypeParam) {
              returnTypeChoosing.add(tvs);
            }
          }
          for (Type toAdd : currentTypeParam.getBounds()) {
            if (toAdd.tsym instanceof TypeVariableSymbol) {
              queue.add((TypeVariableSymbol) toAdd.tsym);
            }
          }
        }
      }
      // If at least one of the method's type parameters is involved in determining the returned
      // type, check each passed parameter to ensure that it is never passed as a subtype
      // of the type.
      if (!returnTypeChoosing.isEmpty()) {
        Multimap<TypeVariableSymbol, TypeInfo> resolved = getResolvedGenerics(tree);
        for (TypeVariableSymbol returnTypeChoosingSymbol : returnTypeChoosing) {
          Collection<TypeInfo> types = resolved.get(returnTypeChoosingSymbol);
          for (TypeInfo type : types) {
            if (ASTHelpers.isSubtype(type.resolvedVariableType, lostType, state)) {
              return buildDescription(type.tree)
                  .setMessage(
                      String.format(
                          "Invocation produces a nested type - Type variable %s, as part of return "
                              + "type %s resolved to %s.",
                          returnTypeChoosingSymbol, methodReturnType, type.resolvedVariableType))
                  .build();
            }
          }
        }
      }
    }
    if (allOf(
            allOf(
                parentNode(AbstractReturnValueIgnored::isObjectReturningLambdaExpression),
                not(AbstractReturnValueIgnored::mockitoInvocation),
                not(AbstractReturnValueIgnored::expectedExceptionTest)),
            specializedMatcher(),
            not((t, s) -> ASTHelpers.isVoidType(ASTHelpers.getType(t), s)))
        .matches(tree, state)) {
      return describeReturnValueIgnored(tree, state);
    }

    return Description.NO_MATCH;
  }

  private static final class TypeInfo {

    private final TypeVariableSymbol sym;
    private final Type resolvedVariableType;
    private final Tree tree;

    private TypeInfo(TypeVariableSymbol sym, Type resolvedVariableType, Tree tree) {
      this.sym = sym;
      this.resolvedVariableType = resolvedVariableType;
      this.tree = tree;
    }
  }

  private static Multimap<TypeVariableSymbol, TypeInfo> getResolvedGenerics(
      MethodInvocationTree tree) {
    Type type = ASTHelpers.getType(tree.getMethodSelect());
    List<Type> from = new ArrayList<>();
    List<Type> to = new ArrayList<>();
    getSubst(type, from, to);
    Multimap<TypeVariableSymbol, TypeInfo> result =
        Streams.zip(
                from.stream(),
                to.stream(),
                (f, t) -> new TypeInfo((TypeVariableSymbol) f.asElement(), t, tree))
            .collect(
                toMultimap(
                    k -> k.sym, k -> k, MultimapBuilder.linkedHashKeys().arrayListValues()::build));
    return result;
  }

  @SuppressWarnings("unchecked")
  public static void getSubst(Type m, List<Type> from, List<Type> to) {
    try {
      // Reflectively extract the mapping from an enclosing instance of Types.Subst
      Field substField = m.getClass().getDeclaredField("this$0");
      substField.setAccessible(true);
      Object subst = substField.get(m);
      Field fromField = subst.getClass().getDeclaredField("from");
      Field toField = subst.getClass().getDeclaredField("to");
      fromField.setAccessible(true);
      toField.setAccessible(true);
      from.addAll((Collection<Type>) fromField.get(subst));
      to.addAll((Collection<Type>) toField.get(subst));
    } catch (ReflectiveOperationException e) {
      return;
    }
  }

  private static boolean isObjectReturningMethodReferenceExpression(
      MemberReferenceTree tree, VisitorState state) {
    return functionalInterfaceReturnsObject(ASTHelpers.getType(tree), state);
  }

  private static boolean isObjectReturningLambdaExpression(Tree tree, VisitorState state) {
    if (!(tree instanceof LambdaExpressionTree)) {
      return false;
    }

    Type type = ASTHelpers.getType(tree);
    return functionalInterfaceReturnsObject(type, state) && !isExemptedInterfaceType(type, state);
  }

  /**
   * Checks that the return value of a functional interface is void. Note, we do not use
   * ASTHelpers.isVoidType here, return values of Void are actually type-checked. Only
   * void-returning functions silently ignore return values of any type.
   */
  private static boolean functionalInterfaceReturnsObject(Type interfaceType, VisitorState state) {
    Type objectType = state.getSymtab().objectType;
    return ASTHelpers.isSubtype(
        objectType,
        ASTHelpers.getUpperBound(
            state.getTypes().findDescriptorType(interfaceType).getReturnType(), state.getTypes()),
        state);
  }

  private static final ImmutableSet<String> EXEMPTED_TYPES =
      ImmutableSet.of(
          "org.mockito.stubbing.Answer",
          "graphql.schema.DataFetcher",
          "org.jmock.lib.action.CustomAction",
          "net.sf.cglib.proxy.MethodInterceptor",
          "org.aopalliance.intercept.MethodInterceptor",
          InvocationHandler.class.getName());

  private static boolean isExemptedInterfaceType(Type type, VisitorState state) {
    return EXEMPTED_TYPES.stream()
        .map(state::getTypeFromString)
        .anyMatch(t -> ASTHelpers.isSubtype(type, t, state));
  }

  private static boolean isExemptedInterfaceMethod(MethodSymbol symbol, VisitorState state) {
    return isExemptedInterfaceType(ASTHelpers.enclosingClass(symbol).type, state);
  }

  /** Returning a type from a lambda or method that returns Object loses the type information. */
  @Override
  public Description matchReturn(ReturnTree tree, VisitorState state) {
    Optional<Type> optionalType = lostType(state);
    if (!optionalType.isPresent()) {
      return Description.NO_MATCH;
    }
    Type objectType = state.getSymtab().objectType;
    Type lostType = optionalType.get();
    Type resultType = ASTHelpers.getResultType(tree.getExpression());
    if (resultType == null) {
      return Description.NO_MATCH;
    }
    if (resultType.getKind() == TypeKind.NULL || resultType.getKind() == TypeKind.NONE) {
      return Description.NO_MATCH;
    }
    if (ASTHelpers.isSubtype(resultType, lostType, state)) {
      // Traverse enclosing nodes of this return tree until either a lambda or a Method is reached.
      for (Tree enclosing : state.getPath()) {
        if (enclosing instanceof MethodTree) {
          MethodTree methodTree = (MethodTree) enclosing;
          MethodSymbol symbol = ASTHelpers.getSymbol(methodTree);
          if (ASTHelpers.isSubtype(objectType, symbol.getReturnType(), state)
              && !isExemptedInterfaceMethod(symbol, state)) {
            return buildDescription(tree)
                .setMessage(
                    lostTypeMessage(resultType.toString(), symbol.getReturnType().toString()))
                .build();
          } else {
            break;
          }
        }
        if (enclosing instanceof LambdaExpressionTree) {
          LambdaExpressionTree lambdaTree = (LambdaExpressionTree) enclosing;
          if (isObjectReturningLambdaExpression(lambdaTree, state)) {
            return buildDescription(tree)
                .setMessage(lostTypeMessage(resultType.toString(), "Object"))
                .build();
          } else {
            break;
          }
        }
      }
    }
    return Description.NO_MATCH;
  }
}
