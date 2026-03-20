/*
 * Copyright 2026 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getThrownExceptions;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isCheckedExceptionType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static java.util.stream.Collectors.toCollection;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.predicates.TypePredicates;
import com.google.errorprone.util.FindIdentifiers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.lang.model.element.ElementKind;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "Minimize the amount of logic in assertThrows", severity = WARNING)
public class AssertThrowsMinimizer extends BugChecker implements MethodTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      staticMethod().onClass("org.junit.Assert").named("assertThrows");

  private final ConstantExpressions constantExpressions;
  private final boolean useVarType;

  @Inject
  AssertThrowsMinimizer(ConstantExpressions constantExpressions, ErrorProneFlags flags) {
    this.constantExpressions = constantExpressions;
    this.useVarType = flags.getBoolean("AssertThrowsMinimizer:UseVarType").orElse(false);
  }

  private ImmutableList<AssertThrows> findAssertThrowsToFix(VisitorState state) {
    ImmutableList.Builder<AssertThrows> toFix = ImmutableList.builder();
    new SuppressibleTreePathScanner<Void, Void>(state) {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        matchMethodInvocation(tree, state.withPath(getCurrentPath())).ifPresent(toFix::add);
        return super.visitMethodInvocation(tree, null);
      }
    }.scan(state.getPath(), null);
    return toFix.build();
  }

  record AssertThrows(
      Tree parent,
      LambdaExpressionTree lambdaExpressionTree,
      MethodInvocationTree runnable,
      ImmutableList<Hoist> toHoist) {}

  record Hoist(ExpressionTree site, String name) {}

  private Optional<AssertThrows> matchMethodInvocation(
      MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return Optional.empty();
    }
    if (!(state.getPath().getParentPath().getLeaf() instanceof StatementTree parent)) {
      // We need a scope to declare variables in, assertThrows is usually an expression statement or
      // a variable initializer
      return Optional.empty();
    }
    if (!(tree.getArguments().getLast() instanceof LambdaExpressionTree lambdaExpressionTree)) {
      return Optional.empty();
    }
    Type firstArgumentType = getType(tree.getArguments().get(0));
    if (firstArgumentType.getTypeArguments().isEmpty()) {
      return Optional.empty();
    }
    Type exceptionType = firstArgumentType.getTypeArguments().get(0);
    MethodInvocationTree runnable;
    switch (lambdaExpressionTree.getBody()) {
      case BlockTree blockTree -> {
        if (blockTree.getStatements().size() != 1) {
          return Optional.empty();
        }
        if (!(getOnlyElement(blockTree.getStatements())
                instanceof ExpressionStatementTree expressionStatementTree
            && expressionStatementTree.getExpression()
                instanceof MethodInvocationTree methodInvocationTree)) {
          return Optional.empty();
        }
        runnable = methodInvocationTree;
      }
      case MethodInvocationTree methodInvocationTree -> runnable = methodInvocationTree;
      default -> {
        return Optional.empty();
      }
    }
    MethodSymbol sym = getSymbol(runnable);
    ImmutableList<Hoist> toHoist =
        Streams.concat(
                Stream.ofNullable(getReceiver(runnable))
                    .map(r -> new Hoist(r, receiverVariableName(r))),
                Streams.zip(
                    runnable.getArguments().stream(),
                    Streams.concat(
                            sym.getParameters().stream(),
                            // if there are varargs, there may be more arguments than parameters
                            Stream.generate(() -> sym.getParameters().getLast()))
                        .map(p -> p.getSimpleName().toString()),
                    (ExpressionTree a, String p) -> new Hoist(a, p)))
            .filter(h -> needsHoisting(h.site(), exceptionType, state))
            .collect(toImmutableList());
    if (toHoist.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new AssertThrows(parent, lambdaExpressionTree, runnable, toHoist));
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    ImmutableList<AssertThrows> toFix = findAssertThrowsToFix(state);
    if (toFix.isEmpty()) {
      return NO_MATCH;
    }

    ImmutableList<Type> checkedExceptions =
        toFix.stream()
            .flatMap(at -> at.toHoist.stream())
            .flatMap(h -> getThrownExceptions(h.site(), state).stream())
            .filter(t -> isCheckedExceptionType(t, state))
            .collect(toImmutableList());

    SuggestedFix.Builder fix = SuggestedFix.builder();
    if (!checkedExceptions.isEmpty()) {
      Type exceptionType = state.getSymtab().exceptionType;
      boolean needsThrowable =
          checkedExceptions.stream()
              .anyMatch(t -> !state.getTypes().isAssignable(t, exceptionType));
      addThrows(
          tree,
          needsThrowable ? state.getSymtab().throwableType : state.getSymtab().exceptionType,
          fix,
          state);
    }

    // update the tree path so VariableName considers the method parameters
    VariableNamer variableNamer =
        new VariableNamer(state.withPath(new TreePath(state.getPath(), toFix.getFirst().runnable)));
    for (AssertThrows current : toFix) {
      StringBuilder hoistedVariables = new StringBuilder();
      for (Hoist hoist : current.toHoist) {
        String identifier = variableNamer.avoidShadowing(hoist.name());
        Type type =
            getType(
                hoist.site() instanceof NewClassTree newClassTree
                    ? newClassTree.getIdentifier()
                    : hoist.site());
        hoistedVariables.append(
            String.format(
                "%s %s = %s;\n",
                useVarType ? "var" : SuggestedFixes.qualifyType(state, fix, type),
                identifier,
                state.getSourceForNode(initializer(state, hoist.site(), type))));
        fix.replace(hoist.site(), identifier);
      }
      fix.prefixWith(current.parent(), hoistedVariables.toString());
      if (current.lambdaExpressionTree().getBody() instanceof BlockTree blockTree) {
        fix.replace(getStartPosition(blockTree), getStartPosition(current.runnable()), "");
        fix.replace(state.getEndPosition(current.runnable()), state.getEndPosition(blockTree), "");
      }
    }
    return describeMatch(toFix.getFirst().parent(), fix.build());
  }

  private ExpressionTree initializer(VisitorState state, ExpressionTree site, Type type) {
    if (useVarType) {
      return site;
    }
    if (site instanceof TypeCastTree typeCastTree
        && isSubtype(getType(typeCastTree.getExpression()), type, state)) {
      // avoid unnecessary casts in hoisted variables
      return typeCastTree.getExpression();
    }
    return site;
  }

  private void addThrows(
      MethodTree tree, Type exceptionType, SuggestedFix.Builder fix, VisitorState state) {
    var types = state.getTypes();
    if (tree.getThrows().stream().anyMatch(t -> types.isSuperType(getType(t), exceptionType))) {
      return;
    }
    if (tree.getThrows().isEmpty()) {
      fix.prefixWith(tree.getBody(), " throws " + exceptionType.tsym.getSimpleName());
    } else {
      fix.postfixWith(
          Iterables.getLast(tree.getThrows()), ", " + exceptionType.tsym.getSimpleName());
    }
  }

  private static String receiverVariableName(ExpressionTree tree) {
    return CaseFormat.UPPER_CAMEL.to(
        CaseFormat.LOWER_CAMEL, getType(tree).asElement().getSimpleName().toString());
  }

  private boolean needsHoisting(ExpressionTree tree, Type exceptionType, VisitorState state) {
    if (KNOWN_SAFE.matches(tree, state)) {
      var arguments =
          switch (tree) {
            case MethodInvocationTree methodInvocationTree -> methodInvocationTree.getArguments();
            case NewClassTree newClassTree -> newClassTree.getArguments();
            default -> throw new AssertionError(tree.getKind());
          };
      if (arguments.stream().noneMatch(a -> needsHoisting(a, exceptionType, state))) {
        return false;
      }
    }
    boolean unqualifiedIdentifier =
        switch (tree) {
          case IdentifierTree identifierTree -> true;
          case MemberSelectTree memberSelectTree ->
              memberSelectTree.getExpression() instanceof IdentifierTree identifierTree
                  && identifierTree.getName().contentEquals("this");
          default -> false;
        };
    if (unqualifiedIdentifier && getSymbol(tree).getKind() == ElementKind.FIELD) {
      return false;
    }
    if (constValue(tree) != null) {
      // Allow anything with a compile-time constant value. constantExpressions doesn't cover
      // constant fields and string concatenation.
      return false;
    }
    if (isCheckedException(exceptionType, state) && !throwsSubtypeOf(tree, exceptionType, state)) {
      return false;
    }
    boolean needsHoisting =
        switch (tree) {
          case LambdaExpressionTree lambdaExpressionTree -> false;
          case MemberReferenceTree memberReferenceTree ->
              needsHoisting(memberReferenceTree.getQualifierExpression(), exceptionType, state);
          case NewArrayTree newArrayTree ->
              Stream.concat(
                      newArrayTree.getDimensions().stream(),
                      Optional.ofNullable(newArrayTree.getInitializers())
                          .map(list -> list.stream())
                          .orElse(Stream.empty()))
                  .anyMatch(t -> needsHoisting(t, exceptionType, state));
          case NewClassTree newClassTree -> newClassTreeNeedsHoisting(newClassTree);
          case TypeCastTree typeCastTree ->
              needsHoisting(typeCastTree.getExpression(), exceptionType, state)
                  || !isSubtype(
                      getType(typeCastTree.getExpression()),
                      getType(typeCastTree.getType()),
                      state);
          default -> true;
        };
    if (!needsHoisting) {
      return false;
    }
    // This is an imperfect heuristic. These expressions aren't guaranteed not to throw, but may be
    // less valuable to hoist.
    return constantExpressions.constantExpression(tree, state).isEmpty();
  }

  // Allow anonymous implementations of interfaces
  private boolean newClassTreeNeedsHoisting(NewClassTree tree) {
    if (tree.getClassBody() == null) {
      return true;
    }
    Symbol sym = getSymbol(tree.getIdentifier());
    // New class expressions for non-interfaces could throw in constructors or class initializers
    if (!sym.isInterface()) {
      return true;
    }
    // Hoist implementations that could throw in constructors or class initializers (in static
    // blocks or field initializers)
    return !tree.getClassBody().getMembers().stream().allMatch(m -> m instanceof MethodTree);
  }

  private static boolean throwsSubtypeOf(
      ExpressionTree tree, Type exceptionType, VisitorState state) {
    Types types = state.getTypes();
    return types.isSubtype(state.getSymtab().runtimeExceptionType, exceptionType)
        || getThrownExceptions(tree, state).stream()
            .anyMatch(t -> isCheckedException(t, state) && types.isSubtype(t, exceptionType));
  }

  private static boolean isCheckedException(Type exception, VisitorState state) {
    Types types = state.getTypes();
    return !types.isSubtype(exception, state.getSymtab().runtimeExceptionType)
        && !types.isSubtype(exception, state.getSymtab().errorType);
  }

  private static final Matcher<ExpressionTree> KNOWN_SAFE =
      anyOf(
          staticMethod()
              .onClass("com.google.net.rpc3.client.RpcClientContext")
              .named("create")
              .withNoParameters(),
          staticMethod()
              .onClass("com.google.net.rpc.RpcAuthority")
              .named("self")
              .withNoParameters(),
          staticMethod().onClass("java.util.Arrays").named("asList"),
          // TODO: b/493258197 - immutable collection factories could throw NPEs
          staticMethod()
              .onClassAny(
                  "com.google.common.collect.ImmutableList",
                  "com.google.common.collect.ImmutableSet")
              .named("of"),
          staticMethod().onClass("java.util.Collections").named("emptyList"),
          constructor()
              .forClass(
                  TypePredicates.isDescendantOf(
                      "com.google.android.gms.tagmanager.internal.type.AbstractType")));

  private static class VariableNamer {
    private final Set<String> idents;

    VariableNamer(VisitorState state) {
      this.idents =
          FindIdentifiers.findAllIdents(state).stream()
              .map(s -> s.getSimpleName().toString())
              .collect(toCollection(HashSet::new));
    }

    // Stolen from PatternMatchingInstanceof
    // TODO: cushon - add to SuggestedFixes?
    private String avoidShadowing(String name) {
      return IntStream.iterate(1, i -> i + 1)
          .mapToObj(i -> i == 1 ? name : (name + i))
          .filter(n -> idents.add(n))
          .findFirst()
          .get();
    }
  }
}
