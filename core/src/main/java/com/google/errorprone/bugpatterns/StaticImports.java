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

package com.google.errorprone.bugpatterns;

import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ImportTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;

import javax.annotation.Nullable;

/**
 * Logic for inspecting static imports used by
 * {@link NonCanonicalStaticImport}, {@link NonCanonicalStaticMemberImport},
 * and {@link UnnecessaryStaticImport}.
 */
final class StaticImports {

  @AutoValue
  public abstract static class StaticImportInfo {
    /** @return the fully qualified name used to import the type (possibly non-canonical) */
    abstract String importedName();

    /** @return the fully-qualified canonical name of the type */
    abstract String canonicalName();

    /**
     * The field or variable symbol for a static non-type member import.
     */
    abstract Optional<Symbol> member();

    /**
     * Returns true if the import is canonical, i.e. the fully qualified name used to import the
     * type matches the scopes it was declared in.
     */
    public boolean isCanonical() {
      return canonicalName().equals(importedName());
    }

    /** Builds the canonical import statement for the type. */
    public String importStatement() {
      if (member().isPresent()) {
        Symbol member = member().get();
        return String.format("import static %s.%s;", canonicalName(), member.getSimpleName());
      } else {
        return String.format("import %s;", canonicalName());
      }
    }

    private static StaticImportInfo create(
        String importedName, String canonicalName, Optional<Symbol> member) {
      return new AutoValue_StaticImports_StaticImportInfo(importedName, canonicalName, member);
    }
  }

  /**
   * Returns a {@link StaticImports} if the given import is a static single-type import.
   * Returns {@code null} otherwise, e.g. because the import is non-static, or an on-demand
   * import, or statically imports a field or method.
   */
  @Nullable
  public static StaticImportInfo tryCreate(ImportTree tree, VisitorState state) {
    if (!tree.isStatic()) {
      return null;
    }
    if (!(tree.getQualifiedIdentifier() instanceof JCTree.JCFieldAccess)) {
      return null;
    }
    JCTree.JCFieldAccess access = (JCTree.JCFieldAccess) tree.getQualifiedIdentifier();
    String importedName = access.toString();
    Type result = state.getTypeFromString(importedName);
    if (result == null) {
      // If the full imported name isn't a type, it might be a field or
      // method:
      return tryAsStaticMember(access, state);
    }
    String canonicalName = state.getTypes().erasure(result).toString();
    if (canonicalName == null) {
      return null;
    }
    return StaticImportInfo.create(importedName, canonicalName, Optional.<Symbol>absent());
  }

  /**
   * Returns a {@code StaticImportInfo} for a static field or method import.
   */
  private static StaticImportInfo tryAsStaticMember(
      JCTree.JCFieldAccess access, VisitorState state) {
    if (access.getIdentifier().contentEquals("*")) {
      // Java doesn't allow non-canonical types inside wildcard imports,
      // so there's nothing to do here.
      return null;
    }
    String importedTypeName = access.getExpression().toString();
    Type importedType = state.getTypeFromString(importedTypeName);
    if (importedType == null) {
      return null;
    }
    Type canonicalType = state.getTypes().erasure(importedType);
    if (canonicalType == null) {
      return null;
    }
    Symbol.TypeSymbol baseType;
    {
      Symbol sym = ASTHelpers.getSymbol(access.getExpression());
      if (!(sym instanceof Symbol.TypeSymbol)) {
        return null;
      }
      baseType = (Symbol.TypeSymbol) sym;
    }
    Symbol.PackageSymbol pkgSym =
        ((JCTree.JCCompilationUnit) state.getPath().getCompilationUnit()).packge;
    Symbol member = lookup(baseType, baseType, access.getIdentifier(), state.getTypes(), pkgSym);
    if (member == null) {
      return null;
    }
    Type canonicalOwner = state.getTypes().erasure(member.owner.type);
    if (canonicalOwner == null) {
      return null;
    }
    return StaticImportInfo.create(
        importedTypeName, canonicalOwner.toString(), Optional.of(member));
  }

  /**
   * Looks for a field or method with the given {@code identifier}, in
   * @code typeSym} or one of it's super-types or super-interfaces,
   * and that is visible from the {@code start} symbol.
   */
  // TODO(cushon): does javac really not expose this anywhere?
  //
  // Resolve.resolveInternal{Method,Field} almost work, but we don't want
  // to filter on method signature.
  private static Symbol lookup(
      Symbol.TypeSymbol typeSym,
      Symbol.TypeSymbol start,
      Name identifier,
      Types types,
      Symbol.PackageSymbol pkg) {
    if (typeSym == null) {
      return null;
    }

    Symbol result = lookup(types.supertype(typeSym.type).tsym, start, identifier, types, pkg);
    if (result != null) {
      return result;
    }

    for (Type i : types.interfaces(typeSym.type)) {
      result = lookup(i.tsym, start, identifier, types, pkg);
      if (result != null) {
        return result;
      }
    }

    OUTER:
    for (Symbol member : typeSym.members().getSymbolsByName(identifier)) {
      if (!member.isStatic()) {
        continue;
      }
      switch ((int) (member.flags() & Flags.AccessFlags)) {
        case Flags.PUBLIC:
          break;
        case Flags.PRIVATE:
          continue OUTER;
        case 0:
        case Flags.PROTECTED:
          if (member.packge() != pkg) {
            continue OUTER;
          }
        default:
          break;
      }
      if (member.isMemberOf(start, types)) {
        return member;
      }
    }

    return null;
  }
}
