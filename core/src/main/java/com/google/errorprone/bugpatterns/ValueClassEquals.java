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
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.overridesObjectEquals;
import static com.google.errorprone.util.ASTHelpers.overridesObjectHashCode;

import com.google.common.base.Enums;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import org.jspecify.annotations.Nullable;

/**
 * Checks that non-record concrete {@code value} classes explicitly override {@link Object#equals}
 * and {@link Object#hashCode}.
 */
@BugPattern(
    summary =
        """
        Non-record concrete value classes should explicitly override equals and hashCode; prefer \
        a value record, or delegate to super.equals and super.hashCode if state-wise identity \
        comparison is intended\
        """,
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE)
public final class ValueClassEquals extends BugChecker implements ClassTreeMatcher {

  private static final @Nullable Modifier VALUE_MODIFIER =
      Enums.getIfPresent(Modifier.class, "VALUE").orNull();

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ClassSymbol symbol = getSymbol(tree);
    if (symbol.getKind() != ElementKind.CLASS) {
      return NO_MATCH;
    }
    if (VALUE_MODIFIER == null
        || !symbol.getModifiers().contains(VALUE_MODIFIER)
        || symbol.getModifiers().contains(Modifier.ABSTRACT)) {
      return NO_MATCH;
    }
    if (overridesObjectEquals(symbol, state) && overridesObjectHashCode(symbol, state)) {
      return NO_MATCH;
    }
    return describeMatch(tree);
  }
}
