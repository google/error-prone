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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.tools.javac.code.Type;
import java.util.Optional;
import javax.inject.Inject;

/// Flags hash-based containers keyed on types that are configured as unsuitable hash keys.
@BugPattern(
    summary =
        "This type should not be used as an element of a hash-based Set, or a key of a"
            + " hash-based Map, Multimap, or Multiset",
    severity = WARNING)
public final class DiscouragedHashKey extends AbstractEqualsHashCodeUsageChecker {

  private static final ImmutableSet<String> DEFAULT_TYPES = ImmutableSet.of();

  private final ImmutableList<Supplier<Type>> types;

  @Inject
  DiscouragedHashKey(ErrorProneFlags flags) {
    this.types =
        Sets.union(DEFAULT_TYPES, flags.getSetOrEmpty("DiscouragedHashKey:Types")).stream()
            .map(Suppliers::typeFromString)
            .collect(toImmutableList());
  }

  @Override
  protected Optional<String> problem(Type type, Usage usage, VisitorState state) {
    if (usage != Usage.HASH_KEY) {
      return Optional.empty();
    }
    for (Supplier<Type> supplier : types) {
      Type discouraged = supplier.get(state);
      if (discouraged != null && isSubtype(type, discouraged, state)) {
        return Optional.of(
            String.format(
                "%s should not be used as an element of a hash-based Set, or a key of a"
                    + " hash-based Map, Multimap, or Multiset; consider keying on a stable"
                    + " identifier instead",
                type.tsym.getSimpleName()));
      }
    }
    return Optional.empty();
  }
}
