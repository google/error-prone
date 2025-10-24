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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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
       * Check if topLevelClazz or fullyQualifiedClazz is already imported.
       * If fullyQualifiedClazz is imported, return the class name.
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
      boolean importTopLevelClazz = false;
      boolean conflictTopLevelClazz = false;
      for (String importName : allImports) {
        if (importName.contentEquals(fullyQualifiedClazz)) {
          // fullyQualifiedClazz already imported
          return makeSelectExpression(inliner, qualifiedPath, qualifiedPath.size() - 1);
        }
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
      PackageSymbol currentPackage = inliner.getContext().get(PackageSymbol.class);
      if (currentPackage == null || !currentPackage.getQualifiedName().contentEquals(packge)) {
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

    private JCExpression makeSelectExpression(
        Inliner inliner, List<String> qualifiedPath, int start) {
      Iterator<String> selects = qualifiedPath.listIterator(start);
      TreeMaker maker = inliner.maker();
      JCExpression select = maker.Ident(inliner.asName(selects.next()));
      while (selects.hasNext()) {
        select = maker.Select(select, inliner.asName(selects.next()));
      }
      return select;
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
      String packge = topLevelClazz.toString();
      int lastDot = packge.lastIndexOf('.');
      packge = (lastDot >= 0) ? packge.substring(0, lastDot) : "";
      PackageSymbol currentPackage = inliner.getContext().get(PackageSymbol.class);
      if (currentPackage == null
          || !currentPackage.getQualifiedName().contentEquals(packge)
          || !topLevelClazz.toString().contentEquals(fullyQualifiedClazz)) {
        // don't import classes from the same package as the class we're refactoring
        inliner.addImport(fullyQualifiedClazz.toString());
      }
      String simpleName = fullyQualifiedClazz.toString();
      simpleName = simpleName.substring(simpleName.lastIndexOf('.') + 1);
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

  public static void bind(Context context, ImportPolicy policy) {
    context.put(ImportPolicy.class, checkNotNull(policy));
  }

  public static ImportPolicy instance(Context context) {
    ImportPolicy result = context.get(ImportPolicy.class);
    checkState(result != null, "No ImportPolicy bound in this context");
    return result;
  }

  public abstract JCExpression classReference(
      Inliner inliner, CharSequence topLevelClazz, CharSequence fullyQualifiedClazz);

  public abstract JCExpression staticReference(
      Inliner inliner,
      CharSequence topLevelClazz,
      CharSequence fullyQualifiedClazz,
      CharSequence member);

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
            Optional.ofNullable(inliner.getContext())
                .map(c -> c.get(JCCompilationUnit.class))
                .map(ImportPolicy::getImports)
                .map(Collection::stream)
                .orElse(Stream.of())
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
