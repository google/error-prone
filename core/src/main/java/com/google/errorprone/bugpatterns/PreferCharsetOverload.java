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
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.findMatchingMethods;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static java.util.Comparator.comparingInt;

import com.google.common.base.Ascii;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.argumentselectiondefects.NamedParameterComment;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Prefer calling overloads that accept a Charset over those that accept a String"
            + " encoding name.",
    severity = WARNING)
public final class PreferCharsetOverload extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  // Explicitly look for common charset parameter name tokens.
  private static final ImmutableSet<String> TARGET_PARAM_NAMES =
      ImmutableSet.of("charset", "cs", "csn", "enc", "encoding");

  // Matches Charset decomposition calls like charset.name(), charset.displayName().
  private static final Matcher<ExpressionTree> CHARSET_DECOMPOSITION_MATCHER =
      instanceMethod()
          .onDescendantOf("java.nio.charset.Charset")
          .namedAnyOf("name", "displayName", "toString");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return match(tree, tree.getArguments(), getSymbol(tree), state);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    return match(tree, tree.getArguments(), getSymbol(tree), state);
  }

  private Description match(
      ExpressionTree tree,
      List<? extends ExpressionTree> arguments,
      @Nullable MethodSymbol methodSymbol,
      VisitorState state) {
    if (methodSymbol == null) {
      return Description.NO_MATCH;
    }

    // 1. Find indices of String parameters that are potential charset names.
    ImmutableList<Integer> candidateIndices = findCharsetCandidates(methodSymbol, state);
    if (candidateIndices.isEmpty()) {
      return Description.NO_MATCH;
    }

    // 2. Find the best overload that accepts Charset.
    MethodSymbol bestOverload = findBestCharsetOverload(methodSymbol, candidateIndices, state);
    if (bestOverload == null) {
      return Description.NO_MATCH;
    }

    // 3. Build the fix to use the Charset overload.
    SuggestedFix fix = buildFix(arguments, bestOverload, candidateIndices, state);
    return fix != null ? describeMatch(tree, fix) : Description.NO_MATCH;
  }

  /**
   * Identifies indices of String parameters in the given method that are likely to represent a
   * charset or encoding.
   */
  private static ImmutableList<Integer> findCharsetCandidates(
      MethodSymbol methodSymbol, VisitorState state) {
    // Skip methods/constructors with synthetic parameter names (e.g. inner-class constructor
    // implicit parameters like this$0, or when real parameter names are not available).
    if (NamedParameterComment.containsSyntheticParameterName(methodSymbol)) {
      return ImmutableList.of();
    }

    ImmutableList.Builder<Integer> candidateIndicesBuilder = ImmutableList.builder();
    List<VarSymbol> parameters = methodSymbol.getParameters();
    // Find indices of String parameters with charset-like word tokens.
    for (int i = 0; i < parameters.size(); i++) {
      VarSymbol param = parameters.get(i);
      if (isSameType(param.asType(), state.getSymtab().stringType, state)) {
        String paramName = param.getSimpleName().toString();
        if (tokenize(paramName).anyMatch(TARGET_PARAM_NAMES::contains)) {
          candidateIndicesBuilder.add(i);
        }
      }
    }
    return candidateIndicesBuilder.build();
  }

  /**
   * Generates a {@link SuggestedFix} to call the {@code bestOverload} instead of the original
   * method, by converting the string arguments at {@code candidateIndices} to Charset types.
   * Returns null if no fix can be generated.
   */
  private static @Nullable SuggestedFix buildFix(
      List<? extends ExpressionTree> arguments,
      MethodSymbol bestOverload,
      ImmutableList<Integer> candidateIndices,
      VisitorState state) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    List<VarSymbol> bestParams = bestOverload.getParameters();
    boolean fixedAnyParam = false;
    Type charsetType = CHARSET_TYPE.get(state);
    for (int i : candidateIndices) {
      if (i >= arguments.size()) {
        continue; // Skip if argument is not provided (e.g., varargs).
      }
      if (isSameType(bestParams.get(i).asType(), charsetType, state)) {
        if (!suggestParamFix(arguments.get(i), state, fix)) {
          return null;
        }
        fixedAnyParam = true;
      }
    }
    return fixedAnyParam ? fix.build() : null;
  }

  private static Stream<String> tokenize(String s) {
    return Arrays.stream(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, s).split("_"));
  }

  private static @Nullable MethodSymbol findBestCharsetOverload(
      MethodSymbol methodSymbol, ImmutableList<Integer> candidateIndices, VisitorState state) {
    Type startClass = enclosingClass(methodSymbol).asType();

    // Find all potential overloads across the class hierarchy.
    ImmutableSet<MethodSymbol> matchingMethods =
        findMatchingMethods(
            methodSymbol.name,
            candidate -> isValidCharsetOverload(candidate, methodSymbol, candidateIndices, state),
            startClass,
            state.getTypes());

    // If multiple overloads match, pick the one that replaces the most parameters.
    return matchingMethods.stream()
        .max(comparingInt(m -> countCharsetParams(m, candidateIndices, state)))
        .orElse(null);
  }

  /**
   * Checks if the given {@code candidate} method is a valid {@code Charset}-based overload of the
   * given {@code methodSymbol}.
   */
  private static boolean isValidCharsetOverload(
      MethodSymbol candidate,
      MethodSymbol methodSymbol,
      ImmutableList<Integer> candidateIndices,
      VisitorState state) {
    if (candidate.equals(methodSymbol)) {
      return false;
    }
    if (methodSymbol.isConstructor() && !candidate.owner.equals(methodSymbol.owner)) {
      return false;
    }
    if (candidate.isStatic() != methodSymbol.isStatic()) {
      return false;
    }
    if (candidate.isVarArgs() != methodSymbol.isVarArgs()) {
      return false;
    }
    List<VarSymbol> parameters = methodSymbol.getParameters();
    List<VarSymbol> candidateParams = candidate.getParameters();
    if (parameters.size() != candidateParams.size()) {
      return false;
    }

    Type charsetType = CHARSET_TYPE.get(state);
    boolean hasCharsetImprovement = false;
    // Check if candidate parameters can be replaced by Charset while keeping
    // non-candidate parameters exactly the same.
    for (int i = 0; i < parameters.size(); i++) {
      Type expectedType = parameters.get(i).asType();
      Type candidateParamType = candidateParams.get(i).asType();

      if (candidateIndices.contains(i)) {
        if (isSameType(candidateParamType, charsetType, state)) {
          hasCharsetImprovement = true;
          continue;
        }
        if (isSameType(candidateParamType, state.getSymtab().stringType, state)) {
          continue;
        }
        return false; // Neither String nor Charset
      } else {
        if (!isSameType(candidateParamType, expectedType, state)) {
          return false;
        }
      }
    }
    return hasCharsetImprovement
        && isSameType(candidate.getReturnType(), methodSymbol.getReturnType(), state);
  }

  /** Counts the number of candidate parameters that are Charset types in the given method. */
  private static int countCharsetParams(
      MethodSymbol method, ImmutableList<Integer> indices, VisitorState state) {
    int count = 0;
    List<VarSymbol> params = method.getParameters();
    Type charsetType = CHARSET_TYPE.get(state);
    for (int i : indices) {
      if (isSameType(params.get(i).asType(), charsetType, state)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Suggests a replacement strategy for a String argument to convert it to a Charset.
   *
   * <p>The replacement strategy is as follows:
   *
   * <ol>
   *   <li>static import {@code StandardCharsets} constants (e.g., {@code UTF_8}, {@code
   *       ISO_8859_1}, etc.)
   *   <li>unwrap decomposed {@code Charset} calls (e.g., {@code charset.name()}, {@code
   *       charset.displayName()}, etc.)
   *   <li>fallback to {@code Charset.forName()}
   * </ol>
   *
   * @return true if a replacement was successfully added to the {@code fix} builder, or false if no
   *     fix could be determined (e.g., for null literals).
   */
  private static boolean suggestParamFix(
      ExpressionTree stringExpr, VisitorState state, SuggestedFix.Builder fix) {
    if (stringExpr.getKind() == Kind.NULL_LITERAL) {
      return false;
    }

    String constantName = getStandardCharsetConstant(constValue(stringExpr, String.class));
    if (constantName != null) {
      fix.addStaticImport("java.nio.charset.StandardCharsets." + constantName);
      fix.replace(stringExpr, constantName);
      return true;
    }

    if (CHARSET_DECOMPOSITION_MATCHER.matches(stringExpr, state)) {
      fix.replace(stringExpr, state.getSourceForNode(getReceiver(stringExpr)));
      return true;
    }

    String charset = SuggestedFixes.qualifyType(state, fix, "java.nio.charset.Charset");
    fix.replace(stringExpr, charset + ".forName(" + state.getSourceForNode(stringExpr) + ")");
    return true;
  }

  /**
   * Returns the name of the {@link java.nio.charset.StandardCharsets} constant if the given charset
   * name matches a known standard charset (otherwise returns {@code null}).
   */
  private static @Nullable String getStandardCharsetConstant(@Nullable String charsetName) {
    if (charsetName == null) {
      return null;
    }

    // Normalize the charset name to handle various common formats like "UTF-8", "UTF8", and
    // "utf_8" consistently.
    String normalized = Ascii.toUpperCase(charsetName).replace("-", "").replace("_", "");
    return switch (normalized) {
      case "UTF8" -> "UTF_8";
      case "ISO88591" -> "ISO_8859_1";
      case "USASCII", "ASCII" -> "US_ASCII";
      case "UTF16" -> "UTF_16";
      case "UTF16BE" -> "UTF_16BE";
      case "UTF16LE" -> "UTF_16LE";
      default -> null;
    };
  }

  private static final Supplier<Type> CHARSET_TYPE = typeFromString("java.nio.charset.Charset");
}
