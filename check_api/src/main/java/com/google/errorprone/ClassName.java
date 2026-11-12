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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.suppliers.Supplier;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Convert;
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

  /**
   * Per-compilation cached javac state for this class name.
   *
   * @param invocation the javac compilation instance that owns the interned {@link Name}s and
   *     {@link ClassSymbol}
   * @param binaryName the binary name of the class in this compilation's {@link Name} table
   * @param enclosingCandidates candidate enclosing class names from {@link
   *     Convert#enclosingCandidates(Name)} (for example, {@code ["pkg.Outer", "pkg.Outer$Middle"]}
   *     for {@code "pkg.Outer$Middle$Inner"}), used to complete enclosing classes so that nested
   *     class symbols are entered into the symbol table before looking up {@code binaryName};
   *     computed lazily on the first {@link #symbol(VisitorState)} lookup
   * @param symbol the resolved {@link ClassSymbol}, or {@code null} if it has not been resolved yet
   *     (or was not found in the compilation)
   */
  private record CacheEntry(
      JavacInvocationInstance invocation,
      Name binaryName,
      @Nullable ImmutableList<Name> enclosingCandidates,
      @Nullable ClassSymbol symbol) {
    static CacheEntry create(JavacInvocationInstance invocation, Name binaryName) {
      return new CacheEntry(
          invocation, binaryName, /* enclosingCandidates= */ null, /* symbol= */ null);
    }

    CacheEntry withEnclosingCandidates() {
      return new CacheEntry(
          invocation,
          binaryName,
          ImmutableList.copyOf(Convert.enclosingCandidates(binaryName)),
          symbol);
    }

    CacheEntry withSymbol(ClassSymbol symbol) {
      return new CacheEntry(invocation, binaryName, enclosingCandidates, symbol);
    }
  }

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
    return getOrCreateCacheEntry(state).binaryName();
  }

  /**
   * Resolves and caches the {@link ClassSymbol} for this class in the current compilation, or
   * returns {@code null} if the symbol is not available.
   */
  public @Nullable ClassSymbol symbol(VisitorState state) {
    CacheEntry entry = getOrCreateCacheEntry(state);
    if (entry.symbol() != null) {
      return entry.symbol();
    }
    // getOrCreateCacheEntry leaves enclosingCandidates null so callers that only need
    // binaryName(state) don't compute it. Populate and cache it on the first symbol lookup in this
    // compilation so repeated negative lookups don't recompute Convert.enclosingCandidates.
    if (entry.enclosingCandidates() == null) {
      entry = entry.withEnclosingCandidates();
      cache = new SoftReference<>(entry);
    }
    // Look up binaryName in Symtab; if it is a nested class whose outer class has been entered but
    // not yet completed, getSymbolFromName completes enclosingCandidates first so the nested class
    // is entered into Symtab. Only cache positive results: javac enters supertypes lazily during
    // the AST scan, so a symbol that returns null now may become available later in the same
    // compilation unit.
    ClassSymbol sym = state.getSymbolFromName(entry.binaryName(), entry.enclosingCandidates());
    if (sym != null) {
      cache = new SoftReference<>(entry.withSymbol(sym));
    }
    return sym;
  }

  private CacheEntry getOrCreateCacheEntry(VisitorState state) {
    JavacInvocationInstance invocation = state.javacInvocationInstance();
    CacheEntry entry = cache.get();
    if (entry == null || entry.invocation() != invocation) {
      entry = CacheEntry.create(invocation, state.getName(binaryNameString));
      cache = new SoftReference<>(entry);
    }
    return entry;
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
