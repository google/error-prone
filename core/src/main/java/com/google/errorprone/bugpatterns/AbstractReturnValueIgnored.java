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
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.parentNode;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.UnusedReturnValueMatcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import java.lang.reflect.InvocationHandler;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeKind;

/**
 * An abstract base class to match API usages in which the return value is not used.
 *
 * <p>In addition to regular contexts in which a return value isn't used (e.g.: the result of {@code
 * String.trim()} is just ignored), this class has the capacity to determine if the result is cast
 * in such a way as to lose important static type information.
 *
 * <p>If an analysis extending this base class chooses to care about this circumstance, they can
 * override {@link #lostType} to define the type information they wish to keep.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public abstract class AbstractReturnValueIgnored extends BugChecker
    implements MethodInvocationTreeMatcher,
        MemberReferenceTreeMatcher,
        ReturnTreeMatcher,
        NewClassTreeMatcher {

  private final Supplier<UnusedReturnValueMatcher> unusedReturnValueMatcher =
      Suppliers.memoize(() -> UnusedReturnValueMatcher.get(allowInExceptionThrowers()));

  private final Supplier<Matcher<ExpressionTree>> matcher =
      Suppliers.memoize(() -> allOf(unusedReturnValueMatcher.get(), this::isCheckReturnValue));

  private final Supplier<Matcher<MemberReferenceTree>> lostReferenceTreeMatcher =
      Suppliers.memoize(
          () ->
              allOf(
                  AbstractReturnValueIgnored::isObjectReturningMethodReferenceExpression,
                  not((t, s) -> isExemptedInterfaceType(ASTHelpers.getType(t), s)),
                  not((t, s) -> Matchers.isThrowingFunctionalInterface(ASTHelpers.getType(t), s)),
                  specializedMatcher()));

  private final ConstantExpressions constantExpressions;

  protected AbstractReturnValueIgnored() {
    this(ErrorProneFlags.empty());
  }

  protected AbstractReturnValueIgnored(ErrorProneFlags flags) {
    this.constantExpressions = ConstantExpressions.fromFlags(flags);
  }

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    Description description =
        matcher.get().matches(methodInvocationTree, state)
            ? describeReturnValueIgnored(methodInvocationTree, state)
            : NO_MATCH;
    if (!description.equals(NO_MATCH)) {
      return description;
    }
    return checkLostType(methodInvocationTree, state);
  }

  @Override
  public Description matchNewClass(NewClassTree newClassTree, VisitorState state) {
    return matcher.get().matches(newClassTree, state)
        ? describeReturnValueIgnored(newClassTree, state)
        : NO_MATCH;
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    Description description =
        matcher.get().matches(tree, state) ? describeReturnValueIgnored(tree, state) : NO_MATCH;
    if (!lostType(state).isPresent() || !description.equals(NO_MATCH)) {
      return description;
    }
    if (lostReferenceTreeMatcher.get().matches(tree, state)) {
      return describeMatch(tree);
    }
    return description;
  }

  /**
   * Returns whether this checker makes any determination about whether the given tree's return
   * value should be used or not. Most checkers either determine that an expression is CRV or make
   * no determination.
   */
  public boolean isCovered(ExpressionTree tree, VisitorState state) {
    return isCheckReturnValue(tree, state);
  }

  /**
   * Returns whether the given tree's return value should be used according to this checker,
   * regardless of whether or not the return value is actually used.
   */
  public final boolean isCheckReturnValue(ExpressionTree tree, VisitorState state) {
    // TODO(cgdecker): Just replace specializedMatcher with this?
    return specializedMatcher().matches(tree, state);
  }

  /** Returns a map of optional metadata about why this check matched the given tree. */
  public ImmutableMap<String, ?> getMatchMetadata(ExpressionTree tree, VisitorState state) {
    return ImmutableMap.of();
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
    return buildDescription(methodInvocationTree)
        .addFix(makeFix(methodInvocationTree, state))
        .setMessage(getMessage(getSymbol(methodInvocationTree).getSimpleName()))
        .build();
  }

  final Fix makeFix(MethodInvocationTree methodInvocationTree, VisitorState state) {
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

    Fix fix = SuggestedFix.emptyFix();
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
      if (parent instanceof ExpressionStatementTree
          && constantExpressions.constantExpression(methodInvocationTree, state).isPresent()) {
        fix = SuggestedFix.delete(parent);
      }
    }
    return fix;
  }

  /**
   * Uses the default description for results ignored via a method reference. Subclasses may
   * override if they prefer a different description.
   */
  protected Description describeReturnValueIgnored(
      MemberReferenceTree memberReferenceTree, VisitorState state) {
    return buildDescription(memberReferenceTree)
        .setMessage(
            getMessage(
                state.getName(descriptiveNameForMemberReference(memberReferenceTree, state))))
        .build();
  }

  /**
   * Uses the default description for results ignored via a constructor call. Subclasses may
   * override if they prefer a different description.
   */
  protected Description describeReturnValueIgnored(NewClassTree newClassTree, VisitorState state) {
    return buildDescription(newClassTree)
        .setMessage(
            String.format(
                "Ignored return value of '%s'",
                state.getSourceForNode(newClassTree.getIdentifier())))
        .build();
  }

  private static String descriptiveNameForMemberReference(
      MemberReferenceTree memberReferenceTree, VisitorState state) {
    if (memberReferenceTree.getMode() == ReferenceMode.NEW) {
      // The qualifier expression *should* just be the name of the class here
      return state.getSourceForNode(memberReferenceTree.getQualifierExpression());
    }
    return memberReferenceTree.getName().toString();
  }

  /**
   * Returns the diagnostic message. Can be overridden by subclasses to provide a customized
   * diagnostic that includes the name of the invoked method.
   */
  protected String getMessage(Name name) {
    return message();
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
          TypeVariableSymbol currentTypeParam = queue.remove();
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
        ListMultimap<TypeVariableSymbol, TypeInfo> resolved = getResolvedGenerics(tree);
        for (TypeVariableSymbol returnTypeChoosingSymbol : returnTypeChoosing) {
          List<TypeInfo> types = resolved.get(returnTypeChoosingSymbol);
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
                not(unusedReturnValueMatcher.get()::isAllowed)),
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

  private static ListMultimap<TypeVariableSymbol, TypeInfo> getResolvedGenerics(
      MethodInvocationTree tree) {
    Type type = ASTHelpers.getType(tree.getMethodSelect());
    ImmutableListMultimap<TypeVariableSymbol, Type> subst =
        ASTHelpers.getTypeSubstitution(type, getSymbol(tree));
    return subst.entries().stream()
        .map(e -> new TypeInfo(e.getKey(), e.getValue(), tree))
        .collect(
            toMultimap(
                k -> k.sym, k -> k, MultimapBuilder.linkedHashKeys().arrayListValues()::build));
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
