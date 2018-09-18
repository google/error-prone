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

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.Multimaps.toMultimap;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.parentNode;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ForkJoinTask;
import javax.lang.model.type.TypeKind;

/** See BugPattern annotation. */
@BugPattern(
    name = "FutureReturnValueIgnored",
    summary =
        "Return value of methods returning Future must be checked. Ignoring returned Futures "
            + "suppresses exceptions thrown from the code that completes the Future.",
    category = JDK,
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE)
public final class FutureReturnValueIgnored extends AbstractReturnValueIgnored
    implements ReturnTreeMatcher {

  private static final Matcher<ExpressionTree> BLACKLIST =
      anyOf(
          // ForkJoinTask#fork has side-effects and returns 'this', so it's reasonable to ignore
          // the return value.
          instanceMethod()
              .onDescendantOf(ForkJoinTask.class.getName())
              .named("fork")
              .withParameters(),
          // CompletionService is intended to be used in a way where the Future returned
          // from submit is discarded, because the Futures are available later via e.g. take()
          instanceMethod().onDescendantOf(CompletionService.class.getName()).named("submit"),
          // IntelliJ's executeOnPooledThread wraps the Callable/Runnable in one that catches
          // Throwable, so it can't fail (unless logging the Throwable also throws, but there's
          // nothing much to be done at that point).
          instanceMethod()
              .onDescendantOf("com.intellij.openapi.application.Application")
              .named("executeOnPooledThread"),
          // ChannelFuture#addListener(s) returns itself for chaining. Any exception during the
          // future execution should be dealt by the listener(s).
          instanceMethod()
              .onDescendantOf("io.netty.channel.ChannelFuture")
              .namedAnyOf("addListener", "addListeners"),
          instanceMethod()
              .onExactClass("java.util.concurrent.CompletableFuture")
              .namedAnyOf("completeAsync", "orTimeout", "completeOnTimeout"));

  private static final Matcher<ExpressionTree> MATCHER =
      new Matcher<ExpressionTree>() {
        @Override
        public boolean matches(ExpressionTree tree, VisitorState state) {
          Type futureType = state.getTypeFromString("java.util.concurrent.Future");
          if (futureType == null) {
            return false;
          }
          Symbol untypedSymbol = ASTHelpers.getSymbol(tree);
          if (!(untypedSymbol instanceof MethodSymbol)) {
            Type resultType = ASTHelpers.getResultType(tree);
            return resultType != null
                && ASTHelpers.isSubtype(
                    ASTHelpers.getUpperBound(resultType, state.getTypes()), futureType, state);
          }
          MethodSymbol sym = (MethodSymbol) untypedSymbol;
          if (hasAnnotation(sym, CanIgnoreReturnValue.class, state)) {
            return false;
          }
          for (MethodSymbol superSym : ASTHelpers.findSuperMethods(sym, state.getTypes())) {
            // There are interfaces annotated with @CanIgnoreReturnValue (like Guava's Function)
            // whose return value really shouldn't be ignored - as a heuristic, check if the super's
            // method is returning a future subtype.
            if (hasAnnotation(superSym, CanIgnoreReturnValue.class, state)
                && ASTHelpers.isSubtype(
                    ASTHelpers.getUpperBound(superSym.getReturnType(), state.getTypes()),
                    futureType,
                    state)) {
              return false;
            }
          }
          if (BLACKLIST.matches(tree, state)) {
            return false;
          }
          Type returnType = sym.getReturnType();
          return ASTHelpers.isSubtype(
              ASTHelpers.getUpperBound(returnType, state.getTypes()), futureType, state);
        }
      };

  @Override
  public Matcher<ExpressionTree> specializedMatcher() {
    return MATCHER;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    Description description = super.matchMethodInvocation(tree, state);
    if (Description.NO_MATCH == description) {
      return checkLostType(tree, state);
    } else {
      return description;
    }
  }

  private Description checkLostType(MethodInvocationTree tree, VisitorState state) {
    Type futureType = state.getTypeFromString("java.util.concurrent.Future");
    if (futureType == null) {
      return Description.NO_MATCH;
    }

    MethodSymbol sym = ASTHelpers.getSymbol(tree);
    Type returnType = ASTHelpers.getResultType(tree);
    Type returnedFutureType = state.getTypes().asSuper(returnType, futureType.tsym);
    if (returnedFutureType != null
        && !returnedFutureType.hasTag(TypeTag.ERROR) // work around error-prone#996
        && !returnedFutureType.isRaw()) {
      if (ASTHelpers.isSubtype(
          ASTHelpers.getUpperBound(returnedFutureType.getTypeArguments().get(0), state.getTypes()),
          futureType,
          state)) {
        return buildDescription(tree)
            .setMessage(String.format("Method returns a nested type, %s", returnType))
            .build();
      }

      // The type variable that determines the generic on the returned future was not a Future.
      // However, many methods (like guava's Futures.transform) have signatures like this:
      // Future<O> do(SomeObject<? extends O>). If O resolves to java.lang.Object or ?, then a
      // SomeObject<Future> is a valid parameter to pass, but results in a nested future.
      Type methodReturnType = sym.getReturnType();
      List<TypeVariableSymbol> typeParameters = sym.getTypeParameters();
      Set<TypeVariableSymbol> returnTypeChoosing = new HashSet<>();
      // For each type variable on the method, see if we can reach the type declared as the param
      // of the returned Future, by traversing its type bounds. If we can reach it, we know that if
      // an argument is passed to an invocation of this method where the type variable is a subtype
      // of Future, that means that a nested Future is being returned.
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
      // Future's type, check each passed parameter to ensure that it is never passed as a subtype
      // of Future.
      if (!returnTypeChoosing.isEmpty()) {
        Multimap<TypeVariableSymbol, TypeInfo> resolved = getResolvedGenerics(tree);
        for (TypeVariableSymbol returnTypeChoosingSymbol : returnTypeChoosing) {
          Collection<TypeInfo> types = resolved.get(returnTypeChoosingSymbol);
          for (TypeInfo type : types) {
            if (ASTHelpers.isSubtype(type.resolvedVariableType, futureType, state)) {
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
                parentNode(FutureReturnValueIgnored::isObjectReturningLambdaExpression),
                not(AbstractReturnValueIgnored::expectedExceptionTest)),
            specializedMatcher(),
            not((t, s) -> ASTHelpers.isVoidType(ASTHelpers.getType(t), s)))
        .matches(tree, state)) {
      return describe(tree, state);
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
    return functionalInterfaceReturnsObject(((JCMemberReference) tree).type, state);
  }

  private static boolean isObjectReturningLambdaExpression(Tree tree, VisitorState state) {
    if (!(tree instanceof LambdaExpressionTree)) {
      return false;
    }

    Type type = ((JCLambda) tree).type;
    return functionalInterfaceReturnsObject(type, state)
        && !isWhitelistedInterfaceType(type, state);
  }

  /**
   * Checks that the return value of a functional interface is void. Note, we do not use
   * ASTHelpers.isVoidType here, return values of Void are actually type-checked. Only
   * void-returning functions silently ignore return values of any type.
   */
  private static boolean functionalInterfaceReturnsObject(Type interfaceType, VisitorState state) {
    Type objectType = state.getTypeFromString("java.lang.Object");
    return ASTHelpers.isSubtype(
        objectType,
        ASTHelpers.getUpperBound(
            state.getTypes().findDescriptorType(interfaceType).getReturnType(), state.getTypes()),
        state);
  }

  /**
   * Detect member references that implement an interface that return Object, but resolve to a
   * method that returns Future.
   */
  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    Description description = super.matchMemberReference(tree, state);
    if (Description.NO_MATCH == description) {
      if (allOf(
              (t, s) -> t.getMode() == ReferenceMode.INVOKE,
              FutureReturnValueIgnored::isObjectReturningMethodReferenceExpression,
              not((t, s) -> isWhitelistedInterfaceType(((JCMemberReference) t).type, s)),
              not((t, s) -> isThrowingFunctionalInterface(s, ((JCMemberReference) t).type)),
              specializedMatcher())
          .matches(tree, state)) {
        return describeMatch(tree);
      }
    }
    return description;
  }

  private static final ImmutableSet<String> WHITELISTED_TYPES =
      ImmutableSet.of(
          "org.mockito.stubbing.Answer",
          "graphql.schema.DataFetcher",
          "org.jmock.lib.action.CustomAction",
          "net.sf.cglib.proxy.MethodInterceptor",
          "org.aopalliance.intercept.MethodInterceptor",
          InvocationHandler.class.getName());

  private static boolean isWhitelistedInterfaceType(Type type, VisitorState state) {
    return WHITELISTED_TYPES.stream()
        .map(state::getTypeFromString)
        .anyMatch(whitelistedType -> ASTHelpers.isSubtype(type, whitelistedType, state));
  }

  private static boolean isWhitelistedInterfaceMethod(MethodSymbol symbol, VisitorState state) {
    return isWhitelistedInterfaceType(ASTHelpers.enclosingClass(symbol).type, state);
  }

  /**
   * Returning a type of Future from a lambda or method that returns Object loses the Future type,
   * which can result in suppressed errors or race conditions.
   */
  @Override
  public Description matchReturn(ReturnTree tree, VisitorState state) {
    Type objectType = state.getTypeFromString("java.lang.Object");
    Type futureType = state.getTypeFromString("java.util.concurrent.Future");
    if (futureType == null) {
      return Description.NO_MATCH;
    }
    Type resultType = ASTHelpers.getResultType(tree.getExpression());
    if (resultType == null) {
      return Description.NO_MATCH;
    }
    if (resultType.getKind() == TypeKind.NULL || resultType.getKind() == TypeKind.NONE) {
      return Description.NO_MATCH;
    }
    if (ASTHelpers.isSubtype(resultType, futureType, state)) {
      // Traverse enclosing nodes of this return tree until either a lambda or a Method is reached.
      for (Tree enclosing : state.getPath()) {
        if (enclosing instanceof MethodTree) {
          MethodTree methodTree = (MethodTree) enclosing;
          MethodSymbol symbol = ASTHelpers.getSymbol(methodTree);
          if (ASTHelpers.isSubtype(objectType, symbol.getReturnType(), state)
              && !isWhitelistedInterfaceMethod(symbol, state)) {
            return buildDescription(tree)
                .setMessage(
                    String.format(
                        "Returning %s from method that returns %s. Errors from the returned future"
                            + " may be ignored.",
                        resultType, symbol.getReturnType()))
                .build();
          } else {
            break;
          }
        }
        if (enclosing instanceof LambdaExpressionTree) {
          LambdaExpressionTree lambdaTree = (LambdaExpressionTree) enclosing;
          if (isObjectReturningLambdaExpression(lambdaTree, state)) {
            return buildDescription(tree)
                .setMessage(
                    String.format(
                        "Returning %s from method that returns Object. Errors from the returned"
                            + " future will be ignored.",
                        resultType))
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
