/*
 * Copyright 2026 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.matchingMethods;
import static java.lang.String.format;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.SideEffectAnalysis;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import java.util.List;

/**
 * Flags {@code Integer.parseInt(s.substring(begin, end))} and friends, which allocate a copy of the
 * region only to throw it away after parsing.
 */
@BugPattern(
    summary =
        "Parsing a substring allocates a copy of the region; the (CharSequence, int, int, int)"
            + " overload parses it in place",
    explanation =
        "`Integer.parseInt(String)` and its siblings force callers to materialize the region they"
            + " want to parse. Since JDK 9 each has a `(CharSequence, int, int, int)` overload that"
            + " reads the region directly out of the original sequence, so the copy is pure"
            + " overhead.\n\n"
            + "The rewrite preserves the parsed value in every case. It does change two things"
            + " about failures: an out-of-range index throws `IndexOutOfBoundsException` rather"
            + " than its subclass `StringIndexOutOfBoundsException`, and the"
            + " `NumberFormatException` message describes the region rather than the copy.\n\n"
            + "A null target keeps throwing `NullPointerException`, because the overload opens"
            + " with `Objects.requireNonNull`. That is specific to the parse overloads: the"
            + " analogous `Appendable.append(CharSequence, int, int)` substitutes the string"
            + " `\"null\"` and then applies the region to it, so extending this check to appends"
            + " would silently turn a null dereference into truncated output.",
    severity = WARNING,
    tags = StandardTags.PERFORMANCE)
public final class UnnecessarySubstring extends BugChecker implements MethodInvocationTreeMatcher {

  /**
   * Parses that have a {@code (CharSequence, int, int, int)} counterpart. {@code Integer.valueOf}
   * and {@code Double.parseDouble} deliberately absent: they have no such overload.
   */
  private static final Matcher<ExpressionTree> PARSE =
      staticMethod()
          .onClassAny("java.lang.Integer", "java.lang.Long")
          .namedAnyOf("parseInt", "parseUnsignedInt", "parseLong", "parseUnsignedLong");

  private static final Matcher<ExpressionTree> SUBSTRING =
      instanceMethod()
          .onExactClassAny("java.lang.String", "java.lang.StringBuilder", "java.lang.StringBuffer")
          .named("substring");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!PARSE.matches(tree, state)) {
      return NO_MATCH;
    }
    // The (String) and (String, int) overloads; the region-parsing overload takes four arguments.
    List<? extends ExpressionTree> args = tree.getArguments();
    if (args.size() != 1 && args.size() != 2) {
      return NO_MATCH;
    }
    if (!(args.get(0) instanceof MethodInvocationTree substring)
        || !SUBSTRING.matches(substring, state)) {
      return NO_MATCH;
    }
    if (!hasRegionParsingOverload(getSymbol(tree), state)) {
      return NO_MATCH;
    }
    ExpressionTree target = getReceiver(substring);
    if (target == null) {
      return NO_MATCH;
    }

    List<? extends ExpressionTree> bounds = substring.getArguments();
    String begin = state.getSourceForNode(bounds.get(0));
    String end;
    if (bounds.size() == 2) {
      end = state.getSourceForNode(bounds.get(1));
    } else {
      // substring(begin) runs to the end, which the region overload needs spelled out. That repeats
      // the target expression, so only rewrite when repeating it is free of consequence.
      if (!canRepeat(target)) {
        return NO_MATCH;
      }
      end = format("%s.length()", state.getSourceForNode(target));
    }
    String radix = args.size() == 2 ? state.getSourceForNode(args.get(1)) : "10";

    // Reuse the method select verbatim so static imports and qualified names survive the rewrite.
    return describeMatch(
        tree,
        SuggestedFix.replace(
            tree,
            format(
                "%s(%s, %s, %s, %s)",
                state.getSourceForNode(tree.getMethodSelect()),
                state.getSourceForNode(target),
                begin,
                end,
                radix)));
  }

  /**
   * Whether {@code target} can be written twice: it must be side-effect free, and it must bind
   * tightly enough that appending {@code .length()} still applies to the whole expression.
   */
  private static boolean canRepeat(ExpressionTree target) {
    return switch (target.getKind()) {
      case IDENTIFIER, STRING_LITERAL -> true;
      // `a.b.c` and `a[i]` qualify, but `a.b().c` and `a[f()]` do not.
      case MEMBER_SELECT, ARRAY_ACCESS -> !SideEffectAnalysis.hasSideEffect(target);
      default -> false;
    };
  }

  /**
   * Whether the class being parsed into declares the region-parsing overload. It arrived in JDK 9,
   * so this is absent when compiling against an older platform.
   */
  private static boolean hasRegionParsingOverload(MethodSymbol parse, VisitorState state) {
    Type charSequence = state.getTypeFromString("java.lang.CharSequence");
    if (charSequence == null) {
      return false;
    }
    return matchingMethods(
            parse.name,
            candidate ->
                candidate.isStatic()
                    && candidate.params().size() == 4
                    && isSameType(candidate.params().get(0).type, charSequence, state),
            parse.owner.type,
            state.getTypes())
        .findAny()
        .isPresent();
  }
}
