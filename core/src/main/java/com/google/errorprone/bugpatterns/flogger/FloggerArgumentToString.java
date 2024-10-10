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

package com.google.errorprone.bugpatterns.flogger;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.flogger.FloggerHelpers.inferFormatSpecifier;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getType;
import static java.util.Arrays.stream;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.lang.model.type.TypeKind;
import org.jspecify.annotations.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Use Flogger's printf-style formatting instead of explicitly converting arguments to"
            + " strings",
    severity = WARNING)
public class FloggerArgumentToString extends BugChecker implements MethodInvocationTreeMatcher {

  /**
   * Match any unescaped '%' and capture probable terms (including the '%'). Note that this
   * expression also captures "%n" (newline) which we convert in-place to an escaped "\n".
   *
   * <p>Printf terms do not currently support specifying an index (e.g. {@code "%1$d"}) as this is
   * used incredibly rarely and not worth automating.
   */
  private static final Pattern PRINTF_TERM_CAPTURE_PATTERN =
      // TODO(amalloy): I think this can be done without possessive quantifiers:
      // (?:^|[^%])(?:%%)*(%[^%a-zA-Z]*[a-zA-Z])
      // I think this also means we no longer need the special-case "at current position" check,
      // since skipping % characters can't cause it to match.
      Pattern.compile(
          // Skip escaped pairs of '%' before the next term.
          "[^%]*+(?:%%[^%]*+)*+"
              // Capture an unescaped '%' and anything up to the first letter.
              + "(%[^%a-zA-Z]*[a-zA-Z])");

  /**
   * Validate captured printf terms (minus the leading '%'). This expression MUST NOT risk having
   * false positives and anything we match (other than "%n") must be 100% legal in Flogger.
   */
  private static final Pattern PRINTF_TERM_VALIDATION_PATTERN =
      Pattern.compile(
          // Simple: %c, %b and %n (newline)
          "[cCbBn]|"
              // String: %s, %20s, %S, %#s
              + "#?(?:[1-9][0-9]*)?[sS]|"
              // Integral: %d, %12d, %,d (numeric grouping)
              + ",?(?:[1-9][0-9]*)?d|"
              // Zero padded Integral: %05d (not allowed with grouping)
              + "0[1-9][0-9]*d|"
              // Hex: %x, %#x, %#016X (%0x is not legal)
              + "#?(?:0[1-9][0-9]*)?[xX]|"
              // Float: %f, %,f, %8e, %12.6G
              + ",?(?:[1-9][0-9]*)?(?:\\.[0-9]+)?[feEgG]");

  private static final Character STRING_FORMAT = 's';
  private static final Character UPPER_STRING_FORMAT = 'S';
  private static final Character HEX_FORMAT = 'x';
  private static final Character UPPER_HEX_FORMAT = 'X';

  static class Parameter {

    final Supplier<String> source;
    final Type type;
    final @Nullable Character placeholder;

    private Parameter(ExpressionTree expression, char placeholder) {
      this(s -> s.getSourceForNode(expression), getType(expression), placeholder);
    }

    private Parameter(Supplier<String> source, Type type, char placeholder) {
      this.source = source;
      this.type = type;
      this.placeholder = placeholder;
    }

    private static Parameter receiver(MethodInvocationTree invocation, char placeholder) {
      ExpressionTree receiver = getReceiver(invocation);
      if (receiver != null) {
        return new Parameter(getReceiver(invocation), placeholder);
      }
      return new Parameter(s -> "this", null, placeholder);
    }
  }

  private enum Unwrapper {
    // Unwrap any instance call to toString().
    TO_STRING(instanceMethod().anyClass().named("toString").withNoParameters()) {

      @Override
      Parameter unwrap(MethodInvocationTree invocation, char placeholder) {
        return Parameter.receiver(invocation, placeholder);
      }
    },
    // Unwrap things like: String.valueOf(x) --> x
    // Consider carefully if it's worth doing the char[] variant (Will we format char[] exactly
    // as the corresponding String? How often is it used?)
    STRING_VALUE_OF(
        anyOf(
            Stream.of(
                    "valueOf(boolean)",
                    "valueOf(char)",
                    "valueOf(int)",
                    "valueOf(long)",
                    "valueOf(float)",
                    "valueOf(double)",
                    "valueOf(java.lang.Object)")
                .map(
                    signature ->
                        instanceMethod().onExactClass("java.lang.String").withSignature(signature))
                .collect(toImmutableList()))) {

      @Override
      Parameter unwrap(MethodInvocationTree invocation, char placeholder) {
        return Parameter.receiver(invocation, placeholder);
      }
    },
    // Unwrap things like: Integer.toString(n) --> n
    STATIC_TO_STRING(
        anyOf(
            ImmutableMap.<Class<?>, String>builder()
                .put(Boolean.class, "toString(boolean)")
                .put(Character.class, "toString(char)")
                .put(Byte.class, "toString(byte)")
                .put(Short.class, "toString(short)")
                .put(Integer.class, "toString(int)")
                .put(Long.class, "toString(long)")
                .put(Float.class, "toString(float)")
                .put(Double.class, "toString(double)")
                .buildOrThrow()
                .entrySet()
                .stream()
                .map(e -> staticMethod().onClass(e.getKey().getName()).withSignature(e.getValue()))
                .collect(toImmutableList()))) {

      @Override
      Parameter unwrap(MethodInvocationTree invocation, char placeholder) {
        return new Parameter(getOnlyArgument(invocation), placeholder);
      }
    },
    // Unwrap any inline manual auto-boxing (good for cases where flogger does not auto-box).
    // Note that we could also unwrap unboxing, but this has the effect of removing a null check.
    STATIC_VALUE_OF(
        anyOf(
            ImmutableMap.<Class<?>, String>builder()
                .put(Boolean.class, "valueOf(boolean)")
                .put(Character.class, "valueOf(char)")
                .put(Byte.class, "valueOf(byte)")
                .put(Short.class, "valueOf(short)")
                .put(Integer.class, "valueOf(int)")
                .put(Long.class, "valueOf(long)")
                .put(Float.class, "valueOf(float)")
                .put(Double.class, "valueOf(double)")
                .buildOrThrow()
                .entrySet()
                .stream()
                .map(e -> staticMethod().onClass(e.getKey().getName()).withSignature(e.getValue()))
                .collect(toImmutableList()))) {

      @Override
      Parameter unwrap(MethodInvocationTree invocation, char placeholder) {
        return new Parameter(getOnlyArgument(invocation), placeholder);
      }
    },
    // Check that the output format safe to unwrap (it could be log("%b", x.toString()) which
    // cannot be unwrapped).
    STRING_TO_UPPER_CASE(
        instanceMethod().onExactClass("java.lang.String").named("toUpperCase").withNoParameters()) {

      @Override
      Parameter unwrap(MethodInvocationTree invocation, char placeholder) {
        return new Parameter(getReceiver(invocation), UPPER_STRING_FORMAT);
      }
    },
    ASCII_TO_UPPER_CASE(
        anyOf(
            Stream.of("java.lang.String", "java.lang.CharSequence")
                .map(
                    param ->
                        staticMethod()
                            .onClass("com.google.common.base.Ascii")
                            .named("toUpperCase")
                            .withParameters(param))
                .collect(toImmutableList()))) {

      @Override
      Parameter unwrap(MethodInvocationTree invocation, char placeholder) {
        return new Parameter(getOnlyArgument(invocation), UPPER_STRING_FORMAT);
      }
    },
    STATIC_TO_HEX_STRING(
        anyOf(
            ImmutableMap.<Class<?>, String>builder()
                .put(Integer.class, "toHexString(int)")
                .put(Long.class, "toHexString(long)")
                .buildOrThrow()
                .entrySet()
                .stream()
                .map(e -> staticMethod().onClass(e.getKey().getName()).withSignature(e.getValue()))
                .collect(toImmutableList()))) {

      @Override
      Parameter unwrap(MethodInvocationTree invocation, char placeholder) {
        return new Parameter(
            getOnlyArgument(invocation),
            Ascii.isUpperCase(placeholder) ? UPPER_HEX_FORMAT : HEX_FORMAT);
      }
    },
    ARRAYS_AS_LIST_OR_TO_STRING(
        allOf(
            staticMethod().onClass("java.util.Arrays").namedAnyOf("asList", "toString"),
            toType(
                MethodInvocationTree.class,
                FloggerArgumentToString::hasSingleVarargsCompatibleArgument))) {

      @Override
      Parameter unwrap(MethodInvocationTree invocation, char placeholder) {
        return new Parameter(getOnlyArgument(invocation), placeholder);
      }
    };

    abstract Parameter unwrap(MethodInvocationTree tree, char placeholder);

    @SuppressWarnings("Immutable") // all implementations are immutable
    private final Matcher<ExpressionTree> matcher;

    Unwrapper(Matcher<ExpressionTree> matcher) {
      this.matcher = matcher;
    }
  }

  private static ExpressionTree getOnlyArgument(MethodInvocationTree invocation) {
    return getOnlyElement(invocation.getArguments());
  }

  private static final Matcher<ExpressionTree> LOG_MATCHER =
      instanceMethod().onDescendantOf("com.google.common.flogger.LoggingApi").named("log");

  static final Matcher<ExpressionTree> UNWRAPPABLE =
      anyOf(stream(Unwrapper.values()).map(u -> u.matcher).collect(toImmutableList()));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!LOG_MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    List<? extends ExpressionTree> arguments = tree.getArguments();
    if (arguments.isEmpty()) {
      return NO_MATCH;
    }
    String formatString = ASTHelpers.constValue(arguments.get(0), String.class);
    if (formatString == null) {
      return NO_MATCH;
    }
    arguments = arguments.subList(1, arguments.size());
    if (arguments.stream().noneMatch(argument -> UNWRAPPABLE.matches(argument, state))) {
      return NO_MATCH;
    }
    return unwrapArguments(formatString, tree, arguments, state);
  }

  Description unwrapArguments(
      String formatString,
      MethodInvocationTree tree,
      List<? extends ExpressionTree> arguments,
      VisitorState state) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    int start = 0;
    java.util.regex.Matcher matcher = PRINTF_TERM_CAPTURE_PATTERN.matcher(formatString);
    StringBuilder sb = new StringBuilder();
    int idx = 0;
    boolean fixed = false;
    // NOTE: Not only must we find() a next term, the match must start at our current position
    // otherwise we can unexpectedly match things like "%%s" (by skipping the first '%').
    while (matcher.find() && matcher.start() == start) {
      String term = matcher.group(1);
      // Validate the term (minus the leading '%').
      if (!PRINTF_TERM_VALIDATION_PATTERN.matcher(term.substring(1)).matches()) {
        return NO_MATCH;
      }
      if (term.equals("%n")) {
        // Replace "%n" with "\n" everywhere (Flogger does not recognize %n and it's
        // potentially confusing if people mistake it for a placeholder).
        term = "\\n";
      } else {
        // Only unwrap existing printf parameters if the placeholder has no additional formatting.
        if (term.length() == 2) {
          ExpressionTree argument = arguments.get(idx);
          char placeholder = term.charAt(1);
          Parameter unwrapped = unwrap(argument, placeholder, state);
          if (unwrapped != null) {
            fix.replace(argument, unwrapped.source.get(state));
            placeholder = firstNonNull(unwrapped.placeholder, 's');
            if (placeholder == STRING_FORMAT) {
              placeholder = inferFormatSpecifier(unwrapped.type, state);
            }
            term = "%" + placeholder;
            fixed = true;
          }
        }
        idx++;
      }
      sb.append(formatString, start, matcher.start(1)).append(term);
      start = matcher.end(1);
    }
    sb.append(formatString, start, formatString.length());
    if (!fixed) {
      return NO_MATCH;
    }
    fix.replace(tree.getArguments().get(0), state.getConstantExpression(sb));
    return describeMatch(tree, fix.build());
  }

  private static @Nullable Parameter unwrap(
      ExpressionTree argument, char placeholder, VisitorState state) {
    for (Unwrapper unwrapper : Unwrapper.values()) {
      if (unwrapper.matcher.matches(argument, state)) {
        return unwrapper.unwrap((MethodInvocationTree) argument, placeholder);
      }
    }
    return null;
  }

  private static boolean hasSingleVarargsCompatibleArgument(
      MethodInvocationTree tree, VisitorState state) {
    if (tree.getArguments().size() != 1) {
      return false;
    }
    Type type = getType(getOnlyArgument(tree));
    if (type == null) {
      return false;
    }
    if (!type.getKind().equals(TypeKind.ARRAY)) {
      return false;
    }
    return ASTHelpers.isSameType(
        ((ArrayType) type).getComponentType(), state.getSymtab().objectType, state);
  }
}
