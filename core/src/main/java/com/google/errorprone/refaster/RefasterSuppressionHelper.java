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

package com.google.errorprone.refaster;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.SuppressionInfo;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Suppressible;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import java.lang.annotation.Annotation;
import java.util.Set;

/** Helpers for handling suppression annotations in refaster. */
final class RefasterSuppressionHelper {

  /**
   * Returns true if the given rule is suppressed on the given tree.
   *
   * <p>Unlike Error Prone, which scans the compilation unit once for all checks, refaster scans the
   * compilation unit separately for each check. This simplifies suppression handling, since we can
   * just stop recursively scanning as soon as a suppression is found for the current refaster rule.
   */
  static boolean suppressed(RefasterRule<?, ?> rule, Tree tree, Context context) {
    VisitorState state = VisitorState.createForUtilityPurposes(context);
    Symbol sym = ASTHelpers.getDeclaredSymbol(tree);
    if (sym == null) {
      return false;
    }
    return SuppressionInfo.EMPTY
        .withExtendedSuppressions(
            sym, state, /* customSuppressionAnnosToLookFor= */ ImmutableSet.of())
        .suppressedState(
            new RefasterSuppressible(rule), /* suppressedInGeneratedCode= */ false, state)
        .equals(SuppressionInfo.SuppressedState.SUPPRESSED);
  }

  /** Adapts a {@link RefasterRule<?, ?>} into a {@link Suppressible}. */
  private static class RefasterSuppressible implements Suppressible {

    private final RefasterRule<?, ?> rule;

    RefasterSuppressible(RefasterRule<?, ?> rule) {
      this.rule = rule;
    }

    @Override
    public Set<String> allNames() {
      return ImmutableSet.of(canonicalName());
    }

    @Override
    public String canonicalName() {
      return rule.simpleTemplateName();
    }

    @Override
    public boolean supportsSuppressWarnings() {
      return true;
    }

    @Override
    public Set<Class<? extends Annotation>> customSuppressionAnnotations() {
      return ImmutableSet.of();
    }

    @Override
    public boolean suppressedByAnyOf(Set<Name> annotations, VisitorState s) {
      // no custom suppression annotations
      return false;
    }
  }

  private RefasterSuppressionHelper() {}
}
