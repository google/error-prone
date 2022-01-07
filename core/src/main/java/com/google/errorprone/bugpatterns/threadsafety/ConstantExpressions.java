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
          staticMethod()
              .onClassAny(
                  "com.google.common.base.Optional",
                  "com.google.common.base.Pair",
                  "com.google.common.base.Splitter",
                  "com.google.common.collect.ImmutableBiMap",
                  "com.google.common.collect.ImmutableCollection",
                  "com.google.common.collect.ImmutableList",
                  "com.google.common.collect.ImmutableListMultimap",
                  "com.google.common.collect.ImmutableMap",
                  "com.google.common.collect.ImmutableMultimap",
                  "com.google.common.collect.ImmutableMultiset",
                  "com.google.common.collect.ImmutableRangeMap",
                  "com.google.common.collect.ImmutableRangeSet",
                  "com.google.common.collect.ImmutableSet",
                  "com.google.common.collect.ImmutableSetMultimap",
                  "com.google.common.collect.ImmutableSortedMap",
                  "com.google.common.collect.ImmutableSortedMultiset",
                  "com.google.common.collect.ImmutableSortedSet",
                  "com.google.common.collect.ImmutableTable",
                  "com.google.common.collect.Range"),
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
          staticMethod()
              .onClassAny(
                  "java.time.Period",
                  "java.time.Year",
                  "java.time.YearMonth",
                  "java.time.ZoneId",
                  "java.time.ZoneOffset"),
          instanceMethod().onDescendantOf("java.lang.String"),
          staticMethod()
              .onClass("java.time.ZonedDateTime")
              .withNameMatching(Pattern.compile("^?!(now)")),
          staticMethod()
              .onClassAny(
                  "java.util.Optional",
                  "java.util.OptionalDouble",
                  "java.util.OptionalInt",
                  "java.util.OptionalLong"),
          staticMethod().onClass("java.util.regex.Pattern"),
          staticMethod()
              .onClassAny(
                  "org.joda.time.DateTime",
                  "org.joda.time.DateTimeZone",
                  "org.joda.time.Days",
                  "org.joda.time.Duration",
                  "org.joda.time.Instant",
                  "org.joda.time.Interval",
                  "org.joda.time.LocalDate",
                  "org.joda.time.LocalDateTime",
                  "org.joda.time.Period",
                  "org.joda.time.format.DateTimeFormatter"),
          Matchers.hasAnnotation("org.checkerframework.dataflow.qual.Pure"),
          (tree, state) -> {
            Symbol symbol = getSymbol(tree);
            return hasAnnotation(symbol.owner, "com.google.auto.value.AutoValue", state)
                && symbol.getModifiers().contains(ABSTRACT);
          },
          staticMethod()
              .onDescendantOf("com.google.protobuf.MessageLite")
              .named("getDefaultInstance"),
          instanceEqualsInvocation(),
          staticEqualsInvocation());

  private final Matcher<ExpressionTree> pureMethods;

  public ConstantExpressions(Matcher<ExpressionTree> pureMethods) {
    this.pureMethods = pureMethods;
  }

  public static ConstantExpressions fromFlags(ErrorProneFlags flags) {
    WellKnownMutability wellKnownMutability = WellKnownMutability.fromFlags(flags);
    return new ConstantExpressions(
        anyOf(
            PURE_METHODS,
            instanceMethod()
                .onDescendantOfAny(wellKnownMutability.getKnownImmutableClasses().keySet())));
  }

  /** Represents sets of things known to be true and false if a boolean statement evaluated true. */
  @AutoValue
  public abstract static class Truthiness {
    public abstract ImmutableSet<ConstantExpression> requiredTrue();

    public abstract ImmutableSet<ConstantExpression> requiredFalse();

    private static Truthiness create(
        Iterable<ConstantExpression> requiredTrue, Iterable<ConstantExpression> requiredFalse) {
      return new AutoValue_ConstantExpressions_Truthiness(
          ImmutableSet.copyOf(requiredTrue), ImmutableSet.copyOf(requiredFalse));
    }
  }

  /**
   * Scans an {@link ExpressionTree} to find anything guaranteed to be false or true if this
   * expression is true.
   */
  public Truthiness truthiness(ExpressionTree tree, boolean not, VisitorState state) {
    ImmutableSet.Builder<ConstantExpression> requiredTrue = ImmutableSet.builder();
    ImmutableSet.Builder<ConstantExpression> requiredFalse = ImmutableSet.builder();

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
          constantExpression(tree, state)
              .ifPresent(
                  e -> {
                    if (tree.getKind().equals(Kind.NOT_EQUAL_TO)) {
                      withNegation(() -> add(e));
                    } else {
                      add(e);
                    }
                  });
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
        constantExpression(tree, state).ifPresent(this::add);
        return null;
      }

      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        constantExpression(tree, state).ifPresent(this::add);
        return null;
      }

      private void withNegation(Runnable runnable) {
        negated = !negated;
        runnable.run();
        negated = !negated;
      }

      private void add(ConstantExpression e) {
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

  /** Represents a constant expression. */
  @AutoOneOf(ConstantExpressionKind.class)
  public abstract static class ConstantExpression {
    enum ConstantExpressionKind {
      LITERAL,
      CONSTANT_EQUALS,
      CONSTANT_ACCESSOR
    }

    abstract ConstantExpressionKind kind();

    abstract Object literal();

    private static ConstantExpression literal(Object object) {
      return AutoOneOf_ConstantExpressions_ConstantExpression.literal(object);
    }

    abstract ConstantEquals constantEquals();

    private static ConstantExpression constantEquals(ConstantEquals constantEquals) {
      return AutoOneOf_ConstantExpressions_ConstantExpression.constantEquals(constantEquals);
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
        case CONSTANT_EQUALS:
          return constantEquals().toString();
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
        case CONSTANT_EQUALS:
          constantEquals().lhs().accept(visitor);
          constantEquals().rhs().accept(visitor);
          break;
        case CONSTANT_ACCESSOR:
          constantAccessor().forEach(pmi -> pmi.accept(visitor));
          break;
      }
    }
  }

  public Optional<ConstantExpression> constantExpression(ExpressionTree tree, VisitorState state) {
    if (tree.getKind().equals(Kind.EQUAL_TO) || tree.getKind().equals(Kind.NOT_EQUAL_TO)) {
      BinaryTree binaryTree = (BinaryTree) tree;

      Optional<ConstantExpression> lhs = constantExpression(binaryTree.getLeftOperand(), state);
      Optional<ConstantExpression> rhs = constantExpression(binaryTree.getRightOperand(), state);
      if (lhs.isPresent() && rhs.isPresent()) {
        return Optional.of(
            ConstantExpression.constantEquals(ConstantEquals.of(lhs.get(), rhs.get())));
      }
    }
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

  /** Visitor for scanning over the components of a constant expression. */
  public interface ConstantExpressionVisitor {
    default void visitConstant(Object constant) {}

    default void visitIdentifier(Symbol identifier) {}
  }
}
