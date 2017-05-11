/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.errorprone.predicates;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.errorprone.predicates.type.Any;
import com.google.errorprone.predicates.type.Array;
import com.google.errorprone.predicates.type.DescendantOf;
import com.google.errorprone.predicates.type.DescendantOfAny;
import com.google.errorprone.predicates.type.Exact;
import com.google.errorprone.predicates.type.ExactAny;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.tools.javac.code.Type;

/** A collection of {@link TypePredicate}s. */
public final class TypePredicates {

  /** Match arrays. */
  public static TypePredicate isArray() {
    return Array.INSTANCE;
  }

  /** Match any type. */
  public static TypePredicate anyType() {
    return Any.INSTANCE;
  }

  /** Match types that are exactly equal. */
  public static TypePredicate isExactType(String type) {
    return isExactType(Suppliers.typeFromString(type));
  }

  /** Match types that are exactly equal. */
  public static TypePredicate isExactType(Supplier<Type> type) {
    return new Exact(type);
  }

  private static final Function<String, Supplier<Type>> GET_TYPE =
      new Function<String, Supplier<Type>>() {
        @Override
        public Supplier<Type> apply(String input) {
          return Suppliers.typeFromString(input);
        }
      };

  /** Match types that are exactly equal to any of the given types. */
  public static TypePredicate isExactTypeAny(Iterable<String> types) {
    return new ExactAny(Iterables.transform(types, GET_TYPE));
  }

  /** Match sub-types of the given type. */
  public static TypePredicate isDescendantOf(Supplier<Type> type) {
    return new DescendantOf(type);
  }

  /** Match types that are a sub-type of one of the given types. */
  public static TypePredicate isDescendantOfAny(Iterable<String> types) {
    return new DescendantOfAny(Iterables.transform(types, GET_TYPE));
  }

  /** Match sub-types of the given type. */
  public static TypePredicate isDescendantOf(String type) {
    return isDescendantOf(Suppliers.typeFromString(type));
  }
}
