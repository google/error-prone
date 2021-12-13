/*
 * Copyright 2021 The Error Prone Authors.
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
import static com.google.common.collect.Multisets.removeOccurrences;
import static com.google.common.collect.Sets.intersection;
import static com.google.common.collect.Sets.union;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.staticEqualsInvocation;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOf;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static javax.lang.model.element.Modifier.ABSTRACT;

import com.google.auto.value.AutoOneOf;
import com.google.auto.value.AutoValue;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.AlreadyChecked.ConstantBooleanExpression.ConstantBooleanExpressionKind;
import com.google.errorprone.bugpatterns.AlreadyChecked.ConstantExpression.ConstantExpressionKind;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.WellKnownMutability;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Bugpattern to find conditions which are checked more than once, and either vacuously true or
 * false.
 */
@BugPattern(
    name = "AlreadyChecked",
    severity = WARNING,
    summary = "This condition has already been checked.")
public final class AlreadyChecked extends BugChecker implements CompilationUnitTreeMatcher {

  private final Matcher<ExpressionTree> pureMethods;

  public AlreadyChecked(ErrorProneFlags flags) {
    WellKnownMutability wellKnownMutability = WellKnownMutability.fromFlags(flags);
    // Bit of a heuristic: instance and static factories on known-immutable classes tend to be pure.
    pureMethods =
        anyOf(
            anyOf(
                wellKnownMutability.getKnownImmutableClasses().keySet().stream()
                    .map(
                        className ->
                            anyOf(
                                staticMethod().onClass(isDescendantOf(className)),
                                instanceMethod().onDescendantOf(className)))
                    .collect(toImmutableList())),
            Matchers.hasAnnotation("org.checkerframework.dataflow.qual.Pure"),
            staticEqualsInvocation(),
            instanceEqualsInvocation(),
            (tree, state) -> {
              Symbol symbol = getSymbol(tree);
              return hasAnnotation(symbol.owner, "com.google.auto.value.AutoValue", state)
                  && symbol.getModifiers().contains(ABSTRACT);
            });
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    new IfScanner(state).scan(state.getPath(), null);

    return NO_MATCH;
  }

  /** Scans a compilation unit, keeping track of which things are known to be true and false. */
  private final class IfScanner extends SuppressibleTreePathScanner<Void, Void> {
    private final Multiset<ConstantBooleanExpression> truths = HashMultiset.create();
    private final Multiset<ConstantBooleanExpression> falsehoods = HashMultiset.create();
    private final VisitorState state;

    private IfScanner(VisitorState state) {
      this.state = state;
    }

    @Override
    public Void visitIf(IfTree tree, Void unused) {
      Truthiness truthiness = truthiness(tree.getCondition(), /* not= */ false, state);

      checkCondition(tree.getCondition(), truthiness);

      withinScope(truthiness, tree.getThenStatement());

      withinScope(truthiness(tree.getCondition(), /* not= */ true, state), tree.getElseStatement());
      return null;
    }

    private void withinScope(Truthiness truthiness, Tree tree) {
      truths.addAll(truthiness.requiredTrue());
      falsehoods.addAll(truthiness.requiredFalse());
      scan(tree, null);
      removeOccurrences(truths, truthiness.requiredTrue());
      removeOccurrences(falsehoods, truthiness.requiredFalse());
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree tree, Void unused) {
      checkCondition(tree.getCondition(), truthiness(tree.getCondition(), false, state));
      return super.visitConditionalExpression(tree, null);
    }

    void checkCondition(Tree tree, Truthiness truthiness) {
      Set<ConstantBooleanExpression> vacuousFalsehoods =
          union(
              intersection(truthiness.requiredTrue(), falsehoods.elementSet()),
              intersection(truthiness.requiredFalse(), truths.elementSet()));
      if (!vacuousFalsehoods.isEmpty()) {
        state.reportMatch(
            buildDescription(tree)
                .setMessage(format("This condition (on %s) is vacuously false.", vacuousFalsehoods))
                .build());
      }
      Set<ConstantBooleanExpression> vacuousTruths =
          union(
              intersection(truthiness.requiredTrue(), truths.elementSet()),
              intersection(truthiness.requiredFalse(), falsehoods.elementSet()));
      if (!vacuousTruths.isEmpty()) {
        state.reportMatch(
            buildDescription(tree)
                .setMessage(
                    format(
                        "This condition (on %s) is vacuously true; it's already been checked by"
                            + " this point.",
                        vacuousTruths))
                .build());
      }
    }
  }

  @AutoValue
  abstract static class Truthiness {
    abstract ImmutableSet<ConstantBooleanExpression> requiredTrue();

    abstract ImmutableSet<ConstantBooleanExpression> requiredFalse();

    private static Truthiness create(
        Iterable<ConstantBooleanExpression> requiredTrue,
        Iterable<ConstantBooleanExpression> requiredFalse) {
      return new AutoValue_AlreadyChecked_Truthiness(
          ImmutableSet.copyOf(requiredTrue), ImmutableSet.copyOf(requiredFalse));
    }
  }
  /**
   * Scans an {@link ExpressionTree} to find anything guaranteed to be false or true if this
   * expression is true.
   */
  private Truthiness truthiness(ExpressionTree tree, boolean not, VisitorState state) {
    ImmutableSet.Builder<ConstantBooleanExpression> requiredTrue = ImmutableSet.builder();
    ImmutableSet.Builder<ConstantBooleanExpression> requiredFalse = ImmutableSet.builder();

    // Keep track of whether we saw an expression too complex for us to handle, and failed.
    AtomicBoolean failed = new AtomicBoolean();

    new SimpleTreeVisitor<Void, Void>() {
      boolean negated = not;

      @Override
      public Void visitParenthesized(ParenthesizedTree tree, Void unused) {
        return visit(tree.getExpression(), null);
      }

      @Override
      public Void visitUnary(UnaryTree tree, Void unused) {
        if (tree.getKind().equals(Kind.LOGICAL_COMPLEMENT)) {
          withNegation(() -> visit(tree.getExpression(), null));
        }
        return null;
      }

      @Override
      public Void visitBinary(BinaryTree tree, Void unused) {
        if (tree.getKind().equals(Kind.EQUAL_TO) || tree.getKind().equals(Kind.NOT_EQUAL_TO)) {
          Optional<ConstantExpression> lhs = constantExpression(tree.getLeftOperand(), state);
          Optional<ConstantExpression> rhs = constantExpression(tree.getRightOperand(), state);
          if (lhs.isPresent() && rhs.isPresent()) {
            ConstantBooleanExpression expression =
                ConstantBooleanExpression.constantEquals(ConstantEquals.of(lhs.get(), rhs.get()));
            if (tree.getKind().equals(Kind.NOT_EQUAL_TO)) {
              withNegation(() -> add(expression));
            } else {
              add(expression);
            }
          }
        } else if (negated
            ? tree.getKind().equals(Kind.CONDITIONAL_OR)
            : tree.getKind().equals(Kind.CONDITIONAL_AND)) {
          visit(tree.getLeftOperand(), null);
          visit(tree.getRightOperand(), null);
        } else {
          failed.set(true);
        }
        return null;
      }

      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        constantBooleanExpression(tree, state).ifPresent(this::add);
        return null;
      }

      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        constantBooleanExpression(tree, state).ifPresent(this::add);
        return null;
      }

      private void withNegation(Runnable runnable) {
        negated = !negated;
        runnable.run();
        negated = !negated;
      }

      private void add(ConstantBooleanExpression e) {
        if (negated) {
          requiredFalse.add(e);
        } else {
          requiredTrue.add(e);
        }
      }
    }.visit(tree, null);

    if (failed.get()) {
      return Truthiness.create(ImmutableSet.of(), ImmutableSet.of());
    }

    return Truthiness.create(requiredTrue.build(), requiredFalse.build());
  }

  @AutoOneOf(ConstantBooleanExpressionKind.class)
  abstract static class ConstantBooleanExpression {
    enum ConstantBooleanExpressionKind {
      BOOLEAN_LITERAL,
      CONSTANT_EQUALS,
      CONSTANT_EXPRESSION,
    }

    abstract ConstantBooleanExpressionKind kind();

    abstract VarSymbol booleanLiteral();

    private static ConstantBooleanExpression booleanLiteral(VarSymbol varSymbol) {
      return AutoOneOf_AlreadyChecked_ConstantBooleanExpression.booleanLiteral(varSymbol);
    }

    abstract ConstantEquals constantEquals();

    private static ConstantBooleanExpression constantEquals(ConstantEquals constantEquals) {
      return AutoOneOf_AlreadyChecked_ConstantBooleanExpression.constantEquals(constantEquals);
    }

    abstract ConstantExpression constantExpression();

    private static ConstantBooleanExpression constantExpression(
        ConstantExpression constantExpression) {
      return AutoOneOf_AlreadyChecked_ConstantBooleanExpression.constantExpression(
          constantExpression);
    }

    @Override
    public String toString() {
      switch (kind()) {
        case BOOLEAN_LITERAL:
          return booleanLiteral().toString();
        case CONSTANT_EQUALS:
          return constantEquals().toString();
        case CONSTANT_EXPRESSION:
          return constantExpression().toString();
      }
      throw new AssertionError();
    }
  }

  private Optional<ConstantBooleanExpression> constantBooleanExpression(
      ExpressionTree tree, VisitorState state) {
    Symbol symbol = getSymbol(tree);
    if (symbol instanceof VarSymbol && isConsideredFinal(symbol)) {
      return Optional.of(ConstantBooleanExpression.booleanLiteral((VarSymbol) symbol));
    }
    Optional<ConstantExpression> constantExpression = constantExpression(tree, state);
    if (constantExpression.isPresent()) {
      return constantExpression.map(ConstantBooleanExpression::constantExpression);
    }
    return Optional.empty();
  }

  @AutoOneOf(ConstantExpressionKind.class)
  abstract static class ConstantExpression {
    enum ConstantExpressionKind {
      CONSTANT,
      CONSTANT_ACCESSOR
    }

    abstract ConstantExpressionKind kind();

    abstract Object constant();

    private static ConstantExpression constant(Object object) {
      return AutoOneOf_AlreadyChecked_ConstantExpression.constant(object);
    }

    abstract ImmutableList<PureMethodInvocation> constantAccessor();

    private static ConstantExpression constantAccessor(
        ImmutableList<PureMethodInvocation> constantAccessor) {
      return AutoOneOf_AlreadyChecked_ConstantExpression.constantAccessor(constantAccessor);
    }

    @Override
    public final String toString() {
      switch (kind()) {
        case CONSTANT:
          return constant().toString();
        case CONSTANT_ACCESSOR:
          return constantAccessor().reverse().stream().map(Object::toString).collect(joining("."));
      }
      throw new AssertionError();
    }
  }

  private Optional<ConstantExpression> constantExpression(ExpressionTree tree, VisitorState state) {
    Object value = constValue(tree);
    if (value != null) {
      return Optional.of(ConstantExpression.constant(value));
    }
    return symbolizeImmutableExpression(tree, state).map(ConstantExpression::constantAccessor);
  }

  @AutoValue
  abstract static class ConstantEquals {
    abstract ConstantExpression lhs();

    abstract ConstantExpression rhs();

    @Override
    public final boolean equals(@Nullable Object other) {
      if (!(other instanceof ConstantEquals)) {
        return false;
      }
      ConstantEquals that = (ConstantEquals) other;
      return (lhs().equals(that.lhs()) && rhs().equals(that.rhs()))
          || (lhs().equals(that.rhs()) && rhs().equals(that.lhs()));
    }

    @Override
    public final String toString() {
      return format("%s equals %s", lhs(), rhs());
    }

    @Override
    public final int hashCode() {
      return lhs().hashCode() + rhs().hashCode();
    }

    static ConstantEquals of(ConstantExpression lhs, ConstantExpression rhs) {
      return new AutoValue_AlreadyChecked_ConstantEquals(lhs, rhs);
    }
  }

  @AutoValue
  abstract static class PureMethodInvocation {
    abstract Symbol symbol();

    abstract ImmutableList<ConstantExpression> arguments();

    @Override
    public final String toString() {
      if (symbol() instanceof VarSymbol || symbol() instanceof ClassSymbol) {
        return symbol().getSimpleName().toString();
      }
      return symbol().getSimpleName()
          + arguments().stream().map(Object::toString).collect(joining(", ", "(", ")"));
    }

    private static PureMethodInvocation of(Symbol symbol, Iterable<ConstantExpression> arguments) {
      return new AutoValue_AlreadyChecked_PureMethodInvocation(
          symbol, ImmutableList.copyOf(arguments));
    }
  }

  /**
   * Returns a list of the methods called to get to this expression, as well as a terminating
   * variable if needed.
   *
   * <p>For example {@code a.getFoo().getBar()} would return {@code MethodSymbol[getBar],
   * MethodSymbol[getFoo], VarSymbol[a]}.
   */
  public Optional<ImmutableList<PureMethodInvocation>> symbolizeImmutableExpression(
      ExpressionTree tree, VisitorState state) {
    ImmutableList.Builder<PureMethodInvocation> symbolized = ImmutableList.builder();
    ExpressionTree receiver = tree;
    while (receiver != null) {
      if (isPureIdentifier(receiver)) {
        symbolized.add(PureMethodInvocation.of(getSymbol(receiver), ImmutableList.of()));
      } else if (receiver instanceof MethodInvocationTree && pureMethods.matches(receiver, state)) {
        ImmutableList.Builder<ConstantExpression> arguments = ImmutableList.builder();
        for (ExpressionTree argument : ((MethodInvocationTree) receiver).getArguments()) {
          Optional<ConstantExpression> argumentConstant = constantExpression(argument, state);
          if (!argumentConstant.isPresent()) {
            return Optional.empty();
          }
          arguments.add(argumentConstant.get());
        }
        symbolized.add(PureMethodInvocation.of(getSymbol(receiver), arguments.build()));
      } else {
        return Optional.empty();
      }
      if (receiver instanceof MethodInvocationTree || receiver instanceof MemberSelectTree) {
        receiver = getReceiver(receiver);
      } else {
        break;
      }
    }
    return Optional.of(symbolized.build());
  }

  private static boolean isPureIdentifier(ExpressionTree receiver) {
    if (!(receiver instanceof IdentifierTree || receiver instanceof MemberSelectTree)) {
      return false;
    }
    Symbol symbol = getSymbol(receiver);
    return symbol.owner.isEnum()
        || (symbol instanceof VarSymbol && isConsideredFinal(symbol))
        || symbol instanceof ClassSymbol;
  }
}
