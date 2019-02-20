/*
 * Copyright 2015 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.Optional;
import javax.lang.model.element.ElementKind;

/**
 * Ban use of YYYY in a SimpleDateFormat pattern, unless it is being used for a week date. Otherwise
 * the user almost certainly meant yyyy instead. See the summary in the {@link BugPattern} below for
 * more details.
 *
 * <p>This bug caused a Twitter outage in December 2014.
 */
@BugPattern(
    name = "MisusedWeekYear",
    summary =
        "Use of \"YYYY\" (week year) in a date pattern without \"ww\" (week in year). "
            + "You probably meant to use \"yyyy\" (year) instead.",
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class MisusedWeekYear extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private static final String JAVA_SIMPLE_DATE_FORMAT = "java.text.SimpleDateFormat";
  private static final String ICU_SIMPLE_DATE_FORMAT = "com.ibm.icu.text.SimpleDateFormat";

  private static final Matcher<NewClassTree> PATTERN_CTOR_MATCHER =
      Matchers.<NewClassTree>anyOf(
          constructor().forClass(JAVA_SIMPLE_DATE_FORMAT).withParameters("java.lang.String"),
          constructor()
              .forClass(JAVA_SIMPLE_DATE_FORMAT)
              .withParameters("java.lang.String", "java.text.DateFormatSymbols"),
          constructor()
              .forClass(JAVA_SIMPLE_DATE_FORMAT)
              .withParameters("java.lang.String", "java.util.Locale"),
          constructor().forClass(ICU_SIMPLE_DATE_FORMAT).withParameters("java.lang.String"),
          constructor()
              .forClass(ICU_SIMPLE_DATE_FORMAT)
              .withParameters("java.lang.String", "com.ibm.icu.text.DateFormatSymbols"),
          constructor()
              .forClass(ICU_SIMPLE_DATE_FORMAT)
              .withParameters(
                  "java.lang.String",
                  "com.ibm.icu.text.DateFormatSymbols",
                  "com.ibm.icu.util.ULocale"),
          constructor()
              .forClass(ICU_SIMPLE_DATE_FORMAT)
              .withParameters("java.lang.String", "java.util.Locale"),
          constructor()
              .forClass(ICU_SIMPLE_DATE_FORMAT)
              .withParameters("java.lang.String", "java.lang.String", "com.ibm.icu.util.ULocale"),
          constructor()
              .forClass(ICU_SIMPLE_DATE_FORMAT)
              .withParameters("java.lang.String", "com.ibm.icu.util.ULocale"));

  /** Matches methods that take a pattern as the first argument. */
  private static final Matcher<ExpressionTree> PATTERN_MATCHER =
      anyOf(
          instanceMethod().onExactClass(JAVA_SIMPLE_DATE_FORMAT).named("applyPattern"),
          instanceMethod().onExactClass(JAVA_SIMPLE_DATE_FORMAT).named("applyLocalizedPattern"),
          instanceMethod().onExactClass(ICU_SIMPLE_DATE_FORMAT).named("applyPattern"),
          instanceMethod().onExactClass(ICU_SIMPLE_DATE_FORMAT).named("applyLocalizedPattern"),
          staticMethod().onClass("java.time.format.DateTimeFormatter").named("ofPattern"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!PATTERN_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    return constructDescription(tree, tree.getArguments().get(0), state);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!PATTERN_CTOR_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    return constructDescription(tree, tree.getArguments().get(0), state);
  }

  /**
   * Matches patterns containing YYYY but not ww, signifying that it was not intended to be a week
   * date. If the pattern is a string literal, suggest replacing the YYYY with yyyy.
   *
   * <p>Given the {@link ExpressionTree} representing the pattern argument to the various methods in
   * SimpleDateFormat that accept a pattern, construct the description for this matcher to return.
   * May be {@link Description#NO_MATCH} if the pattern does not have a constant value, does not use
   * the week year format specifier, or is in proper week date format.
   */
  private Description constructDescription(
      Tree tree, ExpressionTree patternArg, VisitorState state) {
    String pattern = (String) ASTHelpers.constValue(patternArg);
    if (pattern != null && pattern.contains("Y") && !pattern.contains("w")) {
      Description.Builder description = buildDescription(tree);
      getFix(patternArg, state).ifPresent(description::addFix);
      return description.build();
    }

    return Description.NO_MATCH;
  }

  private Optional<SuggestedFix> getFix(ExpressionTree patternArg, VisitorState state) {
    if (patternArg.getKind() == Kind.STRING_LITERAL) {
      String replacement = state.getSourceForNode(patternArg).replace('Y', 'y');
      return Optional.of(SuggestedFix.replace(patternArg, replacement));
    }
    Symbol sym = ASTHelpers.getSymbol(patternArg);
    if (sym instanceof Symbol.VarSymbol && sym.getKind() == ElementKind.FIELD) {
      SuggestedFix[] fix = {null};
      new TreeScanner<Void, Void>() {
        @Override
        public Void visitVariable(VariableTree node, Void aVoid) {
          if (sym.equals(ASTHelpers.getSymbol(node))
              && node.getInitializer() != null
              && node.getInitializer().getKind() == Kind.STRING_LITERAL) {
            String source = state.getSourceForNode(node.getInitializer());
            String replacement = source.replace('Y', 'y');
            if (!source.equals(replacement)) {
              fix[0] = SuggestedFix.replace(node.getInitializer(), replacement);
            }
          }
          return super.visitVariable(node, aVoid);
        }
      }.scan(state.getPath().getCompilationUnit(), null);
      return Optional.ofNullable(fix[0]);
    }
    return Optional.empty();
  }
}
