/*
 * Copyright 2016 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Streams.concat;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.VisitorState.memoize;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.fixByAddingNullableAnnotationToReturnType;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.hasDefinitelyNullBranch;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.isVoid;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.varsProvenNullByParentIf;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyMethod;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.expressionStatement;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.findEnclosingMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static com.sun.source.tree.Tree.Kind.NULL_LITERAL;
import static java.lang.Boolean.FALSE;
import static java.util.regex.Pattern.compile;
import static javax.lang.model.type.TypeKind.TYPEVAR;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.stream.Stream;
import javax.lang.model.element.Name;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "ReturnMissingNullable",
    summary = "Method returns a definitely null value but is not annotated @Nullable",
    severity = SUGGESTION)
public class ReturnMissingNullable extends BugChecker implements CompilationUnitTreeMatcher {
  private static final Matcher<StatementTree> METHODS_THAT_NEVER_RETURN =
      expressionStatement(
          anyOf(
              anyMethod().anyClass().withNameMatching(compile("throw.*Exception")),
              staticMethod()
                  .onClassAny(
                      "org.junit.Assert",
                      "junit.framework.Assert",
                      /*
                       * I'm not sure if TestCase is necessary, as it doesn't define its own fail()
                       * method, but it commonly appears in lists like this one, so I've included
                       * it. (Maybe the method was defined on TestCase many versions ago?)
                       *
                       * TODO(cpovirk): Confirm need, or remove from everywhere.
                       */
                      "junit.framework.TestCase")
                  .named("fail"),
              staticMethod().onClass("java.lang.System").named("exit")));

  private static final Matcher<StatementTree> FAILS_IF_PASSED_FALSE =
      expressionStatement(
          staticMethod()
              .onClassAny("com.google.common.base.Preconditions", "com.google.common.base.Verify")
              .namedAnyOf("checkArgument", "checkState", "verify"));

  /**
   * A supplier that returns a set of methods <i>that users can <b>override</b></i> and whose
   * implementations must in practice always return a nullable type.
   *
   * <p>We use this so that we can say things like "An implementation of {@code Map.get} should be
   * annotated with {@code @Nullable}," <i>not</i> to say that things like "A method with {@code
   * return map.get(foo)} should be annotated with {@code @Nullable}."
   */
  // TODO(cpovirk): For performance, generate a Multimap indexed by Name?
  private static final Supplier<ImmutableSet<MethodSymbol>> METHODS_KNOWN_TO_RETURN_NULL =
      memoize(
          state ->
              concat(
                      /*
                       * We assume that no one is implementing a Map<MyEnum, V> that contains every
                       * enum constant and throws an exception for non-MyEnum values. Also, we
                       * assume that no one minds annotating methods like ImmutableMap.put() with
                       * @Nullable even though they always throw an exception instead of returning.
                       */
                      streamElements(state, "java.util.Map")
                          .filter(
                              m ->
                                  hasName(m, "get")
                                      || hasName(m, "put")
                                      || hasName(m, "putIfAbsent")
                                      || (hasName(m, "remove") && hasParams(m, 1))
                                      || (hasName(m, "replace") && hasParams(m, 2))),
                      streamElements(state, "com.google.common.collect.BiMap")
                          .filter(m -> hasName(m, "forcePut")),
                      streamElements(state, "com.google.common.collect.Table")
                          .filter(
                              m -> hasName(m, "get") || hasName(m, "put") || hasName(m, "remove")),
                      streamElements(state, "com.google.common.cache.Cache")
                          .filter(m -> hasName(m, "getIfPresent")),
                      /*
                       * We assume that no one is implementing a never-empty Queue, etc. -- or at
                       * least that no one minds annotating its return types with @Nullable anyway.
                       */
                      streamElements(state, "java.util.Queue")
                          .filter(m -> hasName(m, "poll") || hasName(m, "peek")),
                      streamElements(state, "java.util.Deque")
                          .filter(
                              m ->
                                  hasName(m, "pollFirst")
                                      || hasName(m, "peekFirst")
                                      || hasName(m, "pollLast")
                                      || hasName(m, "peekLast")),
                      streamElements(state, "java.util.NavigableSet")
                          .filter(
                              m ->
                                  hasName(m, "lower")
                                      || hasName(m, "floor")
                                      || hasName(m, "ceiling")
                                      || hasName(m, "higher")
                                      || hasName(m, "pollFirst")
                                      || hasName(m, "pollLast")),
                      streamElements(state, "java.util.NavigableMap")
                          .filter(
                              m ->
                                  hasName(m, "lowerEntry")
                                      || hasName(m, "floorEntry")
                                      || hasName(m, "ceilingEntry")
                                      || hasName(m, "higherEntry")
                                      || hasName(m, "lowerKey")
                                      || hasName(m, "floorKey")
                                      || hasName(m, "ceilingKey")
                                      || hasName(m, "higherKey")
                                      || hasName(m, "firstEntry")
                                      || hasName(m, "lastEntry")
                                      || hasName(m, "pollFirstEntry")
                                      || hasName(m, "pollLastEntry")),
                      // We assume no one is implementing an infinite Spliterator.
                      streamElements(state, "java.util.Spliterator")
                          .filter(m -> hasName(m, "trySplit")))
                  /*
                   * TODO(cpovirk): Consider AbstractIterator.computeNext().
                   *
                   */
                  .map(MethodSymbol.class::cast)
                  .collect(toImmutableSet()));

  private final boolean beingConservative;

  public ReturnMissingNullable(ErrorProneFlags flags) {
    this.beingConservative = flags.getBoolean("Nullness:Conservative").orElse(true);
  }

  @Override
  public Description matchCompilationUnit(
      CompilationUnitTree tree, VisitorState stateForCompilationUnit) {
    if (beingConservative && stateForCompilationUnit.errorProneOptions().isTestOnlyTarget()) {
      // Annotating test code for nullness can be useful, but it's not our primary focus.
      return NO_MATCH;
    }

    /*
     * In each scanner below, we define helper methods so that we can return early without the
     * verbosity of `return super.visitFoo(...)`.
     */

    ImmutableSet.Builder<VarSymbol> definitelyNullVarsBuilder = ImmutableSet.builder();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitVariable(VariableTree tree, Void unused) {
        doVisitVariable(tree);
        return super.visitVariable(tree, unused);
      }

      void doVisitVariable(VariableTree tree) {
        VarSymbol symbol = getSymbol(tree);
        if (!isConsideredFinal(symbol)) {
          return;
        }

        ExpressionTree initializer = tree.getInitializer();
        if (initializer == null) {
          return;
        }

        if (initializer.getKind() != NULL_LITERAL) {
          return;
        }

        definitelyNullVarsBuilder.add(symbol);
      }
    }.scan(tree, null);
    ImmutableSet<VarSymbol> definitelyNullVars = definitelyNullVarsBuilder.build();

    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitBlock(BlockTree block, Void unused) {
        for (StatementTree statement : block.getStatements()) {
          if (METHODS_THAT_NEVER_RETURN.matches(statement, stateForCompilationUnit)) {
            break;
          }
          if (FAILS_IF_PASSED_FALSE.matches(statement, stateForCompilationUnit)
              && constValue(
                      ((MethodInvocationTree) ((ExpressionStatementTree) statement).getExpression())
                          .getArguments()
                          .get(0))
                  == FALSE) {
            break;
          }
          scan(statement, null);
        }
        return null;
      }

      @Override
      public Void visitMethod(MethodTree tree, Void unused) {
        doVisitMethod(tree);
        return super.visitMethod(tree, unused);
      }

      void doVisitMethod(MethodTree tree) {
        MethodSymbol possibleOverride = getSymbol(tree);

        /*
         * TODO(cpovirk): Provide an option to shortcut if !hasAnnotation(possibleOverride,
         * Override.class). NullAway makes this assumption for performance reasons, so we could
         * follow suit, at least for compile-time checking. (Maybe this will confuse someone
         * someday, but hopefully Error Prone users are saved by the MissingOverride check.) We
         * should still retain the *option* to check for overriding even in the absence of an
         * annotation (in order to serve the case of running this in refactoring/patch mode over a
         * codebase).
         */

        if (NullnessAnnotations.fromAnnotationsOn(possibleOverride).orElse(null)
            == Nullness.NULLABLE) {
          return;
        }

        for (MethodSymbol methodKnownToReturnNull :
            METHODS_KNOWN_TO_RETURN_NULL.get(stateForCompilationUnit)) {
          if (stateForCompilationUnit
              .getElements()
              .overrides(possibleOverride, methodKnownToReturnNull, possibleOverride.enclClass())) {
            SuggestedFix fix =
                fixByAddingNullableAnnotationToReturnType(
                    stateForCompilationUnit.withPath(getCurrentPath()), tree);
            if (!fix.isEmpty()) {
              stateForCompilationUnit.reportMatch(describeMatch(tree, fix));
            }
          }
        }
      }

      @Override
      public Void visitReturn(ReturnTree tree, Void unused) {
        doVisitReturn(tree);
        return super.visitReturn(tree, unused);
      }

      void doVisitReturn(ReturnTree returnTree) {
        /*
         * We need the VisitorState to have the correct TreePath for (a) the call to
         * findEnclosingMethod and (b) the call to NullnessFixes (which looks up identifiers).
         */
        VisitorState state = stateForCompilationUnit.withPath(getCurrentPath());

        ExpressionTree returnExpression = returnTree.getExpression();
        if (returnExpression == null) {
          return;
        }

        MethodTree methodTree = findEnclosingMethod(state);
        if (methodTree == null) {
          return;
        }

        List<? extends StatementTree> statements = methodTree.getBody().getStatements();
        if (beingConservative
            && statements.size() == 1
            && getOnlyElement(statements) == returnTree
            && returnExpression.getKind() == NULL_LITERAL) {
          /*
           * When the entire method body is `return null`, I worry that this may be a stub
           * implementation that all "real" implementations are meant to override. Ideally such
           * stubs would use implementation like `throw new UnsupportedOperationException()`, but
           * let's assume the worst.
           */
          return;
        }

        MethodSymbol method = getSymbol(methodTree);
        Type returnType = method.getReturnType();
        if (beingConservative && isVoid(returnType, state)) {
          // `@Nullable Void` is accurate but noisy, so some users won't want it.
          return;
        }
        if (returnType.isPrimitive()) {
          // Buggy code, but adding @Nullable just makes it worse.
          return;
        }
        if (beingConservative && returnType.getKind() == TYPEVAR) {
          /*
           * Consider AbstractFuture.getDoneValue: It returns a literal `null`, but it shouldn't be
           * annotated @Nullable because it returns null *only* if the AbstractFuture's type
           * argument permits that.
           */
          return;
        }

        if (NullnessAnnotations.fromAnnotationsOn(method).orElse(null) == Nullness.NULLABLE) {
          return;
        }

        ImmutableSet<Name> varsProvenNullByParentIf = varsProvenNullByParentIf(getCurrentPath());
        /*
         * TODO(cpovirk): Consider reporting only one finding per method? Our patching
         * infrastructure is smart enough not to mind duplicate suggested fixes, but users might be
         * annoyed by multiple robocomments with the same fix.
         */
        if (hasDefinitelyNullBranch(
            returnExpression,
            definitelyNullVars,
            varsProvenNullByParentIf,
            stateForCompilationUnit)) {
          SuggestedFix fix =
              fixByAddingNullableAnnotationToReturnType(
                  state.withPath(getCurrentPath()), methodTree);
          if (!fix.isEmpty()) {
            state.reportMatch(describeMatch(returnTree, fix));
          }
        }
      }
    }.scan(tree, null);

    return NO_MATCH; // Any reports were made through state.reportMatch.
  }

  private static boolean hasName(Symbol symbol, String name) {
    return symbol.name.contentEquals(name);
  }

  private static boolean hasParams(Symbol method, int paramCount) {
    return ((MethodSymbol) method).getParameters().size() == paramCount;
  }

  private static Stream<Symbol> streamElements(VisitorState state, String clazz) {
    Symbol symbol = state.getSymbolFromString(clazz);
    return symbol == null ? Stream.empty() : symbol.getEnclosedElements().stream();
  }
}
