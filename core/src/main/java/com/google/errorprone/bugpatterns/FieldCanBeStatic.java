/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.fixes.SuggestedFixes.addModifiers;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.annotationsAmong;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static javax.lang.model.element.ElementKind.FIELD;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ThreadSafety;
import com.google.errorprone.bugpatterns.threadsafety.WellKnownMutability;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Finds fields which can be safely made static. */
@BugPattern(
    name = "FieldCanBeStatic",
    summary =
        "A final field initialized at compile-time with an instance of an immutable type can be"
            + " static.",
    severity = SUGGESTION)
public final class FieldCanBeStatic extends BugChecker implements VariableTreeMatcher {

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
          staticMethod().onClass("org.joda.time.format.DateTimeFormatter"));

  private static final Supplier<ImmutableSet<Name>> EXEMPTING_VARIABLE_ANNOTATIONS =
      VisitorState.memoize(
          s ->
              Stream.of("com.google.inject.testing.fieldbinder.Bind")
                  .map(s::getName)
                  .collect(toImmutableSet()));

  private final WellKnownMutability wellKnownMutability;

  public FieldCanBeStatic(ErrorProneFlags flags) {
    this.wellKnownMutability = WellKnownMutability.fromFlags(flags);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    VarSymbol symbol = getSymbol(tree);
    if (symbol == null
        || !symbol.isPrivate()
        || !tree.getModifiers().getFlags().contains(FINAL)
        || symbol.isStatic()
        || !symbol.getKind().equals(FIELD)) {
      return NO_MATCH;
    }
    if (!isTypeKnownImmutable(getType(tree), state)) {
      return NO_MATCH;
    }
    if (!isPure(tree.getInitializer(), state)) {
      return NO_MATCH;
    }
    if (!annotationsAmong(symbol, EXEMPTING_VARIABLE_ANNOTATIONS.get(state), state).isEmpty()) {
      return NO_MATCH;
    }
    SuggestedFix fix =
        SuggestedFix.builder()
            .merge(renameVariable(tree, state))
            .merge(addModifiers(tree, state, STATIC).orElse(SuggestedFix.emptyFix()))
            .build();
    return describeMatch(tree, fix);
  }

  /**
   * Renames the variable, clobbering any qualifying (like {@code this.}). This is a tad unsafe, but
   * we need to somehow remove any qualification with an instance.
   */
  private SuggestedFix renameVariable(VariableTree variableTree, VisitorState state) {
    String name = variableTree.getName().toString();
    if (!LOWER_CAMEL_PATTERN.matcher(name).matches()) {
      return SuggestedFix.emptyFix();
    }
    String replacement = LOWER_CAMEL.to(UPPER_UNDERSCORE, variableTree.getName().toString());
    int typeEndPos = state.getEndPosition(variableTree.getType());
    int searchOffset = typeEndPos - ((JCTree) variableTree).getStartPosition();
    int pos =
        ((JCTree) variableTree).getStartPosition()
            + state.getSourceForNode(variableTree).indexOf(name, searchOffset);
    SuggestedFix.Builder fix =
        SuggestedFix.builder().replace(pos, pos + name.length(), replacement);
    VarSymbol sym = getSymbol(variableTree);
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        handle(tree);
        return super.visitIdentifier(tree, null);
      }

      @Override
      public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
        handle(tree);
        return super.visitMemberSelect(tree, null);
      }

      private void handle(Tree tree) {
        if (sym.equals(getSymbol(tree))) {
          fix.replace(tree, replacement);
        }
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return fix.build();
  }

  private static final Pattern LOWER_CAMEL_PATTERN = Pattern.compile("[a-z][a-zA-Z0-9]+");

  /**
   * Tries to establish whether an expression is pure. For example, literals and invocations of
   * known-pure functions are pure.
   */
  private boolean isPure(ExpressionTree initializer, VisitorState state) {
    AtomicBoolean isPure = new AtomicBoolean(true);
    new TreeScanner<Void, Void>() {
      @Override
      public Void scan(Tree tree, Void unused) {
        if (tree instanceof MethodInvocationTree) {
          if (!PURE_METHODS.matches((ExpressionTree) tree, state)) {
            isPure.set(false);
          }
          return super.scan(tree, null);
        } else if (tree instanceof BinaryTree
            || tree instanceof LiteralTree
            || tree instanceof ParenthesizedTree) {
          return super.scan(tree, null);
        } else if (tree instanceof IdentifierTree || tree instanceof MemberSelectTree) {
          Symbol symbol = getSymbol(tree);
          if (symbol instanceof VarSymbol && !(symbol.isStatic() && isConsideredFinal(symbol))) {
            isPure.set(false);
          }
          return super.scan(tree, null);
        } else {
          isPure.set(false);
          return null;
        }
      }
    }.scan(initializer, null);
    return isPure.get();
  }

  private boolean isTypeKnownImmutable(Type type, VisitorState state) {
    ThreadSafety threadSafety =
        ThreadSafety.builder()
            .setPurpose(ThreadSafety.Purpose.FOR_IMMUTABLE_CHECKER)
            .knownTypes(wellKnownMutability)
            .acceptedAnnotations(
                ImmutableSet.of(Immutable.class.getName(), AutoValue.class.getName()))
            .markerAnnotations(ImmutableSet.of())
            .build(state);
    return !threadSafety
        .isThreadSafeType(
            /* allowContainerTypeParameters= */ true,
            threadSafety.threadSafeTypeParametersInScope(type.tsym),
            type)
        .isPresent();
  }
}
