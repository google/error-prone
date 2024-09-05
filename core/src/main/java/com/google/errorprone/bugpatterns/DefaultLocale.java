/*
 * Copyright 2024 The Error Prone Authors.
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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.FieldMatchers.staticField;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.receiverOfInvocation;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Formattable;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Implicit use of the JVM default locale, which can result in differing behaviour between"
            + " JVM executions.",
    severity = WARNING)
public class DefaultLocale extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private enum LocaleFix {
    ROOT_FIX("Specify ROOT locale") {
      @Override
      String replacement(SuggestedFix.Builder fix, VisitorState state) {
        fix.addImport("java.util.Locale");
        return "Locale.ROOT";
      }
    },
    DEFAULT_LOCALE_FIX("Specify default locale") {
      @Override
      String replacement(SuggestedFix.Builder fix, VisitorState state) {
        fix.addImport("java.util.Locale");
        return "Locale.getDefault()";
      }
    },
    DEFAULT_DISPLAY_LOCALE_FIX("Specify default display locale") {
      @Override
      String replacement(SuggestedFix.Builder fix, VisitorState state) {
        fix.addImport("java.util.Locale");
        return String.format(
            "Locale.getDefault(%s)",
            SuggestedFixes.qualifyStaticImport("java.util.Locale.Category.DISPLAY", fix, state));
      }
    },
    DEFAULT_FORMAT_LOCALE_FIX("Specify default format locale") {
      @Override
      String replacement(SuggestedFix.Builder fix, VisitorState state) {
        fix.addImport("java.util.Locale");
        return String.format(
            "Locale.getDefault(%s)",
            SuggestedFixes.qualifyStaticImport("java.util.Locale.Category.FORMAT", fix, state));
      }
    };

    private final String title;

    LocaleFix(String title) {
      this.title = title;
    }

    String title() {
      return title;
    }

    abstract String replacement(SuggestedFix.Builder fix, VisitorState state);
  }

  private static final Pattern SPECIFIER_ALLOW_LIST_REGEX =
      Pattern.compile("%([%n]|(\\d+\\$|<)?-?\\d*(\\.\\d+)?[bhsc])");

  private static final Supplier<Type> FORMATTABLE = Suppliers.typeFromClass(Formattable.class);

  private static final Supplier<Type> APPENDABLE = Suppliers.typeFromClass(Appendable.class);

  private static final Supplier<Type> PRINTSTREAM = Suppliers.typeFromClass(PrintStream.class);

  private static final ImmutableList<Supplier<Type>> PATTERN_AND_ARGS =
      ImmutableList.of(Suppliers.STRING_TYPE, Suppliers.arrayOf(Suppliers.OBJECT_TYPE));

  private static final Matcher<ExpressionTree> FORMAT_METHODS =
      anyOf(
          instanceMethod()
              .onDescendantOfAny(PrintStream.class.getName(), PrintWriter.class.getName())
              .namedAnyOf("format", "printf")
              .withParametersOfType(PATTERN_AND_ARGS),
          staticMethod()
              .onClass(Suppliers.STRING_TYPE)
              .named("format")
              .withParametersOfType(PATTERN_AND_ARGS));

  private static final Matcher<MethodInvocationTree> SYSTEM_OUT_RECEIVER =
      receiverOfInvocation(
          anyOf(
              staticField(System.class.getName(), "out"),
              staticField(System.class.getName(), "err")));

  private static final Matcher<ExpressionTree> STRING_FORMATTED =
      instanceMethod().onExactClass(Suppliers.STRING_TYPE).named("formatted");

  private static final Matcher<ExpressionTree> DISPLAY_METHODS =
      instanceMethod().onExactClass("java.util.Currency").named("getSymbol").withNoParameters();

  private static final Matcher<ExpressionTree> FACTORIES =
      anyOf(
          staticMethod()
              .onClass("java.text.BreakIterator")
              .namedAnyOf(
                  "getCharacterInstance",
                  "getLineInstance",
                  "getSentenceInstance",
                  "getWordInstance")
              .withNoParameters(),
          staticMethod().onClass("java.text.Collator").named("getInstance").withNoParameters());

  private static final Matcher<ExpressionTree> FORMATTER_FACTORIES =
      anyOf(
          staticMethod()
              .onClass("java.text.NumberFormat")
              .namedAnyOf(
                  "getCompactNumberInstance",
                  "getCurrencyInstance",
                  "getInstance",
                  "getIntegerInstance",
                  "getNumberInstance",
                  "getPercentInstance")
              .withNoParameters(),
          staticMethod()
              .onClass("java.text.DateFormatSymbols")
              .named("getInstance")
              .withNoParameters(),
          staticMethod()
              .onClass("java.text.DecimalFormatSymbols")
              .named("getInstance")
              .withNoParameters(),
          staticMethod()
              .onClass("java.time.format.DateTimeFormatter")
              .named("ofPattern")
              .withParametersOfType(ImmutableList.of(Suppliers.STRING_TYPE)),
          instanceMethod()
              .onDescendantOf("java.time.format.DateTimeFormatterBuilder")
              .named("toFormatter")
              .withNoParameters());

  private static final Matcher<ExpressionTree> DATE_FORMAT =
      anyOf(
          staticMethod().onClass("java.text.DateFormat").named("getInstance").withNoParameters(),
          staticMethod()
              .onClass("java.text.DateFormat")
              .namedAnyOf("getDateInstance", "getTimeInstance")
              .withNoParameters(),
          staticMethod()
              .onClass("java.text.DateFormat")
              .namedAnyOf("getDateInstance", "getTimeInstance")
              .withParametersOfType(ImmutableList.of(Suppliers.INT_TYPE)),
          staticMethod()
              .onClass("java.text.DateFormat")
              .named("getDateTimeInstance")
              .withNoParameters(),
          staticMethod()
              .onClass("java.text.DateFormat")
              .named("getDateTimeInstance")
              .withParametersOfType(ImmutableList.of(Suppliers.INT_TYPE, Suppliers.INT_TYPE)));

  private static final Matcher<ExpressionTree> MESSAGEFORMAT_FORMAT =
      staticMethod()
          .onClass("java.text.MessageFormat")
          .named("format")
          .withParametersOfType(PATTERN_AND_ARGS);

  private static final Matcher<ExpressionTree> RESOURCE_BUNDLE =
      anyOf(
          staticMethod()
              .onClass("java.util.ResourceBundle")
              .named("getBundle")
              .withParametersOfType(ImmutableList.of(Suppliers.STRING_TYPE)),
          staticMethod()
              .onClass("java.util.ResourceBundle")
              .named("getBundle")
              .withParameters("java.lang.String", "java.util.ResourceBundle.Control"),
          staticMethod()
              .onClass("java.util.ResourceBundle")
              .named("getBundle")
              .withParameters("java.lang.String", "java.lang.Module"));

  private static final Matcher<ExpressionTree> FORMAT_CONSTRUCTORS =
      anyOf(
          constructor()
              .forClass("java.text.MessageFormat")
              .withParametersOfType(ImmutableList.of(Suppliers.STRING_TYPE)),
          constructor().forClass("java.text.DateFormatSymbols").withNoParameters(),
          constructor().forClass("java.text.DecimalFormatSymbols").withNoParameters());

  private static final Matcher<ExpressionTree> DECIMAL_FORMAT =
      anyOf(
          constructor().forClass("java.text.DecimalFormat").withNoParameters(),
          constructor()
              .forClass("java.text.DecimalFormat")
              .withParametersOfType(ImmutableList.of(Suppliers.STRING_TYPE)));

  private static final Matcher<ExpressionTree> SIMPLE_DATE_FORMAT =
      anyOf(
          constructor().forClass("java.text.SimpleDateFormat").withNoParameters(),
          constructor()
              .forClass("java.text.SimpleDateFormat")
              .withParametersOfType(ImmutableList.of(Suppliers.STRING_TYPE)));

  private static final Matcher<ExpressionTree> FORMATTER =
      anyOf(
          constructor().forClass("java.util.Formatter").withNoParameters(),
          constructor()
              .forClass("java.util.Formatter")
              .withParametersOfType(ImmutableList.of(Suppliers.STRING_TYPE)),
          constructor()
              .forClass("java.util.Formatter")
              .withParametersOfType(ImmutableList.of(Suppliers.STRING_TYPE, Suppliers.STRING_TYPE)),
          constructor().forClass("java.util.Formatter").withParameters("java.lang.Appendable"),
          constructor().forClass("java.util.Formatter").withParameters("java.io.File"),
          constructor()
              .forClass("java.util.Formatter")
              .withParameters("java.io.File", "java.lang.String"),
          constructor().forClass("java.util.Formatter").withParameters("java.io.PrintStream"),
          constructor().forClass("java.util.Formatter").withParameters("java.io.OutputStream"),
          constructor()
              .forClass("java.util.Formatter")
              .withParameters("java.io.OutputStream", "java.lang.String"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    // String.toUpperCase/toLowerCase are already handled by StringCaseLocaleUsage
    if (FORMAT_METHODS.matches(tree, state)) {
      // Allow System.out and System.err
      if (SYSTEM_OUT_RECEIVER.matches(tree, state)
          || !shouldRefactorStringFormat(
              tree.getArguments().get(0),
              tree.getArguments().stream().skip(1).collect(toImmutableList()),
              state)) {
        return NO_MATCH;
      }
      return prependLocales(
          tree,
          state,
          LocaleFix.ROOT_FIX,
          LocaleFix.DEFAULT_LOCALE_FIX,
          LocaleFix.DEFAULT_FORMAT_LOCALE_FIX);
    }
    if (STRING_FORMATTED.matches(tree, state)) {
      return handleStringFormatted(tree, state);
    }
    if (DISPLAY_METHODS.matches(tree, state)) {
      return appendLocales(
          tree,
          state,
          LocaleFix.ROOT_FIX,
          LocaleFix.DEFAULT_LOCALE_FIX,
          LocaleFix.DEFAULT_DISPLAY_LOCALE_FIX);
    }
    if (FACTORIES.matches(tree, state)) {
      return appendLocales(tree, state, LocaleFix.ROOT_FIX, LocaleFix.DEFAULT_LOCALE_FIX);
    }
    if (FORMATTER_FACTORIES.matches(tree, state)) {
      return appendLocales(
          tree,
          state,
          LocaleFix.ROOT_FIX,
          LocaleFix.DEFAULT_LOCALE_FIX,
          LocaleFix.DEFAULT_FORMAT_LOCALE_FIX);
    }
    if (DATE_FORMAT.matches(tree, state)) {
      return handleDateFormat(tree, state);
    }
    if (MESSAGEFORMAT_FORMAT.matches(tree, state)) {
      return handleMessageFormatFormat(tree, state);
    }
    if (RESOURCE_BUNDLE.matches(tree, state)) {
      return handleResourceBundle(tree, state);
    }
    return NO_MATCH;
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (FORMAT_CONSTRUCTORS.matches(tree, state)) {
      return appendLocales(
          tree,
          state,
          LocaleFix.ROOT_FIX,
          LocaleFix.DEFAULT_LOCALE_FIX,
          LocaleFix.DEFAULT_FORMAT_LOCALE_FIX);
    }
    if (DECIMAL_FORMAT.matches(tree, state)) {
      return handleDecimalFormat(tree, state);
    }
    if (SIMPLE_DATE_FORMAT.matches(tree, state)) {
      return handleSimpleDateFormat(tree, state);
    }
    if (FORMATTER.matches(tree, state)) {
      return handleFormatter(tree, state);
    }
    return NO_MATCH;
  }

  private Description handleStringFormatted(MethodInvocationTree tree, VisitorState state) {
    if (!shouldRefactorStringFormat(ASTHelpers.getReceiver(tree), tree.getArguments(), state)) {
      return NO_MATCH;
    }
    var description = buildDescription(tree);
    description.addFix(stringFormattedFix(tree, state, LocaleFix.ROOT_FIX));
    description.addFix(stringFormattedFix(tree, state, LocaleFix.DEFAULT_LOCALE_FIX));
    description.addFix(stringFormattedFix(tree, state, LocaleFix.DEFAULT_FORMAT_LOCALE_FIX));
    return description.build();
  }

  private Fix stringFormattedFix(
      MethodInvocationTree tree, VisitorState state, LocaleFix localeFix) {
    var fix = SuggestedFix.builder().setShortDescription(localeFix.title());
    fix.replace(
        tree,
        String.format(
            "String.format(%s, %s, %s)",
            localeFix.replacement(fix, state),
            state.getSourceForNode(ASTHelpers.getReceiver(tree)),
            tree.getArguments().stream()
                .map(state::getSourceForNode)
                .collect(Collectors.joining(", "))));
    return fix.build();
  }

  private Description handleDateFormat(MethodInvocationTree tree, VisitorState state) {
    var description = buildDescription(tree);
    var methodName = ASTHelpers.getSymbol(tree).getSimpleName();
    if (methodName.contentEquals("getInstance")) {
      dateFormatGetInstanceFixes(description, tree, state);
    } else if (methodName.contentEquals("getDateTimeInstance")) {
      dateFormatFixes(description, tree, state, 2);
    } else {
      dateFormatFixes(description, tree, state, 1);
    }
    return description.build();
  }

  private void dateFormatGetInstanceFixes(
      Description.Builder description, MethodInvocationTree tree, VisitorState state) {
    description.addFix(dateFormatGetInstanceFix(tree, state, LocaleFix.ROOT_FIX));
    description.addFix(dateFormatGetInstanceFix(tree, state, LocaleFix.DEFAULT_LOCALE_FIX));
    description.addFix(dateFormatGetInstanceFix(tree, state, LocaleFix.DEFAULT_FORMAT_LOCALE_FIX));
  }

  private Fix dateFormatGetInstanceFix(
      MethodInvocationTree tree, VisitorState state, LocaleFix localeFix) {
    var fix = SuggestedFix.builder().setShortDescription(localeFix.title());
    fix.replace(
            state.getEndPosition(tree.getMethodSelect()),
            state.getEndPosition(tree),
            String.format(
                "(%1$s, %<s, %2$s)",
                SuggestedFixes.qualifyStaticImport("java.text.DateFormat.SHORT", fix, state),
                localeFix.replacement(fix, state)))
        .merge(SuggestedFixes.renameMethodInvocation(tree, "getDateTimeInstance", state));
    return fix.build();
  }

  private void dateFormatFixes(
      Description.Builder description,
      MethodInvocationTree tree,
      VisitorState state,
      int nonLocaleArgs) {
    description.addFix(dateFormatFix(tree, state, nonLocaleArgs, LocaleFix.ROOT_FIX));
    description.addFix(dateFormatFix(tree, state, nonLocaleArgs, LocaleFix.DEFAULT_LOCALE_FIX));
    description.addFix(
        dateFormatFix(tree, state, nonLocaleArgs, LocaleFix.DEFAULT_FORMAT_LOCALE_FIX));
  }

  private Fix dateFormatFix(
      MethodInvocationTree tree, VisitorState state, int nonLocaleArgs, LocaleFix localeFix) {
    var fix = SuggestedFix.builder().setShortDescription(localeFix.title());
    if (tree.getArguments().isEmpty()) {
      var defaultConst =
          SuggestedFixes.qualifyStaticImport("java.text.DateFormat.DEFAULT", fix, state);
      fix.replace(
          state.getEndPosition(tree.getMethodSelect()),
          state.getEndPosition(tree),
          String.format(
              "(%s, %s)",
              Stream.generate(() -> defaultConst)
                  .limit(nonLocaleArgs)
                  .collect(Collectors.joining(", ")),
              localeFix.replacement(fix, state)));
    } else {
      fix.postfixWith(
          Iterables.getLast(tree.getArguments()), ", " + localeFix.replacement(fix, state));
    }
    return fix.build();
  }

  private Description handleMessageFormatFormat(MethodInvocationTree tree, VisitorState state) {
    var pattern = tree.getArguments().get(0);
    var arguments = tree.getArguments().stream().skip(1).collect(toImmutableList());
    if (!shouldRefactorStringFormat(pattern, arguments, state)) {
      return NO_MATCH;
    }
    var description = buildDescription(tree);
    description.addFix(messageFormatFormatFix(tree, pattern, arguments, state, LocaleFix.ROOT_FIX));
    description.addFix(
        messageFormatFormatFix(tree, pattern, arguments, state, LocaleFix.DEFAULT_LOCALE_FIX));
    description.addFix(
        messageFormatFormatFix(
            tree, pattern, arguments, state, LocaleFix.DEFAULT_FORMAT_LOCALE_FIX));
    return description.build();
  }

  /**
   * Should only refactor String.format() and similar methods' invocations where specifiers aren't
   * locale-dependent. For %s this includes checking for non-Formattable arguments. Format strings
   * (first argument) as variables or constants are excluded from refactoring.
   */
  private boolean shouldRefactorStringFormat(
      ExpressionTree pattern, List<? extends ExpressionTree> arguments, VisitorState state) {
    String patternValue = ASTHelpers.constValue(pattern, String.class);
    // TODO: add a flag to be stricter and reformat whenever the pattern is not a constant
    if (patternValue != null && !onlyContainsSpecifiersInAllowList(patternValue)) {
      return true;
    }
    // Ideally we'd only check for Formattable on arguments used in %s specifiers
    return containsSomeFormattableArgument(arguments, state);
  }

  @VisibleForTesting
  static boolean onlyContainsSpecifiersInAllowList(String pattern) {
    var noSpecifierFormatBase = SPECIFIER_ALLOW_LIST_REGEX.matcher(pattern).replaceAll("");
    // If it still has a specifier after the replacement, it means that it was not on the allowlist.
    return !noSpecifierFormatBase.contains("%");
  }

  private boolean containsSomeFormattableArgument(
      List<? extends ExpressionTree> arguments, VisitorState state) {
    return arguments.stream().anyMatch(tree -> mightBeFormattable(tree, state));
  }

  private boolean mightBeFormattable(ExpressionTree tree, VisitorState state) {
    if (tree instanceof LiteralTree) {
      return false;
    }
    // TODO: add a flag to be stricter and detect any argument that could be cast to Formattable
    //       (rather than only the ones that are proven to be Formattable)
    return ASTHelpers.isSubtype(ASTHelpers.getResultType(tree), FORMATTABLE.get(state), state);
  }

  private Fix messageFormatFormatFix(
      MethodInvocationTree tree,
      ExpressionTree pattern,
      ImmutableList<? extends ExpressionTree> arguments,
      VisitorState state,
      LocaleFix localeFix) {
    var fix = SuggestedFix.builder().setShortDescription(localeFix.title());
    fix.replace(
        tree,
        String.format(
            "new %s(%s, %s).format(%s)",
            SuggestedFixes.qualifyType(state, fix, "java.text.MessageFormat"),
            state.getSourceForNode(pattern),
            localeFix.replacement(fix, state),
            arguments.stream().map(state::getSourceForNode).collect(Collectors.joining(", "))));
    return fix.build();
  }

  private Description handleResourceBundle(MethodInvocationTree tree, VisitorState state) {
    var description = buildDescription(tree);
    description.addFix(resourceBundleFix(tree, state, LocaleFix.ROOT_FIX));
    description.addFix(resourceBundleFix(tree, state, LocaleFix.DEFAULT_LOCALE_FIX));
    return description.build();
  }

  private Fix resourceBundleFix(
      MethodInvocationTree tree, VisitorState state, LocaleFix localeFix) {
    var fix = SuggestedFix.builder().setShortDescription(localeFix.title());
    fix.postfixWith(tree.getArguments().get(0), ", " + localeFix.replacement(fix, state));
    return fix.build();
  }

  private Description handleDecimalFormat(NewClassTree tree, VisitorState state) {
    var description = buildDescription(tree);
    if (tree.getArguments().isEmpty()) {
      description.addFix(decimalFormatToNumberFormatFix(tree, state, LocaleFix.ROOT_FIX));
      description.addFix(decimalFormatToNumberFormatFix(tree, state, LocaleFix.DEFAULT_LOCALE_FIX));
      description.addFix(
          decimalFormatToNumberFormatFix(tree, state, LocaleFix.DEFAULT_FORMAT_LOCALE_FIX));
    } else {
      description.addFix(decimalFormatFix(tree, state, LocaleFix.ROOT_FIX));
      description.addFix(decimalFormatFix(tree, state, LocaleFix.DEFAULT_LOCALE_FIX));
      description.addFix(decimalFormatFix(tree, state, LocaleFix.DEFAULT_FORMAT_LOCALE_FIX));
    }
    return description.build();
  }

  private Fix decimalFormatToNumberFormatFix(
      NewClassTree tree, VisitorState state, LocaleFix localeFix) {
    var fix =
        SuggestedFix.builder()
            .setShortDescription(localeFix.title())
            .addImport("java.text.NumberFormat");
    fix.replace(
        tree, String.format("NumberFormat.getInstance(%s)", localeFix.replacement(fix, state)));
    return fix.build();
  }

  private Fix decimalFormatFix(NewClassTree tree, VisitorState state, LocaleFix localeFix) {
    var fix =
        SuggestedFix.builder()
            .setShortDescription(localeFix.title())
            .addImport("java.text.DecimalFormatSymbols");
    fix.postfixWith(
        Iterables.getLast(tree.getArguments()),
        String.format(", DecimalFormatSymbols.getInstance(%s)", localeFix.replacement(fix, state)));
    return fix.build();
  }

  private Description handleSimpleDateFormat(NewClassTree tree, VisitorState state) {
    var description = buildDescription(tree);
    if (tree.getArguments().isEmpty()) {
      description.addFix(simpleDateFormatToDateFormatFix(tree, state, LocaleFix.ROOT_FIX));
      description.addFix(
          simpleDateFormatToDateFormatFix(tree, state, LocaleFix.DEFAULT_LOCALE_FIX));
      description.addFix(
          simpleDateFormatToDateFormatFix(tree, state, LocaleFix.DEFAULT_FORMAT_LOCALE_FIX));
    } else {
      description.addFix(simpleDateFormatFix(tree, state, LocaleFix.ROOT_FIX));
      description.addFix(simpleDateFormatFix(tree, state, LocaleFix.DEFAULT_LOCALE_FIX));
      description.addFix(simpleDateFormatFix(tree, state, LocaleFix.DEFAULT_FORMAT_LOCALE_FIX));
    }
    return description.build();
  }

  private Fix simpleDateFormatToDateFormatFix(
      NewClassTree tree, VisitorState state, LocaleFix localeFix) {
    var fix =
        SuggestedFix.builder()
            .setShortDescription(localeFix.title())
            .addImport("java.text.DateFormat");
    fix.replace(
        tree,
        String.format(
            "DateFormat.getDateTimeInstance(%1$s, %<s, %2$s)",
            SuggestedFixes.qualifyStaticImport("java.text.DateFormat.SHORT", fix, state),
            localeFix.replacement(fix, state)));
    return fix.build();
  }

  private Fix simpleDateFormatFix(NewClassTree tree, VisitorState state, LocaleFix localeFix) {
    var fix = SuggestedFix.builder().setShortDescription(localeFix.title());
    fix.postfixWith(
        Iterables.getLast(tree.getArguments()), ", " + localeFix.replacement(fix, state));
    return fix.build();
  }

  private Description handleFormatter(NewClassTree tree, VisitorState state) {
    if (tree.getArguments().isEmpty() || tree.getArguments().size() == 2) {
      return appendLocales(
          tree,
          state,
          LocaleFix.ROOT_FIX,
          LocaleFix.DEFAULT_LOCALE_FIX,
          LocaleFix.DEFAULT_FORMAT_LOCALE_FIX);
    }
    var argType = ASTHelpers.getResultType(Iterables.getOnlyElement(tree.getArguments()));
    if (ASTHelpers.isSubtype(argType, APPENDABLE.get(state), state)
        && !ASTHelpers.isSubtype(argType, PRINTSTREAM.get(state), state)) {
      return appendLocales(
          tree,
          state,
          LocaleFix.ROOT_FIX,
          LocaleFix.DEFAULT_LOCALE_FIX,
          LocaleFix.DEFAULT_FORMAT_LOCALE_FIX);
    }
    return buildDescription(tree).build();
  }

  private Description prependLocales(
      MethodInvocationTree tree, VisitorState state, LocaleFix... localeFixes) {
    return prependLocales(tree, tree.getMethodSelect(), tree.getArguments(), state, localeFixes);
  }

  private Description prependLocales(
      Tree tree,
      Tree select,
      List<? extends ExpressionTree> arguments,
      VisitorState state,
      LocaleFix... localeFixes) {
    Description.Builder description = buildDescription(tree);
    for (LocaleFix localeFix : localeFixes) {
      description.addFix(prependLocale(tree, select, arguments, state, localeFix));
    }
    return description.build();
  }

  private Fix prependLocale(
      Tree tree,
      Tree select,
      List<? extends ExpressionTree> arguments,
      VisitorState state,
      LocaleFix localeFix) {
    SuggestedFix.Builder fix = SuggestedFix.builder().setShortDescription(localeFix.title());
    if (arguments.isEmpty()) {
      fix.replace(
          state.getEndPosition(select),
          state.getEndPosition(tree),
          String.format("(%s)", localeFix.replacement(fix, state)));
    } else {
      fix.prefixWith(arguments.get(0), localeFix.replacement(fix, state) + ", ");
    }
    return fix.build();
  }

  private Description appendLocales(
      MethodInvocationTree tree, VisitorState state, LocaleFix... localeFixes) {
    return appendLocales(tree, tree.getMethodSelect(), tree.getArguments(), state, localeFixes);
  }

  private Description appendLocales(
      NewClassTree tree, VisitorState state, LocaleFix... localeFixes) {
    return appendLocales(tree, tree.getIdentifier(), tree.getArguments(), state, localeFixes);
  }

  private Description appendLocales(
      Tree tree,
      Tree select,
      List<? extends ExpressionTree> arguments,
      VisitorState state,
      LocaleFix... localeFixes) {
    Description.Builder description = buildDescription(tree);
    for (LocaleFix localeFix : localeFixes) {
      description.addFix(appendLocale(tree, select, arguments, state, localeFix));
    }
    return description.build();
  }

  private Fix appendLocale(
      Tree tree,
      Tree select,
      List<? extends ExpressionTree> arguments,
      VisitorState state,
      LocaleFix localeFix) {
    SuggestedFix.Builder fix = SuggestedFix.builder().setShortDescription(localeFix.title());
    if (arguments.isEmpty()) {
      fix.replace(
          state.getEndPosition(select),
          state.getEndPosition(tree),
          String.format("(%s)", localeFix.replacement(fix, state)));
    } else {
      fix.postfixWith(Iterables.getLast(arguments), ", " + localeFix.replacement(fix, state));
    }
    return fix.build();
  }
}
