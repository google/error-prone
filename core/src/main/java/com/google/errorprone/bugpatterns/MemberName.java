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

import static com.google.common.base.Ascii.isUpperCase;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.renameMethodWithInvocations;
import static com.google.errorprone.fixes.SuggestedFixes.renameVariable;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.JUnitMatchers.TEST_CASE;
import static com.google.errorprone.util.ASTHelpers.annotationsAmong;
import static com.google.errorprone.util.ASTHelpers.findSuperMethods;
import static com.google.errorprone.util.ASTHelpers.getGeneratedBy;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.outermostClass;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.util.Name;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/** Flags a few ways in which member names may violate the style guide. */
@BugPattern(
    name = "MemberName",
    severity = WARNING,
    summary = "Methods and non-static variables should be named in lowerCamelCase.",
    linkType = CUSTOM,
    link = "https://google.github.io/styleguide/javaguide.html#s5.2-specific-identifier-names")
public final class MemberName extends BugChecker implements MethodTreeMatcher, VariableTreeMatcher {
  private static final Supplier<ImmutableSet<Name>> EXEMPTED_CLASS_ANNOTATIONS =
      VisitorState.memoize(
          s ->
              Stream.of(
                      "org.robolectric.annotation.Implements")
                  .map(s::getName)
                  .collect(toImmutableSet()));

  private static final Supplier<ImmutableSet<Name>> EXEMPTED_METHOD_ANNOTATIONS =
      VisitorState.memoize(
          s ->
              Stream.of("com.pholser.junit.quickcheck.Property")
                  .map(s::getName)
                  .collect(toImmutableSet()));

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol symbol = getSymbol(tree);
    if (symbol == null) {
      return NO_MATCH;
    }
    if (!annotationsAmong(symbol.owner, EXEMPTED_CLASS_ANNOTATIONS.get(state), state).isEmpty()) {
      return NO_MATCH;
    }
    if (!annotationsAmong(symbol, EXEMPTED_METHOD_ANNOTATIONS.get(state), state).isEmpty()) {
      return NO_MATCH;
    }
    if (hasTestAnnotation(symbol)
        || findSuperMethods(symbol, state.getTypes()).stream()
            .anyMatch(s -> hasTestAnnotation(s))) {
      return NO_MATCH;
    }
    // It is a surprisingly common error to replace @Test with @Ignore to ignore a test.
    if (hasAnnotation(symbol, "org.junit.Ignore", state)) {
      return NO_MATCH;
    }
    if (tree.getModifiers().getFlags().contains(Modifier.NATIVE)) {
      return NO_MATCH;
    }
    // JUnitParams reflectively accesses methods starting with "parametersFor" to provide parameters
    // for tests (which may then contain underscores).
    if (symbol.getSimpleName().toString().startsWith("parametersFor")) {
      return NO_MATCH;
    }
    if (findSuperMethods(symbol, state.getTypes()).stream()
        .anyMatch(m -> !getGeneratedBy(outermostClass(m), state).isEmpty())) {
      return NO_MATCH;
    }
    if (TEST_CASE.matches(tree, state)) {
      return NO_MATCH;
    }
    String name = tree.getName().toString();
    if (isConformant(name)) {
      return NO_MATCH;
    }
    String suggested = suggestedRename(name);
    return suggested.equals(name) || !symbol.isPrivate()
        ? describeMatch(tree)
        : describeMatch(tree, renameMethodWithInvocations(tree, suggested, state));
  }

  private static boolean hasTestAnnotation(MethodSymbol symbol) {
    return symbol.getRawAttributes().stream()
        .anyMatch(c -> c.type.tsym.getSimpleName().toString().contains("Test"));
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    Symbol symbol = getSymbol(tree);
    if (symbol == null) {
      return NO_MATCH;
    }
    // TODO(ghm): We could validate static variables too; they can contain underscores, but should
    // be either UPPER_UNDERSCORE or lowerCamel, not some mix of both.
    if (symbol.isStatic()) {
      return NO_MATCH;
    }
    String name = tree.getName().toString();
    // Try to avoid dual-matching with ConstantCaseForConstants.
    if (UPPER_UNDERSCORE_PATTERN.matcher(name).matches() && !symbol.isStatic()) {
      return NO_MATCH;
    }
    if (isConformant(name)) {
      return NO_MATCH;
    }
    String suggested = suggestedRename(name);
    return suggested.equals(name) || !canBeRenamed(symbol)
        ? describeMatch(tree)
        : describeMatch(tree, renameVariable(tree, suggested, state));
  }

  private static String suggestedRename(String name) {
    if (LOWER_UNDERSCORE_PATTERN.matcher(name).matches()) {
      return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
    }
    if (UPPER_UNDERSCORE_PATTERN.matcher(name).matches()) {
      return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
    }
    return name.replace("_", "");
  }

  private static final Pattern LOWER_UNDERSCORE_PATTERN = Pattern.compile("[a-z0-9_]+");
  private static final Pattern UPPER_UNDERSCORE_PATTERN = Pattern.compile("[A-Z0-9_]+");

  private static boolean canBeRenamed(Symbol symbol) {
    return symbol.isPrivate() || symbol.getKind().equals(ElementKind.LOCAL_VARIABLE);
  }

  private static boolean isConformant(String name) {
    return !name.contains("_") && !isUpperCase(name.charAt(0));
  }
}
