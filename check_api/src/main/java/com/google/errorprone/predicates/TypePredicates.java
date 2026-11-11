/*
 * Copyright 2015 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.suppliers.Suppliers.fromStrings;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.ClassName;
import com.google.errorprone.VisitorState;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.util.List;
import org.jspecify.annotations.Nullable;

/** A collection of {@link TypePredicate}s. */
public final class TypePredicates {

  /** Matches nothing (always {@code false}). */
  public static TypePredicate nothing() {
    return (type, state) -> false;
  }

  /** Matches everything (always {@code true}). */
  public static TypePredicate anything() {
    return (type, state) -> true;
  }

  /** Match arrays. */
  public static TypePredicate isArray() {
    return (type, state) -> type != null && state.getTypes().isArray(type);
  }

  public static TypePredicate isArrayOf(TypePredicate predicate) {
    return isArray().and((type, state) -> predicate.apply(state.getTypes().elemtype(type), state));
  }

  public static TypePredicate isPrimitive() {
    return (type, state) -> type.isPrimitive();
  }

  /** Match types that are annotated with the given annotation. */
  public static TypePredicate hasAnnotation(String annotationType) {
    return (type, state) -> ASTHelpers.hasAnnotation(type.tsym, annotationType, state);
  }

  /** Match types that are annotated with the given annotation. */
  public static TypePredicate hasAnnotation(ClassName annotationType) {
    return (type, state) -> ASTHelpers.hasAnnotation(type.tsym, annotationType, state);
  }

  /** Match types that are exactly equal. */
  public static TypePredicate isExactType(String type) {
    return isExactType(typeFromString(type));
  }

  /** Match types that are exactly equal. */
  public static TypePredicate isExactType(ClassName type) {
    return (actual, state) -> ASTHelpers.isSameType(actual, type, state);
  }

  /** Match types that are exactly equal. */
  public static TypePredicate isExactType(Supplier<Type> type) {
    return (actual, state) -> ASTHelpers.isSameType(actual, type.get(state), state);
  }

  /** Match types that are exactly equal to any of the given types. */
  public static TypePredicate isExactTypeAny(Iterable<String> types) {
    ImmutableList<Supplier<Type>> suppliers = fromStrings(types);
    return (actual, state) ->
        suppliers.stream()
            .anyMatch(supplier -> ASTHelpers.isSameType(actual, supplier.get(state), state));
  }

  /** Match types that are exactly equal to any of the given types. */
  public static TypePredicate isExactTypeAny(ClassName... types) {
    return isExactTypeAnyOf(ImmutableList.copyOf(types));
  }

  /** Match types that are exactly equal to any of the given classes. */
  public static TypePredicate isExactTypeAnyOf(Iterable<ClassName> types) {
    ImmutableList<ClassName> classNames = ImmutableList.copyOf(types);
    return (actual, state) ->
        classNames.stream().anyMatch(className -> ASTHelpers.isSameType(actual, className, state));
  }

  /** Match sub-types of the given type. */
  public static TypePredicate isDescendantOf(Supplier<Type> type) {
    return (actual, state) -> ASTHelpers.isSubtype(actual, type.get(state), state);
  }

  /** Match sub-types of the given type. */
  public static TypePredicate isDescendantOf(String type) {
    return isDescendantOf(typeFromString(type));
  }

  /** Match sub-types of the given type. */
  public static TypePredicate isDescendantOf(ClassName type) {
    return (actual, state) -> ASTHelpers.isSubtype(actual, type, state);
  }

  /** Match types that are a sub-type of one of the given types. */
  public static TypePredicate isDescendantOfAny(Iterable<String> types) {
    return isDescendantOfAny(fromStrings(types));
  }

  /** Match types that are a sub-type of one of the given types. */
  public static TypePredicate isDescendantOfAny(ClassName... types) {
    return isDescendantOfAnyOf(ImmutableList.copyOf(types));
  }

  public static TypePredicate isDescendantOfAny(ImmutableList<Supplier<Type>> types) {
    return (actual, state) -> isSubtypeOfAny(actual, types, state);
  }

  /** Match types that are a sub-type of one of the given classes. */
  public static TypePredicate isDescendantOfAnyOf(Iterable<ClassName> types) {
    return isDescendantOfAny(
        stream(types).map(ClassName::asTypeSupplier).collect(toImmutableList()));
  }

  /**
   * Returns true if {@link ASTHelpers#isSubtype} is true for {@code actual} and any of the given
   * types.
   *
   * <p>Checking subtyping is relatively expensive, and the lists of types can be long. So this
   * first checks whether a type's class is in the (cached) supertype closure of {@code actual}, and
   * skips it if not, as it can't be a supertype.
   */
  private static boolean isSubtypeOfAny(
      @Nullable Type actual, ImmutableList<Supplier<Type>> types, VisitorState state) {
    if (actual == null) {
      return false;
    }
    Type erasure = state.getTypes().erasure(actual);
    // Class types (other than Object) that aren't in the closure can't be supertypes, except for
    // arrays, the null type, and error types (e.g. from a type variable with a missing bound),
    // which can be subtypes of classes despite not having a closure.
    @Nullable List<Type> closure =
        switch (erasure.getTag()) {
          case CLASS -> state.getTypes().closure(erasure);
          case ARRAY, BOT, ERROR -> null;
          default -> List.nil();
        };
    for (Supplier<Type> supplier : types) {
      Type type = supplier.get(state);
      if (type == null) {
        continue;
      }
      // Object is excluded because it's missing from the closure if a superclass is missing.
      if (closure != null
          && type.hasTag(TypeTag.CLASS)
          && !type.isCompound()
          && !type.isUnion()
          && type.tsym != state.getSymtab().objectType.tsym
          && !contains(closure, type.tsym)) {
        continue;
      }
      if (ASTHelpers.isSubtype(actual, type, state)) {
        return true;
      }
    }
    return false;
  }

  private static boolean contains(List<Type> types, Symbol sym) {
    for (Type type : types) {
      if (type.tsym == sym) {
        return true;
      }
    }
    return false;
  }

  public static TypePredicate allOf(TypePredicate... predicates) {
    return (type, state) -> {
      for (TypePredicate predicate : predicates) {
        if (!predicate.apply(type, state)) {
          return false;
        }
      }
      return true;
    };
  }

  public static TypePredicate anyOf(TypePredicate... predicates) {
    return anyOf(ImmutableList.copyOf(predicates));
  }

  public static TypePredicate anyOf(ImmutableList<TypePredicate> predicates) {
    return (type, state) -> {
      for (TypePredicate predicate : predicates) {
        if (predicate.apply(type, state)) {
          return true;
        }
      }
      return false;
    };
  }

  public static TypePredicate not(TypePredicate predicate) {
    return (type, state) -> !predicate.apply(type, state);
  }

  private TypePredicates() {}
}
