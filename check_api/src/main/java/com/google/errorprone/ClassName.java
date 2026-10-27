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

package com.google.errorprone;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.errorprone.VisitorState.SharedState;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.suppliers.Supplier;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Name;
import java.lang.ref.SoftReference;
import org.jspecify.annotations.Nullable;

/**
 * Encapsulates a qualified class name and caches its resolved javac {@link ClassSymbol}, {@link
 * Type}, and {@link Name} across a compilation.
 */
@Immutable
public final class ClassName {

  /**
   * Constructs a {@link ClassName} from a qualified canonical or binary class name (e.g. {@code
   * "java.util.List"}, {@code "java.util.Map.Entry"}, or {@code "java.util.Map$Entry"}).
   *
   * @throws IllegalArgumentException if {@code name} is empty, ends with {@code '.'}, is
   *     primitive/void, is an array type ({@code "[...]"}), or is parameterized ({@code "<...>"})
   */
  public static ClassName of(String name) {
    requireNonNull(name);
    validateClassName(name);
    return new ClassName(VisitorState.inferBinaryName(name));
  }

  private record CacheEntry(
      JavacInvocationInstance invocation,
      Name binaryName,
      @Nullable ClassSymbol symbol,
      @Nullable SharedState lastAbsentSharedState) {}

  private final String binaryNameString;

  // Memoized per-Context javac state via SoftReference
  @SuppressWarnings("Immutable")
  private volatile SoftReference<CacheEntry> cache = new SoftReference<>(null);

  // Stateless method reference delegating to type(VisitorState)
  @SuppressWarnings("Immutable")
  private final Supplier<@Nullable Type> typeSupplier = this::type;

  private ClassName(String binaryNameString) {
    this.binaryNameString = binaryNameString;
  }

  /** Returns the cached javac {@link Name} for the binary name. */
  public Name binaryName(VisitorState state) {
    JavacInvocationInstance invocation = state.javacInvocationInstance();
    CacheEntry entry = cache.get();
    if (entry != null && entry.invocation() == invocation) {
      return entry.binaryName();
    }
    Name binaryName = state.getName(binaryNameString);
    cache =
        new SoftReference<>(
            new CacheEntry(
                invocation, binaryName, /* symbol= */ null, /* lastAbsentSharedState= */ null));
    return binaryName;
  }

  /**
   * Resolves and caches the {@link ClassSymbol} for this class in the current compilation, or
   * returns {@code null} if the symbol is not available.
   */
  public @Nullable ClassSymbol symbol(VisitorState state) {
    JavacInvocationInstance invocation = state.javacInvocationInstance();
    SharedState sharedState = state.sharedState();
    CacheEntry entry = cache.get();
    Name binaryName;
    if (entry != null && entry.invocation() == invocation) {
      if (entry.symbol() != null) {
        return entry.symbol();
      }
      if (entry.lastAbsentSharedState() == sharedState) {
        return null;
      }
      binaryName = entry.binaryName();
    } else {
      binaryName = state.getName(binaryNameString);
    }
    ClassSymbol sym = state.getSymbolFromName(binaryName);
    cache =
        new SoftReference<>(
            new CacheEntry(invocation, binaryName, sym, sym == null ? sharedState : null));
    return sym;
  }

  /**
   * Resolves and caches the {@link Type} for this class in the current compilation, or returns
   * {@code null} if the symbol is not available.
   */
  public @Nullable Type type(VisitorState state) {
    ClassSymbol sym = symbol(state);
    return sym == null ? null : sym.asType();
  }

  /** Returns a {@link Supplier} view of {@link #type(VisitorState)}. */
  public Supplier<@Nullable Type> asTypeSupplier() {
    return typeSupplier;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return obj instanceof ClassName that && this.binaryNameString.equals(that.binaryNameString);
  }

  @Override
  public int hashCode() {
    return binaryNameString.hashCode();
  }

  @Override
  public String toString() {
    return binaryNameString;
  }

  private static void validateClassName(String name) {
    checkArgument(!name.isEmpty(), "class name must be non-empty");
    checkArgument(name.charAt(name.length() - 1) != '.', "invalid class name: %s", name);
    checkArgument(
        !name.contains("[") && !name.contains("]"),
        "Cannot create ClassName for array types (%s), build them using Suppliers.arrayOf() or"
            + " VisitorState.arrayTypeForType()",
        name);
    checkArgument(
        !name.contains("<") && !name.contains(">"),
        "Cannot create ClassName for generic types (%s), build them using VisitorState.getType()",
        name);
    switch (name) {
      case "byte", "short", "int", "long", "float", "double", "boolean", "char", "void" ->
          throw new IllegalArgumentException(
              String.format("Cannot create ClassName for primitive or void type (%s)", name));
      default -> {}
    }
  }
}
