/*
 * Copyright 2019 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.time;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.names.NamingConventions.splitToLowercaseTerms;
import static com.google.errorprone.suppliers.Suppliers.DOUBLE_TYPE;
import static com.google.errorprone.suppliers.Suppliers.INT_TYPE;
import static com.google.errorprone.suppliers.Suppliers.LONG_TYPE;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.sun.source.tree.Tree.Kind.MEMBER_SELECT;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/** Checker that detects likely time-unit mismatches by looking at identifier names. */
@BugPattern(
    name = "TimeUnitMismatch",
    summary =
        "An value that appears to be represented in one unit is used where another appears to be "
            + "required (e.g., seconds where nanos are needed)",
    severity = WARNING,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public final class TimeUnitMismatch extends BugChecker
    implements AssignmentTreeMatcher,
        MethodInvocationTreeMatcher,
        NewClassTreeMatcher,
        VariableTreeMatcher {
  @Override
  public Description matchAssignment(AssignmentTree tree, VisitorState state) {
    String formalName = extractArgumentName(tree.getVariable());
    if (formalName != null) {
      check(formalName, tree.getExpression(), state);
    }
    return ANY_MATCHES_WERE_ALREADY_REPORTED;
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (tree.getInitializer() != null) {
      check(tree.getName().toString(), tree.getInitializer(), state);
    }
    return ANY_MATCHES_WERE_ALREADY_REPORTED;
  }

  /*
   * TODO(cpovirk): Hardcode a list of methods that are very common or have surprising return types
   * or argument types, and consult that list when matching method/constructor calls. (Maybe even
   * hardcode some methods whose parameters have meaningful types if we fear that some people will
   * compile against .class files that were compiled without parameter names?) e.g.,
   * SystemClock.elapsedRealtime is millis. And how about Stopwatch.elapsed(TimeUnit) and perhaps
   * similar methods?
   */

  /*
   * TODO(cpovirk): Check `return` statements against the type suggested by the method name (or from
   * the hardcoded list, since mismatches there seem more likely -- e.g., Ticker.read() that returns
   * elapsedRealtime()).
   */

  /*
   * TODO(cpovirk): Write a separate check that looks for methods whose units are unclear from the
   * method+parameter names. Identify that they are in fact time-unit methods by the fact that
   * people are passing parameters whose units we can detect (in which case we can give a
   * suggestion!) or from generic names like "now"/"time"/"instant." In addition to giving a
   * suggested fix that is a rename, also say in prose that it's better to use Duration, etc.
   */

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    MethodSymbol symbol = getSymbol(tree);
    if (symbol != null) {
      checkAll(symbol.getParameters(), tree.getArguments(), state);
    }
    return ANY_MATCHES_WERE_ALREADY_REPORTED;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MethodSymbol symbol = getSymbol(tree);
    if (symbol != null) {
      checkTimeUnitToUnit(tree, symbol, state);
      checkAll(symbol.getParameters(), tree.getArguments(), state);
    }
    return ANY_MATCHES_WERE_ALREADY_REPORTED;
  }

  /*
   * TODO(cpovirk): Match addition, subtraction, and division in which args are of different types?
   * I wonder if division will have weird edge cases in which people are trying to write
   * conversions?? I think I'm confusing myself, though: The check will probably be correct if it
   * uses the same mismatch rules for division as for the other two.
   */

  /**
   * Checks whether this call is a call to {@code TimeUnit.to*} and, if so, whether the units of its
   * parameter and its receiver disagree.
   */
  private void checkTimeUnitToUnit(
      MethodInvocationTree tree, MethodSymbol methodSymbol, VisitorState state) {
    if (tree.getMethodSelect().getKind() != MEMBER_SELECT) {
      return;
    }

    MemberSelectTree memberSelect = (MemberSelectTree) tree.getMethodSelect();
    Symbol receiverSymbol = getSymbol(memberSelect.getExpression());
    if (receiverSymbol == null) {
      return;
    }

    if (isTimeUnit(receiverSymbol, state)
        && receiverSymbol.isEnum()
        && TIME_UNIT_TO_UNIT_METHODS.containsValue(methodSymbol.getSimpleName().toString())
        && tree.getArguments().size() == 1) {
      check(receiverSymbol.getSimpleName().toString(), getOnlyElement(tree.getArguments()), state);
    }
  }

  private static boolean isTimeUnit(Symbol receiverSymbol, VisitorState state) {
    return isSameType(
        state.getTypeFromString("java.util.concurrent.TimeUnit"), receiverSymbol.type, state);
  }

  private static final ImmutableBiMap<TimeUnit, String> TIME_UNIT_TO_UNIT_METHODS =
      new ImmutableBiMap.Builder<TimeUnit, String>()
          .put(NANOSECONDS, "toNanos")
          .put(MICROSECONDS, "toMicros")
          .put(MILLISECONDS, "toMillis")
          .put(SECONDS, "toSeconds")
          .put(MINUTES, "toMinutes")
          .put(HOURS, "toHours")
          .put(DAYS, "toDays")
          .build();

  private void checkAll(
      List<VarSymbol> formals, List<? extends ExpressionTree> actuals, VisitorState state) {
    if (formals.size() != actuals.size()) {
      // varargs? weird usages of inner classes? TODO(cpovirk): Handle those correctly.
      return;
    }

    /*
     * TODO(cpovirk): Look for calls with a bad TimeUnit parameter: "foo(timeoutMillis, SECONDS)."
     * This is the kind of thing that DurationToLongTimeUnit covers but more generic.
     */

    for (int i = 0; i < formals.size(); i++) {
      check(formals.get(i).getSimpleName().toString(), actuals.get(i), state);
    }
  }

  private void check(String formalName, ExpressionTree actualTree, VisitorState state) {
    /*
     * Sometimes people name a Duration parameter something like "durationMs." Then we falsely
     * report a problem if the value comes from Duration.ofSeconds(). Let's stick to numeric types.
     *
     * TODO(cpovirk): But consider looking at List<Integer> and even String, for which I've seen
     * possible mistakes.
     */
    if (!NUMERIC_TIME_TYPE.matches(actualTree, state)) {
      return;
    }

    /*
     * TODO(cpovirk): Is it worth assuming, e.g., that a literal "60" is likely to be a number of
     * seconds?
     */

    String actualName = extractArgumentName(actualTree);
    if (actualName == null) {
      /*
       * TODO(cpovirk): Look for other assignments to a variable in the method to guess its type.
       * (Maybe even guess the type returned by a method by looking at other calls in the file?) Of
       * course, that may be slow.
       */
      // TODO(cpovirk): Look for multiplication/division operations that are meant to change units.
      // TODO(cpovirk): ...even if they include casts!
      return;
    }

    TimeUnit formalUnit = unitSuggestedByName(formalName);
    TimeUnit actualUnit = unitSuggestedByName(actualName);
    if (formalUnit == null || actualUnit == null || formalUnit == actualUnit) {
      return;
    }

    String message =
        String.format(
            "Possible unit mismatch: expected %s but was %s. Before accepting this change, make "
                + "sure that there is a true unit mismatch and not just an identifier whose name "
                + "contains the wrong unit. (If there is, correct that instead!)",
            formalUnit.toString().toLowerCase(Locale.ROOT),
            actualUnit.toString().toLowerCase(Locale.ROOT));
    if ((actualUnit == MICROSECONDS || actualUnit == MILLISECONDS)
        && (formalUnit == MICROSECONDS || formalUnit == MILLISECONDS)) {
      // TODO(cpovirk): Display this only if the code contained one of the ambiguous terms.
      message +=
          " WARNING: This checker considers \"ms\" and \"msec\" to always refer to *milli*seconds. "
              + "Occasionally, code uses them for *micro*seconds. If this error involves "
              + "identifiers with those terms, be sure to check that it does mean milliseconds "
              + "before accepting this fix. If it instead means microseconds, consider renaming to "
              + "\"us\" or \"usec\" (or just \"micros\").";
      // TODO(cpovirk): More ambitiously, suggest an edit to rename the identifier to "micros," etc.
    } else if (formalUnit == SECONDS && (actualUnit != HOURS && actualUnit != DAYS)) {
      message +=
          " WARNING: The suggested replacement truncates fractional seconds, so a value "
              + "like 999ms becomes 0.";
      message +=
          "Consider performing a floating-point division instead.";
      // TODO(cpovirk): Offer this as a suggested fix.
    }

    /*
     * TODO(cpovirk): I saw two instances in which the fix needs to be "backward" because the value
     * is a rate. For example, to convert "queries per second" to "queries per millisecond," we need
     * to _multiply_ by 1000, rather than divide as we would if we were converting seconds to
     * milliseconds.
     */
    SuggestedFix.Builder fix = SuggestedFix.builder();
    // TODO(cpovirk): This can conflict with constants with names like "SECONDS."
    fix.addStaticImport(TimeUnit.class.getName() + "." + actualUnit);
    // TODO(cpovirk): This won't work for `double` and won't work if the output needs to be `int`.
    fix.prefixWith(
        actualTree, String.format("%s.%s(", actualUnit, TIME_UNIT_TO_UNIT_METHODS.get(formalUnit)));
    fix.postfixWith(actualTree, ")");
    /*
     * TODO(cpovirk): Often a better fix would be Duration.ofMillis(...).toNanos(). However, that
     * implies that the values are durations rather than instants, and it requires Java 8 (and some
     * utility methods in the case of micros). Maybe we should suggest a number of possible fixes?
     */
    state.reportMatch(buildDescription(actualTree).setMessage(message).addFix(fix.build()).build());
    /*
     * TODO(cpovirk): Supply a different fix in the matchTimeUnitToUnit case (or the similar case in
     * which someone is calling, say, toMillis() but should be calling toDays(). The current fix
     * produces nested toFoo(...) calls. A better fix would be to replace the existing call with a
     * corrected call.
     */
  }

  /**
   * Extracts the "argument name," as defined in section 2.1 of "Nomen est Omen," from the
   * expression. This translates a potentially complex expression into a simple name that can be
   * used by the similarity metric.
   *
   * <p>"Nomen est Omen: Exploring and Exploiting Similarities between Argument and Parameter
   * Names," ICSE 2016
   */
  @Nullable
  private static String extractArgumentName(ExpressionTree expr) {
    switch (expr.getKind()) {
      case MEMBER_SELECT:
        {
          // If we have a field or method access, we use the name of the field/method. (We ignore
          // the name of the receiver object.) Exception: If the method is named "get" (Optional,
          // Flag, etc.), we use the name of the object or class that it's called on.
          MemberSelectTree memberSelect = (MemberSelectTree) expr;
          String member = memberSelect.getIdentifier().toString();
          return member.equals("get") ? extractArgumentName(memberSelect.getExpression()) : member;
        }
      case METHOD_INVOCATION:
        {
          // If we have a 'call expression' we use the name of the method we are calling. Exception:
          // If the method is named "get," we use the object or class instead. (See above.)
          Symbol sym = getSymbol(expr);
          if (sym == null) {
            return null;
          }
          String methodName = sym.getSimpleName().toString();
          return methodName.equals("get")
              ? extractArgumentName(((MethodInvocationTree) expr).getMethodSelect())
              : methodName;
        }
      case IDENTIFIER:
        {
          IdentifierTree idTree = (IdentifierTree) expr;
          if (idTree.getName().contentEquals("this")) {
            // for the 'this' keyword the argument name is the name of the object's class
            Symbol sym = getSymbol(idTree);
            return (sym == null) ? null : enclosingClass(sym).getSimpleName().toString();
          } else {
            // if we have a variable, just extract its name
            return ((IdentifierTree) expr).getName().toString();
          }
        }
      default:
        return null;
    }
  }

  // TODO(cpovirk): Theoretically we'd want to handle byte, float, and short, too.
  private static final Matcher<Tree> NUMERIC_TIME_TYPE =
      anyOf(
          isSameType(INT_TYPE),
          isSameType(LONG_TYPE),
          isSameType(DOUBLE_TYPE),
          isSameType("java.lang.Integer"),
          isSameType("java.lang.Long"),
          isSameType("java.lang.Double"));

  @VisibleForTesting
  static TimeUnit unitSuggestedByName(String name) {
    // Tuple types, especially Pair, trip us up. Skip APIs that might be from them.
    // This check is somewhat redundant with the "second" check below.
    // TODO(cpovirk): Skip APIs only if they're from a type that also declares a first/getFirst()?
    if (name.equals("second") || name.equals("getSecond")) {
      return null;
    }

    // http://grepcode.com/file/repo1.maven.org/maven2/mysql/mysql-connector-java/5.1.33/com/mysql/jdbc/TimeUtil.java#336
    if (name.equals("secondsPart")) {
      return NANOSECONDS;
    }

    // The name of a Google-internal method, but I see other methods with this name externally.
    if (name.equals("msToS")) {
      return SECONDS;
    }

    List<String> words = fixUnitCamelCase(splitToLowercaseTerms(name));

    // People use variable names like "firstTimestamp" and "secondTimestamp."
    // This check is somewhat redundant with the "second" check above.
    if (words.get(0).equals("second")) {
      return null;
    }

    /*
     * Sometimes people write a method like "fromNanos()." Whatever unit that might return, it's
     * very unlikely to be nanos, so we give up.
     */
    if (hasNameOfFromUnits(words)) {
      return null;
    }

    /*
     * Sometimes people write "final int TWO_SECONDS = 2 * 1000." Whatever unit that might be in,
     * it's very unlikely to be seconds, so we give up.
     *
     * TODO(cpovirk): We could probably guess the unit correctly most of the time if we wanted.
     */
    if (isNamedForNumberOfUnits(words)) {
      return null;
    }

    Set<TimeUnit> units = timeUnits(words);
    /*
     * TODO(cpovirk): If the name has multiple units, like "millisToNanos," attempt to determine
     * which is the output. We can look not only at the method name but also at its parameter: If
     * the parameter is named "millis," then the output is presumably nanos.
     */
    return units.size() == 1 ? getOnlyElement(units) : null;
  }

  /** Returns true if the input looks like [from, seconds]. */
  private static boolean hasNameOfFromUnits(List<String> words) {
    return words.size() == 2
        && words.get(0).equals("from")
        && UNIT_FOR_SUFFIX.containsKey(words.get(1));
  }

  /** Returns true if the input looks like [five, seconds]. */
  private static boolean isNamedForNumberOfUnits(List<String> words) {
    return words.size() == 2
        && NUMBER_WORDS.contains(words.get(0))
        && UNIT_FOR_SUFFIX.containsKey(words.get(1));
  }

  // TODO(cpovirk): Maybe there's some test we could use in ICU4J? RuleBasedNumberFormat.SPELLOUT?
  private static final ImmutableSet<String> NUMBER_WORDS =
      ImmutableSet.of(
          "one",
          "two",
          "three",
          "four",
          "five",
          "six",
          "seven",
          "eight",
          "nine",
          "ten",
          "eleven",
          "twelve",
          "thirteen",
          "fourteen",
          "fifteen",
          "twenty",
          "thirty",
          "forty",
          "fifty",
          "sixty");

  /**
   * Converts "MS"/"MSec"/etc. to "Ms"/etc. and "microSeconds"/"MicroSeconds"/etc. to
   * "microseconds"/"Microseconds"/etc.
   */
  private static ImmutableList<String> fixUnitCamelCase(List<String> words) {
    ImmutableList.Builder<String> out = ImmutableList.builderWithExpectedSize(words.size());
    int i = 0;
    for (i = 0; i < words.size() - 1; i++) {
      String current = words.get(i);
      String next = words.get(i + 1);
      String together = current + next;

      if (UNIT_FOR_SUFFIX.containsKey(together)) {
        out.add(together);
        i++; // Move past `next`, as well.
      } else {
        out.add(current);
      }
    }
    if (i == words.size() - 1) {
      out.add(words.get(i));
    }
    return out.build();
  }

  private static Set<TimeUnit> timeUnits(List<String> wordsLists) {
    return wordsLists.stream()
        .map(UNIT_FOR_SUFFIX::get)
        .filter(x -> x != null)
        .collect(toImmutableSet());
  }

  // TODO(cpovirk): Unify with UNITS_TO_LOOK_FOR in other checkers. That also adds hours and days.
  // TODO(cpovirk): Maybe also add things like "weeks?"
  private static final ImmutableMap<String, TimeUnit> UNIT_FOR_SUFFIX =
      ImmutableMap.copyOf(
          new ImmutableSetMultimap.Builder<TimeUnit, String>()
              .putAll(SECONDS, "sec", "secs", "second", "seconds")
              .putAll(
                  MILLISECONDS, //
                  "milli",
                  "millis",
                  "mills",
                  "ms",
                  "msec",
                  "msecs",
                  "millisecond",
                  "milliseconds")
              // TODO(cpovirk): milis, milisecond, etc.? (i.e., misspellings)
              .putAll(
                  MICROSECONDS, //
                  "micro",
                  "micros",
                  "us",
                  "usec",
                  "usecs",
                  "microsecond",
                  "microseconds")
              .putAll(
                  NANOSECONDS, //
                  "nano",
                  "nanos",
                  "ns",
                  "nsec",
                  "nsecs",
                  "nanosecond",
                  "nanoseconds")
              .build()
              .inverse()
              .entries());

  /**
   * {@link Description#NO_MATCH}, returned by all our {@code match*} methods because any matches
   * have already been reported by manual calls to {@link VisitorState#reportMatch}.
   */
  private static final Description ANY_MATCHES_WERE_ALREADY_REPORTED = Description.NO_MATCH;
}
