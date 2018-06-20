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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.instanceMethod;

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
import com.sun.tools.javac.tree.JCTree;

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
    category = JDK,
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class MisusedWeekYear extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private static final String JAVA_SIMPLE_DATE_FORMAT = "java.text.SimpleDateFormat";
  private static final String ICU_SIMPLE_DATE_FORMAT = "com.ibm.icu.text.SimpleDateFormat";

  private static final Matcher<NewClassTree> simpleDateFormatConstructorMatcher =
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

  private static final Matcher<ExpressionTree> applyPatternMatcher =
      Matchers.<ExpressionTree>anyOf(
          instanceMethod().onExactClass(JAVA_SIMPLE_DATE_FORMAT).named("applyPattern"),
          instanceMethod().onExactClass(JAVA_SIMPLE_DATE_FORMAT).named("applyLocalizedPattern"),
          instanceMethod().onExactClass(ICU_SIMPLE_DATE_FORMAT).named("applyPattern"),
          instanceMethod().onExactClass(ICU_SIMPLE_DATE_FORMAT).named("applyLocalizedPattern"));

  /**
   * Match uses of SimpleDateFormat.applyPattern and SimpleDateFormat.applyLocalizedPattern in which
   * the pattern passed in contains YYYY but not ww, signifying that it was not intended to be a
   * week date. If the pattern is a string literal, suggest replacing the YYYY with yyyy. If the
   * pattern is a constant, don't give a suggested fix since the fix is nonlocal.
   */
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!applyPatternMatcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    return constructDescription(tree, tree.getArguments().get(0));
  }

  /**
   * Match uses of the SimpleDateFormat constructor in which the pattern passed in contains YYYY but
   * not ww, signifying that it was not intended to be a week date. If the pattern is a string
   * literal, suggest replacing the YYYY with yyyy. If the pattern is a constant, don't give a
   * suggested fix since the fix is nonlocal.
   */
  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!simpleDateFormatConstructorMatcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    return constructDescription(tree, tree.getArguments().get(0));
  }

  /**
   * Given the {@link ExpressionTree} representing the pattern argument to the various methods in
   * SimpleDateFormat that accept a pattern, construct the description for this matcher to return.
   * May be {@link Description#NO_MATCH} if the pattern does not have a constant value, does not use
   * the week year format specifier, or is in proper week date format.
   */
  private Description constructDescription(Tree tree, ExpressionTree patternArg) {
    String pattern = (String) ASTHelpers.constValue((JCTree) patternArg);
    if (pattern != null && pattern.contains("Y") && !pattern.contains("w")) {
      if (patternArg.getKind() == Kind.STRING_LITERAL) {
        String replacement = patternArg.toString().replace('Y', 'y');
        return describeMatch(tree, SuggestedFix.replace(patternArg, replacement));
      } else {
        return describeMatch(tree);
      }
    }

    return Description.NO_MATCH;
  }
}
