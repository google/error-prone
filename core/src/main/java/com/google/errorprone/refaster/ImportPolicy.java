/*
 * Copyright 2013 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.FindIdentifiers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Kinds.Kind;
import com.sun.tools.javac.code.Kinds.KindSelector;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.TreeMaker;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Policy specifying when and how to import classes when inlining types.
 *
 * @author Louis Wasserman
 */
public enum ImportPolicy {
  // TODO(lowasser): add support for other policies, like "explicitly qualify everything"

  /**
   * Import the outermost class and explicitly qualify references below that. For example, to
   * reference {@code com.google.Foo.Bar}, we import {@code com.google.Foo} and explicitly qualify
   * {@code Foo.Bar}.
   *
   * <p><b>Note:</b> static methods named {@code assertThat}, {@code assertWithMessage} and {@code
   * assertAbout} are always statically imported.
   */
  IMPORT_TOP_LEVEL {

    private static final ImmutableSet<String> METHOD_NAMES_TO_STATICALLY_IMPORT =
        ImmutableSet.of("assertThat", "assertAbout", "assertWithMessage");

    @Override
    public JCExpression classReference(
        Inliner inliner, CharSequence topLevelClazz, CharSequence fullyQualifiedClazz) {
      if (Refaster.class.getName().contentEquals(fullyQualifiedClazz)) {
        // Special handling to ensure that the pretty-printer always recognizes Refaster references
        return inliner.maker().Ident(inliner.asName("Refaster"));
      }
      ImmutableSet<String> allImports = getAllImports(inliner, WhichImports.NON_STATIC);
      /*
       * If fullyQualifiedClazz is imported and its simple name isn't shadowed at the replacement
       * site, return the class name.
       * If the simple name of topLevelClazz refers to some other type at the replacement site (e.g.
       * an inherited member type), return the fully qualified name.
       * If topLevelClazz is imported, return the name from the top level class to the class name.
       * If there are imports whose names conflict with topLevelClazz, return fully qualified name.
       * Otherwise, import toplevelClazz, and return the name from the top level class
       * to the class name.
       */
      checkArgument(
          topLevelClazz.length() > 0 && fullyQualifiedClazz.length() > 0,
          "either topLevelClass (%s) or fullyQualifiedClazz (%s) is null or empty",
          topLevelClazz,
          fullyQualifiedClazz);
      List<String> topLevelPath = Splitter.on('.').splitToList(topLevelClazz);
      String topClazz = Iterables.getLast(topLevelPath);
      List<String> qualifiedPath = Splitter.on('.').splitToList(fullyQualifiedClazz);
      if (allImports.contains(fullyQualifiedClazz.toString())
          && !isShadowedInScope(inliner, Iterables.getLast(qualifiedPath), fullyQualifiedClazz)) {
        // fullyQualifiedClazz already imported
        return makeSelectExpression(inliner, qualifiedPath, qualifiedPath.size() - 1);
      }
      if (isShadowedInScope(inliner, topClazz, topLevelClazz)) {
        // The simple name of the top-level class refers to something else (e.g. an inherited member
        // type) at the replacement site, which an import can't override.
        return makeSelectExpression(inliner, qualifiedPath, 0);
      }
      boolean importTopLevelClazz = false;
      boolean conflictTopLevelClazz = false;
      for (String importName : allImports) {
        importTopLevelClazz |= importName.contentEquals(topLevelClazz);
        if (!importTopLevelClazz) {
          conflictTopLevelClazz |=
              topClazz.equals(Iterables.getLast(Splitter.on('.').split(importName)));
        }
      }
      if (importTopLevelClazz) {
        return makeSelectExpression(inliner, qualifiedPath, topLevelPath.size() - 1);
      } else if (conflictTopLevelClazz) {
        return makeSelectExpression(inliner, qualifiedPath, 0);
      }
      // No conflicts
      String packge = Joiner.on('.').join(topLevelPath.subList(0, topLevelPath.size() - 1));
      PackageSymbol currentPackage = inliner.packageSymbol();
      if (!currentPackage.getQualifiedName().contentEquals(packge)) {
        // don't import classes from the same package as the class we're refactoring
        inliner.addImport(topLevelClazz.toString());
      }
      return makeSelectExpression(inliner, qualifiedPath, topLevelPath.size() - 1);
    }

    @Override
    public JCExpression staticReference(
        Inliner inliner,
        CharSequence topLevelClazz,
        CharSequence fullyQualifiedClazz,
        CharSequence member) {
      // NOTE(b/17121704): we always statically import certain method names
      if (METHOD_NAMES_TO_STATICALLY_IMPORT.contains(member.toString())) {
        return STATIC_IMPORT_ALWAYS.staticReference(
            inliner, topLevelClazz, fullyQualifiedClazz, member);
      }
      return inliner
          .maker()
          .Select(
              classReference(inliner, topLevelClazz, fullyQualifiedClazz), inliner.asName(member));
    }
  },
  /** Import nested classes directly, and qualify static references from the class level. */
  IMPORT_CLASS_DIRECTLY {
    @Override
    public JCExpression classReference(
        Inliner inliner, CharSequence topLevelClazz, CharSequence fullyQualifiedClazz) {
      if (Refaster.class.getName().contentEquals(fullyQualifiedClazz)) {
        // Special handling to ensure that the pretty-printer always recognizes Refaster references
        return inliner.maker().Ident(inliner.asName("Refaster"));
      }
      List<String> qualifiedPath = Splitter.on('.').splitToList(fullyQualifiedClazz);
      String simpleName = Iterables.getLast(qualifiedPath);
      if (isShadowedInScope(inliner, simpleName, fullyQualifiedClazz)
          || hasConflictingImport(inliner, simpleName, fullyQualifiedClazz)) {
        // The simple name refers to something else at the replacement site (e.g. an inherited
        // member type or another imported class), so importing it wouldn't work.
        return makeSelectExpression(inliner, qualifiedPath, 0);
      }
      String packge = topLevelClazz.toString();
      int lastDot = packge.lastIndexOf('.');
      packge = (lastDot >= 0) ? packge.substring(0, lastDot) : "";
      PackageSymbol currentPackage = inliner.packageSymbol();
      if (!currentPackage.getQualifiedName().contentEquals(packge)
          || !topLevelClazz.toString().contentEquals(fullyQualifiedClazz)) {
        // don't import classes from the same package as the class we're refactoring
        inliner.addImport(fullyQualifiedClazz.toString());
      }
      return inliner.maker().Ident(inliner.asName(simpleName));
    }

    @Override
    public JCExpression staticReference(
        Inliner inliner,
        CharSequence topLevelClazz,
        CharSequence fullyQualifiedClazz,
        CharSequence member) {
      if (Refaster.class.getName().contentEquals(topLevelClazz)) {
        // Special handling to ensure that the pretty-printer always recognizes Refaster references
        return inliner
            .maker()
            .Select(inliner.maker().Ident(inliner.asName("Refaster")), inliner.asName(member));
      }
      return inliner
          .maker()
          .Select(
              classReference(inliner, topLevelClazz, fullyQualifiedClazz), inliner.asName(member));
    }
  },

  /**
   * When inlining static methods, always static import the method. Non-static references to classes
   * are imported from the top level as in {@code IMPORT_TOP_LEVEL}.
   */
  STATIC_IMPORT_ALWAYS {
    @Override
    public JCExpression classReference(
        Inliner inliner, CharSequence topLevelClazz, CharSequence fullyQualifiedClazz) {
      return IMPORT_TOP_LEVEL.classReference(inliner, topLevelClazz, fullyQualifiedClazz);
    }

    @Override
    public JCExpression staticReference(
        Inliner inliner,
        CharSequence topLevelClazz,
        CharSequence fullyQualifiedClazz,
        CharSequence member) {
      if (Refaster.class.getName().contentEquals(topLevelClazz)) {
        // Special handling to ensure that the pretty-printer always recognizes Refaster references
        return inliner
            .maker()
            .Select(inliner.maker().Ident(inliner.asName("Refaster")), inliner.asName(member));
      }
      // Foo.class tokens are considered static members :(.
      if (member.toString().equals("class")) {
        return IMPORT_TOP_LEVEL.staticReference(
            inliner, topLevelClazz, fullyQualifiedClazz, member);
      }
      // Check to see if the reference is already static-imported.
      String importableName = fullyQualifiedClazz + "." + member;
      if (!getAllImports(inliner, WhichImports.STATIC).contains(importableName)) {
        inliner.addStaticImport(importableName);
      }
      return inliner.maker().Ident(inliner.asName(member));
    }
  };

  public abstract JCExpression classReference(
      Inliner inliner, CharSequence topLevelClazz, CharSequence fullyQualifiedClazz);

  public abstract JCExpression staticReference(
      Inliner inliner,
      CharSequence topLevelClazz,
      CharSequence fullyQualifiedClazz,
      CharSequence member);

  private static JCExpression makeSelectExpression(
      Inliner inliner, List<String> qualifiedPath, int start) {
    Iterator<String> selects = qualifiedPath.listIterator(start);
    TreeMaker maker = inliner.maker();
    JCExpression select = maker.Ident(inliner.asName(selects.next()));
    while (selects.hasNext()) {
      select = maker.Select(select, inliner.asName(selects.next()));
    }
    return select;
  }

  /**
   * Returns true if, at the replacement site, {@code simpleName} refers to a type other than {@code
   * qualifiedName} that a single-type import of {@code qualifiedName} would not override, so the
   * reference has to be qualified instead. That covers member types (declared or inherited) and
   * type variables in scope (JLS 6.4.1), and top-level types declared in the same compilation unit,
   * which can't be imported over (JLS 7.5.1).
   *
   * <p>Top-level types from other files in the same package, or from on-demand imports (including
   * {@code java.lang}), are overridden by a single-type import, so they don't count, unless {@code
   * qualifiedName} is a {@code java.lang} type, which {@link Inliner#addImport} never imports.
   * Existing single-type imports of other top-level types don't count either; callers check those
   * against the import list.
   *
   * <p>Local classes declared in a method body aren't detected, because {@link
   * FindIdentifiers#findIdent} doesn't see them. {@code SuggestedFixes.qualifyType} makes the same
   * tradeoff.
   */
  private static boolean isShadowedInScope(
      Inliner inliner, String simpleName, CharSequence qualifiedName) {
    TreePath path = inliner.path();
    if (path == null) {
      return false;
    }
    VisitorState state = VisitorState.createForUtilityPurposes(inliner.getContext()).withPath(path);
    Symbol sym = FindIdentifiers.findIdent(simpleName, state, KindSelector.TYP);
    if (sym == null) {
      return false;
    }
    if (sym.kind == Kind.STATICERR || sym.kind == Kind.AMBIGUOUS) {
      // e.g. a type variable or a non-static member type of a generic class referenced from a
      // static context, or a member type inherited from two supertypes. javac reports these before
      // consulting imports.
      return true;
    }
    if (sym.kind != Kind.TYP) {
      return false;
    }
    if (sym instanceof ClassSymbol classSymbol
        && classSymbol.getQualifiedName().contentEquals(qualifiedName)) {
      return false;
    }
    // Inliner.addImport doesn't add imports for java.lang types, so a same-package or on-demand
    // type would win.
    return sym.owner == null
        || sym.owner.kind != Kind.PCK
        || qualifiedName.toString().startsWith("java.lang.")
        || inliner.compilationUnit().getTypeDecls().stream()
            .anyMatch(t -> t instanceof ClassTree c && sym.equals(ASTHelpers.getSymbol(c)));
  }

  /**
   * Returns true if a class other than {@code qualifiedName} with the same simple name is already
   * imported, or is about to be.
   */
  private static boolean hasConflictingImport(
      Inliner inliner, String simpleName, CharSequence qualifiedName) {
    return getAllImports(inliner, WhichImports.NON_STATIC).stream()
        .anyMatch(i ->
            !i.contentEquals(qualifiedName)
                && simpleName.equals(Iterables.getLast(Splitter.on('.').split(i))));
  }

  private enum WhichImports {
    STATIC {
      @Override
      Stream<String> getExistingImports(Inliner inliner) {
        return inliner.getStaticImportsToAdd().stream();
      }

      @Override
      boolean existingImportMatches(JCImport jcImport) {
        return jcImport.isStatic();
      }
    },
    NON_STATIC {
      @Override
      Stream<String> getExistingImports(Inliner inliner) {
        return inliner.getImportsToAdd().stream();
      }

      @Override
      boolean existingImportMatches(JCImport jcImport) {
        // An inner type can be imported non-statically or statically
        return true;
      }
    };

    abstract Stream<String> getExistingImports(Inliner inliner);

    abstract boolean existingImportMatches(JCImport jcImport);
  }

  /**
   * Returns the set of imports that already exist of the import type (both in the source file and
   * in the pending list of imports to add).
   */
  private static ImmutableSet<String> getAllImports(Inliner inliner, WhichImports whichImports) {
    return Streams.concat(
            whichImports.getExistingImports(inliner),
            getImports(inliner.compilationUnit()).stream()
                .filter(JCImport.class::isInstance)
                .map(JCImport.class::cast)
                .filter(whichImports::existingImportMatches)
                .map(imp -> getQualifiedIdentifier(imp).toString()))
        .collect(toImmutableSet());
  }

  // The return type of JCCompilationUnit#getImports changed for 'import module' in JDK 23
  @SuppressWarnings("UnnecessaryCast")
  private static List<? extends ImportTree> getImports(JCCompilationUnit unit) {
    return ((CompilationUnitTree) unit).getImports();
  }

  // Added for 'module import' declarations in JDK 23
  private static JCTree getQualifiedIdentifier(JCImport i) {
    try {
      return (JCTree) JCImport.class.getMethod("getQualifiedIdentifier").invoke(i);
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }
}
