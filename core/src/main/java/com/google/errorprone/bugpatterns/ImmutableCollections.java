/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.common.collect.ImmutableBiMap;
import com.sun.tools.javac.code.Type;
import java.util.Optional;

/** Common utility functions for immutable collections. */
public final class ImmutableCollections {

  private ImmutableCollections() {}

  private static final ImmutableBiMap<String, String> MUTABLE_TO_IMMUTABLE_CLASS_NAME_MAP =
      ImmutableBiMap.<String, String>builder()
          .put(
              com.google.common.collect.BiMap.class.getName(),
              com.google.common.collect.ImmutableBiMap.class.getName())
          .put(
              com.google.common.collect.ListMultimap.class.getName(),
              com.google.common.collect.ImmutableListMultimap.class.getName())
          .put(
              com.google.common.collect.Multimap.class.getName(),
              com.google.common.collect.ImmutableMultimap.class.getName())
          .put(
              com.google.common.collect.Multiset.class.getName(),
              com.google.common.collect.ImmutableMultiset.class.getName())
          .put(
              com.google.common.collect.RangeMap.class.getName(),
              com.google.common.collect.ImmutableRangeMap.class.getName())
          .put(
              com.google.common.collect.RangeSet.class.getName(),
              com.google.common.collect.ImmutableRangeSet.class.getName())
          .put(
              com.google.common.collect.SetMultimap.class.getName(),
              com.google.common.collect.ImmutableSetMultimap.class.getName())
          .put(
              com.google.common.collect.SortedMultiset.class.getName(),
              com.google.common.collect.ImmutableSortedMultiset.class.getName())
          .put(
              com.google.common.collect.Table.class.getName(),
              com.google.common.collect.ImmutableTable.class.getName())
          .put(
              java.util.Collection.class.getName(),
              com.google.common.collect.ImmutableCollection.class.getName())
          .put(
              java.util.List.class.getName(),
              com.google.common.collect.ImmutableList.class.getName())
          .put(
              java.util.Map.class.getName(), com.google.common.collect.ImmutableMap.class.getName())
          .put(
              java.util.NavigableMap.class.getName(),
              com.google.common.collect.ImmutableSortedMap.class.getName())
          .put(
              java.util.NavigableSet.class.getName(),
              com.google.common.collect.ImmutableSortedSet.class.getName())
          .put(
              java.util.Set.class.getName(), com.google.common.collect.ImmutableSet.class.getName())
          .build();

  public static boolean isImmutableType(Type type) {
    return MUTABLE_TO_IMMUTABLE_CLASS_NAME_MAP.containsValue(getTypeQualifiedName(type));
  }

  static Optional<String> mutableToImmutable(String fullyQualifiedClassName) {
    return Optional.ofNullable(MUTABLE_TO_IMMUTABLE_CLASS_NAME_MAP.get(fullyQualifiedClassName));
  }

  private static String getTypeQualifiedName(Type type) {
    return type.tsym.getQualifiedName().toString();
  }
}
