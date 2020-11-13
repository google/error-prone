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

import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.Optional;
import javax.lang.model.element.ElementKind;

/** Base class for checks which find common errors in date format patterns. */
public abstract class MisusedDateFormat extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private static final String JAVA_SIMPLE_DATE_FORMAT = "java.text.SimpleDateFormat";

  private static final String ICU_SIMPLE_DATE_FORMAT = "com.ibm.icu.text.SimpleDateFormat";

  private static final Matcher<NewClassTree> PATTERN_CTOR_MATCHER =
      anyOf(
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
      return NO_MATCH;
    }
    String argument = constValue(tree.getArguments().get(0), String.class);
    if (argument == null) {
      return NO_MATCH;
    }
    return constructDescription(tree, tree.getArguments().get(0), state);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!PATTERN_CTOR_MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    return constructDescription(tree, tree.getArguments().get(0), state);
  }

  /**
   * Override this method to provide a rewritten date format pattern from {@code pattern}. An empty
   * optional indicates that {@code pattern} does not need rewriting.
   */
  abstract Optional<String> rewriteTo(String pattern);

  private Description constructDescription(
      Tree tree, ExpressionTree patternArg, VisitorState state) {
    return Optional.ofNullable(constValue(patternArg, String.class))
        .flatMap(this::rewriteTo)
        .map(replacement -> describeMatch(tree, replaceArgument(patternArg, replacement, state)))
        .orElse(NO_MATCH);
  }

  private static SuggestedFix replaceArgument(
      ExpressionTree patternArg, String replacement, VisitorState state) {
    String quotedReplacement = String.format("\"%s\"", replacement);
    if (patternArg.getKind() == Kind.STRING_LITERAL) {
      return SuggestedFix.replace(patternArg, quotedReplacement);
    }
    Symbol sym = getSymbol(patternArg);
    if (!(sym instanceof VarSymbol) || sym.getKind() != ElementKind.FIELD) {
      return SuggestedFix.emptyFix();
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitVariable(VariableTree node, Void unused) {
        if (sym.equals(getSymbol(node))
            && node.getInitializer() != null
            && node.getInitializer().getKind() == Kind.STRING_LITERAL) {
          fix.replace(node.getInitializer(), quotedReplacement);
          return null;
        }
        return super.visitVariable(node, null);
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return fix.build();
  }

  static String replaceFormatChar(String format, char from, char to) {
    StringBuilder builder = new StringBuilder();
    parseDateFormat(
        format,
        new DateFormatConsumer() {
          @Override
          public void consumeLiteral(char literal) {
            builder.append(literal);
          }

          @Override
          public void consumeSpecial(char special) {
            builder.append(special == from ? to : special);
          }
        });
    return builder.toString();
  }

  static void parseDateFormat(String format, DateFormatConsumer consumer) {
    for (int pos = 0; pos < format.length(); ++pos) {
      char c = format.charAt(pos);
      if (c == '\'') {
        consumer.consumeSpecial('\'');
        pos++;
        for (; pos < format.length(); ++pos) {
          consumer.consumeLiteral(format.charAt(pos));
          if (format.charAt(pos) == '\'') {
            if (pos + 1 < format.length() && format.charAt(pos + 1) == '\'') {
              consumer.consumeSpecial('\'');
              pos++; // Increment another to skip the escaped '
            } else {
              break;
            }
          }
        }
      } else {
        consumer.consumeSpecial(c);
      }
    }
  }

  interface DateFormatConsumer {
    /** Consumes a literal (escaped) character. */
    void consumeLiteral(char literal);

    /** Consumes a special (format) character. */
    void consumeSpecial(char special);
  }
}
