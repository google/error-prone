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

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.util.ASTHelpers.enclosingPackage;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isStatic;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;
import org.jspecify.annotations.Nullable;

/**
 * Logic for inspecting static imports used by {@link NonCanonicalStaticImport}, {@link
 * NonCanonicalStaticMemberImport}, and {@link UnnecessaryStaticImport}.
 */
public final class StaticImports {

  /**
   * Information about a static import.
   *
   * @param importedName The fully qualified name used to import the type (possibly non-canonical).
   * @param canonicalName The fully-qualified canonical name of the type.
   * @param simpleName The simple name of the imported member.
   * @param members The field or variable symbol for a static non-type member import.
   */
  public record StaticImportInfo(
      String importedName,
      String canonicalName,
      Optional<String> simpleName,
      ImmutableSet<Symbol> members) {
    /**
     * Returns whether the import is canonical, i.e. the fully qualified name used to import the
     * type matches the scopes it was declared in.
     */
    public boolean isCanonical() {
      return canonicalName().equals(importedName());
    }

    /** Builds the canonical import statement for the type. */
    public String importStatement() {
      if (members().isEmpty()) {
        return String.format("import %s;", canonicalName());
      } else {
        return String.format("import static %s.%s;", canonicalName(), simpleName().get());
      }
    }

    private static StaticImportInfo create(String importedName, String canonicalName) {
      return new StaticImportInfo(
          importedName, canonicalName, Optional.<String>absent(), ImmutableSet.<Symbol>of());
    }

    private static StaticImportInfo create(
        String importedName, String canonicalName, String simpleName, Iterable<Symbol> members) {
      return new StaticImportInfo(
          importedName, canonicalName, Optional.of(simpleName), ImmutableSet.copyOf(members));
    }
  }

  /**
   * Returns a {@link StaticImportInfo} if the given import is a static single-type import. Returns
   * {@code null} otherwise, e.g. because the import is non-static, or an on-demand import, or
   * statically imports a field or method.
   */
  public static @Nullable StaticImportInfo tryCreate(ImportTree tree, VisitorState state) {
    if (!tree.isStatic()) {
      return null;
    }
    if (!(tree.getQualifiedIdentifier() instanceof JCTree.JCFieldAccess access)) {
      return null;
    }
    Name identifier = access.getIdentifier();
    if (identifier.contentEquals("*")) {
      // Java doesn't allow non-canonical types inside wildcard imports,
      // so there's nothing to do here.
      return null;
    }
    return tryCreate(access, state);
  }

  public static @Nullable StaticImportInfo tryCreate(MemberSelectTree access, VisitorState state) {
    Name identifier = (Name) access.getIdentifier();
    Symbol importedType = getSymbol(access.getExpression());
    if (importedType == null) {
      return null;
    }

    Types types = state.getTypes();

    Type canonicalType = types.erasure(importedType.asType());
    if (canonicalType == null) {
      return null;
    }

    Symbol sym = getSymbol(access.getExpression());
    if (!(sym instanceof Symbol.TypeSymbol baseType)) {
      return null;
    }
    Symbol.PackageSymbol pkgSym =
        ((JCTree.JCCompilationUnit) state.getPath().getCompilationUnit()).packge;
    ImmutableSet<Symbol> members = lookup(baseType, baseType, identifier, types, pkgSym);
    if (members.isEmpty()) {
      return null;
    }

    // Find the most specific subtype that defines one of the members that is imported.
    // TODO(gak): we should instead find the most specific subtype with a member that is _used_
    Type canonicalOwner = null;
    for (Symbol member : members) {
      Type owner = types.erasure(member.owner.type);
      if (canonicalOwner == null || types.isSubtype(owner, canonicalOwner)) {
        canonicalOwner = owner;
      }
    }

    if (canonicalOwner == null) {
      return null;
    }

    if (members.size() == 1 && getOnlyElement(members) instanceof ClassSymbol) {
      return StaticImportInfo.create(access.toString(), getOnlyElement(members).toString());
    }
    return StaticImportInfo.create(
        access.getExpression().toString(),
        canonicalOwner.toString(),
        identifier.toString(),
        members);
  }

  /**
   * Looks for a field or method with the given {@code identifier}, in {@code typeSym} or one of its
   * super-types or super-interfaces, and that is visible from the {@code start} symbol.
   */
  // TODO(cushon): does javac really not expose this anywhere?
  //
  // Resolve.resolveInternal{Method,Field} almost work, but we don't want
  // to filter on method signature.
  private static ImmutableSet<Symbol> lookup(
      Symbol.TypeSymbol typeSym,
      Symbol.TypeSymbol start,
      Name identifier,
      Types types,
      Symbol.PackageSymbol pkg) {
    if (typeSym == null) {
      return ImmutableSet.of();
    }

    ImmutableSet.Builder<Symbol> members = ImmutableSet.builder();

    members.addAll(lookup(types.supertype(typeSym.type).tsym, start, identifier, types, pkg));

    for (Type type : types.interfaces(typeSym.type)) {
      members.addAll(lookup(type.tsym, start, identifier, types, pkg));
    }

    for (Symbol member : typeSym.members().getSymbolsByName(identifier)) {
      if (!isStatic(member)) {
        continue;
      }
      switch ((int) (member.flags() & Flags.AccessFlags)) {
        case Flags.PRIVATE:
          continue;
        case 0:
        case Flags.PROTECTED:
          if (enclosingPackage(member) != pkg) {
            continue;
          }
          break;
        case Flags.PUBLIC:
        default:
          break;
      }
      if (member.isMemberOf(start, types)) {
        members.add(member);
      }
    }

    return members.build();
  }

  private StaticImports() {}
}
