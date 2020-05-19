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
  LONG_SPARSE_ARRAY(
      "LongSparseArray",
      "android.util.LongSparseArray",
      "android.support.v4.util.LongSparseArrayCompat",
      "androidx.collection.LongSparseArrayCompat"),
  SPARSE_ARRAY(
      "SparseArray",
      "android.util.SparseArray",
      "android.support.v4.util.SparseArrayCompat",
      "androidx.collection.SparseArrayCompat"),
  MULTIMAP("Multimap", "com.google.common.collect.Multimap"),
  CHAR_SEQUENCE("CharSequence", "java.lang.CharSequence"),
  ITERABLE("Iterable", "java.lang.Iterable", "com.google.common.collect.FluentIterable"),
  COLLECTION("Collection", "java.util.Collection"),
  IMMUTABLE_COLLECTION("ImmutableCollection", "com.google.common.collect.ImmutableCollection"),
  QUEUE("Queue", "java.util.Queue"),
  DATE("Date", "java.util.Date");

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

  public ImmutableSet<String> typeNames() {
    return typeNames;
  }
}
