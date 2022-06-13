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

import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.sun.tools.javac.code.Type;

/** Enumerates types which have poorly-defined behaviour for equals. */
public enum TypesWithUndefinedEquality {
  // keep-sorted start
  CHAR_SEQUENCE("CharSequence", "java.lang.CharSequence"),
  COLLECTION("Collection", "java.util.Collection"),
  DATE("Date", "java.util.Date"),
  IMMUTABLE_COLLECTION("ImmutableCollection", "com.google.common.collect.ImmutableCollection"),
  IMMUTABLE_MULTIMAP("ImmutableMultimap", "com.google.common.collect.ImmutableMultimap"),
  ITERABLE("Iterable", "com.google.common.collect.FluentIterable", "java.lang.Iterable"),
  LONG_SPARSE_ARRAY(
      "LongSparseArray",
      "android.support.v4.util.LongSparseArrayCompat",
      "android.util.LongSparseArray",
      "androidx.collection.LongSparseArrayCompat",
      "androidx.core.util.LongSparseArrayCompat"),
  MULTIMAP("Multimap", "com.google.common.collect.Multimap"),
  QUEUE("Queue", "java.util.Queue"),
  SPARSE_ARRAY("SparseArray", "android.util.SparseArray", "androidx.collection.SparseArrayCompat");
  // keep-sorted end

  private final String shortName;
  private final ImmutableSet<String> typeNames;

  TypesWithUndefinedEquality(String shortName, String... typeNames) {
    this.shortName = shortName;
    this.typeNames = ImmutableSet.copyOf(typeNames);
  }

  public boolean matchesType(Type type, VisitorState state) {
    return typeNames.stream()
        .anyMatch(typeName -> isSameType(type, state.getTypeFromString(typeName), state));
  }

  public String shortName() {
    return shortName;
  }
}
