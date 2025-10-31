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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Suppliers.memoize;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Lists.reverse;
import static com.google.common.collect.Multimaps.toMultimap;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ResultUsePolicy.EXPECTED;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ResultUsePolicy.UNSPECIFIED;
import static com.google.errorprone.fixes.SuggestedFix.delete;
import static com.google.errorprone.fixes.SuggestedFix.postfixWith;
import static com.google.errorprone.fixes.SuggestedFix.prefixWith;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyStaticImport;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.isThrowingFunctionalInterface;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.parentNode;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.getResultType;
import static com.google.errorprone.util.ASTHelpers.getRootAssignable;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.getTypeSubstitution;
import static com.google.errorprone.util.ASTHelpers.getUpperBound;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ASTHelpers.isVoidType;
import static com.google.errorprone.util.ASTHelpers.matchingMethods;
import static com.google.errorprone.util.FindIdentifiers.findAllIdents;
import static com.sun.tools.javac.parser.Tokens.TokenKind.RPAREN;
import static java.lang.String.format;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.concat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.bugpatterns.checkreturnvalue.ResultUsePolicy;
import com.google.errorprone.bugpatterns.checkreturnvalue.ResultUsePolicyAnalyzer;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.UnusedReturnValueMatcher;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
import com.sun.source.tree.MemberSelectTree;
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
import java.lang.reflect.InvocationHandler;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
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
        NewClassTreeMatcher,
        ResultUsePolicyAnalyzer<ExpressionTree, VisitorState> {

  private final Supplier<UnusedReturnValueMatcher> unusedReturnValueMatcher =
      memoize(() -> UnusedReturnValueMatcher.get(allowInExceptionThrowers()));

  private final Supplier<Matcher<ExpressionTree>> matcher =
      memoize(() -> allOf(unusedReturnValueMatcher.get(), this::isCheckReturnValue));

  private final Supplier<Matcher<MemberReferenceTree>> lostReferenceTreeMatcher =
      memoize(
          () ->
              allOf(
                  (t, s) -> isObjectReturningMethodReferenceExpression(t, s),
                  not((t, s) -> isExemptedInterfaceType(getType(t), s)),
                  not((t, s) -> isThrowingFunctionalInterface(getType(t), s)),
                  specializedMatcher()));

  private final ConstantExpressions constantExpressions;

  protected AbstractReturnValueIgnored(ConstantExpressions constantExpressions) {
    this.constantExpressions = constantExpressions;
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

  @Override
  public boolean isCovered(ExpressionTree tree, VisitorState state) {
    return isCheckReturnValue(tree, state);
  }

  @Override
  public ResultUsePolicy getMethodPolicy(ExpressionTree expression, VisitorState state) {
    return isCheckReturnValue(expression, state) ? EXPECTED : UNSPECIFIED;
  }

  /**
   * Returns whether the given expression's return value should be used according to this checker,
   * regardless of whether or not the return value is actually used.
   */
  private boolean isCheckReturnValue(ExpressionTree tree, VisitorState state) {
    // TODO(cgdecker): Just replace specializedMatcher with this?
    return specializedMatcher().matches(tree, state);
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
    return format("Returning %s from method that returns %s.", returnedType, declaredReturnType);
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
        .addAllFixes(fixesAtCallSite(methodInvocationTree, state))
        .setMessage(getMessage(getSymbol(methodInvocationTree).getSimpleName()))
        .build();
  }

  final ImmutableList<Fix> fixesAtCallSite(ExpressionTree invocationTree, VisitorState state) {
    checkArgument(
        invocationTree instanceof MethodInvocationTree || invocationTree instanceof NewClassTree,
        "unexpected kind: %s",
        invocationTree.getKind());

    Tree parent = state.getPath().getParentPath().getLeaf();

    Type resultType = getType(invocationTree);
    // Find the root of the field access chain, i.e. a.intern().trim() ==> a.
    /*
     * TODO(cpovirk): Enhance getRootAssignable to return array accesses (e.g., `x[y]`)? If we do,
     * then we'll also need to accept `symbol == null` (which is fine, since all we need the symbol
     * for is to check against `this`, and `x[y]` is not `this`.)
     */
    ExpressionTree identifierExpr =
        invocationTree instanceof MethodInvocationTree methodInvocationTree
            ? getRootAssignable(methodInvocationTree)
            : null; // null root assignable for constructor calls (as well as some method calls)
    Symbol symbol = getSymbol(identifierExpr);
    Type identifierType = getType(identifierExpr);

    /*
     * A map from short description to fix instance (even though every short description ultimately
     * will become _part of_ a fix instance later).
     *
     * As always, the order of suggested fixes can matter. In practice, it probably matters mostly
     * just to the checker's own tests. But it also affects the order in which the fixes are printed
     * during compile errors, and it affects which fix is chosen for automatically generated fix CLs
     * (though those should be rare inside Google: b/244334502#comment13).
     *
     * Note that, when possible, we have separate code that suggests adding @CanIgnoreReturnValue in
     * preference to all the fixes below.
     *
     * The _names_ of the fixes probably don't actually matter inside Google: b/204435834#comment4.
     * Luckily, they're not a ton harder to include than plain code comments would be.
     */
    ImmutableMap.Builder<String, SuggestedFix> fixes = ImmutableMap.builder();
    if (MOCKITO_VERIFY.matches(invocationTree, state)) {
      ExpressionTree maybeCallToMock =
          ((MethodInvocationTree) invocationTree).getArguments().get(0);
      if (maybeCallToMock instanceof MethodInvocationTree methodInvocationTree) {
        ExpressionTree maybeMethodSelectOnMock = methodInvocationTree.getMethodSelect();
        if (maybeMethodSelectOnMock instanceof MemberSelectTree maybeSelectOnMock) {
          // For this suggestion, we want to move the closing parenthesis:
          // verify(foo .bar())
          //           ^      v
          //           +------+
          //
          // The result is:
          // verify(foo).bar()
          //
          // TODO(cpovirk): Suggest this only if `foo` looks like an actual mock object.
          SuggestedFix.Builder fix = SuggestedFix.builder();
          fix.postfixWith(maybeSelectOnMock.getExpression(), ")");
          int closingParen =
              reverse(state.getOffsetTokensForNode(invocationTree)).stream()
                  .filter(t -> t.kind() == RPAREN)
                  .findFirst()
                  .get()
                  .pos();
          fix.replace(closingParen, closingParen + 1, "");
          fixes.put(
              format("Verify that %s was called", maybeSelectOnMock.getIdentifier()), fix.build());
        }
      }
    }
    boolean considerBlanketFixes = true;
    if (resultType != null && resultType.getKind() == TypeKind.BOOLEAN) {
      // Fix by calling either assertThat(...).isTrue() or verify(...).
      if (state.errorProneOptions().isTestOnlyTarget()) {
        SuggestedFix.Builder fix = SuggestedFix.builder();
        fix.prefixWith(
                invocationTree,
                qualifyStaticImport("com.google.common.truth.Truth.assertThat", fix, state) + "(")
            .postfixWith(invocationTree, ").isTrue()");
        fixes.put("Assert that the result is true", fix.build());
      } else {
        SuggestedFix.Builder fix = SuggestedFix.builder();
        fix.prefixWith(
                invocationTree,
                qualifyStaticImport("com.google.common.base.Verify.verify", fix, state) + "(")
            .postfixWith(invocationTree, ")");
        fixes.put("Insert a runtime check that the result is true", fix.build());
      }
    } else if (resultType != null
        // By looking for any isTrue() method, we handle not just Truth but also AssertJ.
        && matchingMethods(
                NAME_OF_IS_TRUE.get(state),
                m -> m.getParameters().isEmpty(),
                resultType,
                state.getTypes())
            .anyMatch(m -> true)) {
      fixes.put("Assert that the result is true", postfixWith(invocationTree, ".isTrue()"));
      considerBlanketFixes = false;
    }
    if (identifierExpr != null
        && symbol != null
        && !symbol.name.contentEquals("this")
        && resultType != null
        && state.getTypes().isAssignable(resultType, identifierType)) {
      fixes.put(
          "Assign result back to variable",
          prefixWith(invocationTree, state.getSourceForNode(identifierExpr) + " = "));
    }
    /*
     * TODO(cpovirk): Suggest returning the value from the enclosing method where possible... *if*
     * we can find a good heuristic. We could consider "Is the return type a protobuf" and/or "Is
     * this a constructor call or build() call?"
     */
    if (parent instanceof ExpressionStatementTree
        && !constantExpressions.constantExpression(invocationTree, state).isPresent()
        && considerBlanketFixes) {
      ImmutableSet<String> identifiersInScope =
          findAllIdents(state).stream().map(v -> v.name.toString()).collect(toImmutableSet());
      concat(Stream.of("unused"), range(2, 10).mapToObj(i -> "unused" + i))
          // TODO(b/72928608): Handle even local variables declared *later* within this scope.
          // TODO(b/250568455): Also check whether we have suggested this name before in this scope.
          .filter(n -> !identifiersInScope.contains(n))
          .findFirst()
          .ifPresent(
              n ->
                  fixes.put(
                      "Suppress error by assigning to a variable",
                      prefixWith(parent, format("var %s = ", n))));
    }
    if (parent instanceof ExpressionStatementTree && considerBlanketFixes) {
      if (constantExpressions.constantExpression(invocationTree, state).isPresent()) {
        fixes.put("Delete call", delete(parent));
      } else {
        fixes.put("Delete call and any side effects", delete(parent));
      }
    }
    return fixes.buildOrThrow().entrySet().stream()
        .map(e -> e.getValue().toBuilder().setShortDescription(e.getKey()).build())
        .collect(toImmutableList());
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
            format(
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
      return NO_MATCH;
    }

    Type lostType = optionalType.get();

    MethodSymbol sym = getSymbol(tree);
    Type returnType = getResultType(tree);
    Type returnedFutureType = state.getTypes().asSuper(returnType, lostType.tsym);
    if (returnedFutureType != null
        && !returnedFutureType.hasTag(TypeTag.ERROR) // work around error-prone#996
        && !returnedFutureType.isRaw()) {
      if (isSubtype(
          getUpperBound(returnedFutureType.getTypeArguments().get(0), state.getTypes()),
          lostType,
          state)) {
        return buildDescription(tree)
            .setMessage(format("Method returns a nested type, %s", returnType))
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
            if (toAdd.tsym instanceof TypeVariableSymbol typeVariableSymbol) {
              queue.add(typeVariableSymbol);
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
            if (isSubtype(type.resolvedVariableType, lostType, state)) {
              return buildDescription(type.tree)
                  .setMessage(
                      format(
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
            not((t, s) -> isVoidType(getType(t), s)))
        .matches(tree, state)) {
      return describeReturnValueIgnored(tree, state);
    }

    return NO_MATCH;
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
    Type type = getType(tree.getMethodSelect());
    ImmutableListMultimap<TypeVariableSymbol, Type> subst =
        getTypeSubstitution(type, getSymbol(tree));
    return subst.entries().stream()
        .map(e -> new TypeInfo(e.getKey(), e.getValue(), tree))
        .collect(
            toMultimap(
                k -> k.sym, k -> k, MultimapBuilder.linkedHashKeys().arrayListValues()::build));
  }

  private static boolean isObjectReturningMethodReferenceExpression(
      MemberReferenceTree tree, VisitorState state) {
    return functionalInterfaceReturnsObject(getType(tree), state);
  }

  private static boolean isObjectReturningLambdaExpression(Tree tree, VisitorState state) {
    if (!(tree instanceof LambdaExpressionTree)) {
      return false;
    }

    Type type = getType(tree);
    return functionalInterfaceReturnsObject(type, state) && !isExemptedInterfaceType(type, state);
  }

  /**
   * Checks that the return value of a functional interface is void. Note, we do not use
   * ASTHelpers.isVoidType here, return values of Void are actually type-checked. Only
   * void-returning functions silently ignore return values of any type.
   */
  private static boolean functionalInterfaceReturnsObject(Type interfaceType, VisitorState state) {
    Type objectType = state.getSymtab().objectType;
    return isSubtype(
        objectType,
        getUpperBound(
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
        .anyMatch(t -> isSubtype(type, t, state));
  }

  private static boolean isExemptedInterfaceMethod(MethodSymbol symbol, VisitorState state) {
    return isExemptedInterfaceType(enclosingClass(symbol).type, state);
  }

  /** Returning a type from a lambda or method that returns Object loses the type information. */
  @Override
  public Description matchReturn(ReturnTree tree, VisitorState state) {
    Optional<Type> optionalType = lostType(state);
    if (!optionalType.isPresent()) {
      return NO_MATCH;
    }
    Type objectType = state.getSymtab().objectType;
    Type lostType = optionalType.get();
    Type resultType = getResultType(tree.getExpression());
    if (resultType == null) {
      return NO_MATCH;
    }
    if (resultType.getKind() == TypeKind.NULL || resultType.getKind() == TypeKind.NONE) {
      return NO_MATCH;
    }
    if (isSubtype(resultType, lostType, state)) {
      // Traverse enclosing nodes of this return tree until either a lambda or a Method is reached.
      for (Tree enclosing : state.getPath()) {
        if (enclosing instanceof MethodTree methodTree) {
          MethodSymbol symbol = getSymbol(methodTree);
          if (isSubtype(objectType, symbol.getReturnType(), state)
              && !isExemptedInterfaceMethod(symbol, state)) {
            return buildDescription(tree)
                .setMessage(
                    lostTypeMessage(resultType.toString(), symbol.getReturnType().toString()))
                .build();
          } else {
            break;
          }
        }
        if (enclosing instanceof LambdaExpressionTree lambdaTree) {
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
    return NO_MATCH;
  }

  private static final Matcher<ExpressionTree> MOCKITO_VERIFY =
      staticMethod().onClass("org.mockito.Mockito").named("verify");

  private static final com.google.errorprone.suppliers.Supplier<com.sun.tools.javac.util.Name>
      NAME_OF_IS_TRUE = VisitorState.memoize(state -> state.getName("isTrue"));
}
