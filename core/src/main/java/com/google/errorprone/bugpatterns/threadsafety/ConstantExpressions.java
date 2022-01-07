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

package com.google.errorprone.bugpatterns.threadsafety;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.staticEqualsInvocation;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.ConstantBooleanExpression.ConstantBooleanExpressionKind;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.ConstantExpression.ConstantExpressionKind;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Helper for establishing whether expressions correspond to a constant expression. */
public final class ConstantExpressions {
  private static final Matcher<ExpressionTree> PURE_METHODS =
      anyOf(
          staticMethod().onClass("com.google.common.base.Optional"),
          staticMethod().onClass("com.google.common.base.Pair"),
          staticMethod().onClass("com.google.common.base.Splitter"),
          staticMethod().onClass("com.google.common.collect.ImmutableBiMap"),
          staticMethod().onClass("com.google.common.collect.ImmutableCollection"),
          staticMethod().onClass("com.google.common.collect.ImmutableList"),
          staticMethod().onClass("com.google.common.collect.ImmutableListMultimap"),
          staticMethod().onClass("com.google.common.collect.ImmutableMap"),
          staticMethod().onClass("com.google.common.collect.ImmutableMultimap"),
          staticMethod().onClass("com.google.common.collect.ImmutableMultiset"),
          staticMethod().onClass("com.google.common.collect.ImmutableRangeMap"),
          staticMethod().onClass("com.google.common.collect.ImmutableRangeSet"),
          staticMethod().onClass("com.google.common.collect.ImmutableSet"),
          staticMethod().onClass("com.google.common.collect.ImmutableSetMultimap"),
          staticMethod().onClass("com.google.common.collect.ImmutableSortedMap"),
          staticMethod().onClass("com.google.common.collect.ImmutableSortedMultiset"),
          staticMethod().onClass("com.google.common.collect.ImmutableSortedSet"),
          staticMethod().onClass("com.google.common.collect.ImmutableTable"),
          staticMethod().onClass("com.google.common.collect.Range"),
          staticMethod().onClass("com.google.protobuf.GeneratedMessage"),
          staticMethod()
              .onClass("java.time.Duration")
              .namedAnyOf("ofNanos", "ofMillis", "ofSeconds", "ofMinutes", "ofHours", "ofDays")
              .withParameters("long"),
          staticMethod()
              .onClass("java.time.Instant")
              .namedAnyOf("ofEpochMilli", "ofEpochSecond")
              .withParameters("long"),
          staticMethod()
              .onClass("com.google.protobuf.util.Timestamps")
              .namedAnyOf("fromNanos", "fromMicros", "fromMillis", "fromSeconds"),
          staticMethod()
              .onClass("com.google.protobuf.util.Durations")
              .namedAnyOf(
                  "fromNanos",
                  "fromMicros",
                  "fromMillis",
                  "fromSeconds",
                  "fromMinutes",
                  "fromHours",
                  "fromDays"),
          staticMethod()
              .onClass("org.joda.time.Duration")
              .namedAnyOf(
                  "millis", "standardSeconds", "standardMinutes", "standardHours", "standardDays")
              .withParameters("long"),
          constructor().forClass("org.joda.time.Instant").withParameters("long"),
          constructor().forClass("org.joda.time.DateTime").withParameters("long"),
          staticMethod()
              .onClass("java.time.LocalDate")
              .withNameMatching(Pattern.compile("^?!(now)")),
          staticMethod()
              .onClass("java.time.LocalDateTime")
              .withNameMatching(Pattern.compile("^?!(now)")),
          staticMethod()
              .onClass("java.time.LocalTime")
              .withNameMatching(Pattern.compile("^?!(now)")),
          staticMethod().onClass("java.time.MonthDay"),
          staticMethod()
              .onClass("java.time.OffsetDateTime")
              .withNameMatching(Pattern.compile("^?!(now)")),
          staticMethod()
              .onClass("java.time.OffsetTime")
              .withNameMatching(Pattern.compile("^?!(now)")),
          staticMethod().onClass("java.time.Period"),
          staticMethod().onClass("java.time.Year"),
          staticMethod().onClass("java.time.YearMonth"),
          staticMethod().onClass("java.time.ZoneId"),
          staticMethod().onClass("java.time.ZoneOffset"),
          staticMethod()
              .onClass("java.time.ZonedDateTime")
              .withNameMatching(Pattern.compile("^?!(now)")),
          staticMethod().onClass("java.util.Optional"),
          staticMethod().onClass("java.util.OptionalDouble"),
          staticMethod().onClass("java.util.OptionalInt"),
          staticMethod().onClass("java.util.OptionalLong"),
          staticMethod().onClass("java.util.regex.Pattern"),
          staticMethod().onClass("org.joda.time.DateTime"),
          staticMethod().onClass("org.joda.time.DateTimeZone"),
          staticMethod().onClass("org.joda.time.Days"),
          staticMethod().onClass("org.joda.time.Duration"),
          staticMethod().onClass("org.joda.time.Instant"),
          staticMethod().onClass("org.joda.time.Interval"),
          staticMethod().onClass("org.joda.time.LocalDate"),
          staticMethod().onClass("org.joda.time.LocalDateTime"),
          staticMethod().onClass("org.joda.time.Period"),
          staticMethod().onClass("org.joda.time.format.DateTimeFormatter"),
          Matchers.hasAnnotation("org.checkerframework.dataflow.qual.Pure"),
          (tree, state) -> {
            Symbol symbol = getSymbol(tree);
            return hasAnnotation(symbol.owner, "com.google.auto.value.AutoValue", state)
                && symbol.getModifiers().contains(ABSTRACT);
          },
          instanceMethod().onDescendantOf("com.google.protobuf.MessageLite"),
          instanceMethod()
              .onDescendantOf("com.google.protobuf.MessageLite.Builder")
              .withNameMatching(Pattern.compile("get|has.*")),
          staticMethod()
              .onDescendantOf("com.google.protobuf.MessageLite")
              .named("getDefaultInstance"),
          instanceEqualsInvocation(),
          staticEqualsInvocation());

  public static ConstantExpressions fromFlags(ErrorProneFlags flags) {
    // No dependence on flags yet, but this is instantiable to make future flagging easier.
    return new ConstantExpressions();
  }

  /** Represents sets of things known to be true and false if a boolean statement evaluated true. */
  @AutoValue
  public abstract static class Truthiness {
    public abstract ImmutableSet<ConstantBooleanExpression> requiredTrue();

    public abstract ImmutableSet<ConstantBooleanExpression> requiredFalse();

    private static Truthiness create(
        Iterable<ConstantBooleanExpression> requiredTrue,
        Iterable<ConstantBooleanExpression> requiredFalse) {
      return new AutoValue_ConstantExpressions_Truthiness(
          ImmutableSet.copyOf(requiredTrue), ImmutableSet.copyOf(requiredFalse));
    }
  }

  /**
   * Scans an {@link ExpressionTree} to find anything guaranteed to be false or true if this
   * expression is true.
   */
  public Truthiness truthiness(ExpressionTree tree, boolean not, VisitorState state) {
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

  /** Represents a constant boolean expression. */
  @AutoOneOf(ConstantBooleanExpressionKind.class)
  public abstract static class ConstantBooleanExpression {
    enum ConstantBooleanExpressionKind {
      BOOLEAN_LITERAL,
      CONSTANT_EQUALS,
      CONSTANT_EXPRESSION,
    }

    abstract ConstantBooleanExpressionKind kind();

    abstract VarSymbol booleanLiteral();

    private static ConstantBooleanExpression booleanLiteral(VarSymbol varSymbol) {
      return AutoOneOf_ConstantExpressions_ConstantBooleanExpression.booleanLiteral(varSymbol);
    }

    abstract ConstantEquals constantEquals();

    private static ConstantBooleanExpression constantEquals(ConstantEquals constantEquals) {
      return AutoOneOf_ConstantExpressions_ConstantBooleanExpression.constantEquals(constantEquals);
    }

    abstract ConstantExpression constantExpression();

    private static ConstantBooleanExpression constantExpression(
        ConstantExpression constantExpression) {
      return AutoOneOf_ConstantExpressions_ConstantBooleanExpression.constantExpression(
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

  /** Represents a constant expression. */
  @AutoOneOf(ConstantExpressionKind.class)
  public abstract static class ConstantExpression {
    enum ConstantExpressionKind {
      LITERAL,
      CONSTANT_ACCESSOR
    }

    abstract ConstantExpressionKind kind();

    abstract Object literal();

    private static ConstantExpression literal(Object object) {
      return AutoOneOf_ConstantExpressions_ConstantExpression.literal(object);
    }

    abstract ImmutableList<PureMethodInvocation> constantAccessor();

    private static ConstantExpression constantAccessor(
        ImmutableList<PureMethodInvocation> constantAccessor) {
      return AutoOneOf_ConstantExpressions_ConstantExpression.constantAccessor(constantAccessor);
    }

    @Override
    public final String toString() {
      switch (kind()) {
        case LITERAL:
          return literal().toString();
        case CONSTANT_ACCESSOR:
          return constantAccessor().reverse().stream().map(Object::toString).collect(joining("."));
      }
      throw new AssertionError();
    }

    public void accept(ConstantExpressionVisitor visitor) {
      switch (kind()) {
        case LITERAL:
          visitor.visitConstant(literal());
          break;
        case CONSTANT_ACCESSOR:
          constantAccessor().forEach(pmi -> pmi.accept(visitor));
          break;
      }
    }
  }

  public Optional<ConstantExpression> constantExpression(ExpressionTree tree, VisitorState state) {
    checkNotNull(tree);
    Object value = constValue(tree);
    if (value != null && tree instanceof LiteralTree) {
      return Optional.of(ConstantExpression.literal(value));
    }
    return symbolizeImmutableExpression(tree, state).map(ConstantExpression::constantAccessor);
  }

  /** Represents a binary equals call on two constant expressions. */
  @AutoValue
  public abstract static class ConstantEquals {
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
      return new AutoValue_ConstantExpressions_ConstantEquals(lhs, rhs);
    }
  }

  /** Represents a method invocation of a pure method on constant arguments. */
  @AutoValue
  public abstract static class PureMethodInvocation {
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
      return new AutoValue_ConstantExpressions_PureMethodInvocation(
          symbol, ImmutableList.copyOf(arguments));
    }

    public void accept(ConstantExpressionVisitor visitor) {
      visitor.visitIdentifier(symbol());
      arguments().forEach(a -> a.accept(visitor));
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
      } else if (receiver instanceof MethodInvocationTree
          && PURE_METHODS.matches(receiver, state)) {
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

  /** Visitor for scanning over the components of a constant expression. */
  public interface ConstantExpressionVisitor {
    default void visitConstant(Object constant) {}

    default void visitIdentifier(Symbol identifier) {}
  }
}
