/*
 * Copyright 2013 Google Inc. All rights reserved.
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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
   */
  IMPORT_TOP_LEVEL {
    @Override
    public JCExpression classReference(
        Inliner inliner, CharSequence topLevelClazz, CharSequence fullyQualifiedClazz) {
      if (Refaster.class.getName().contentEquals(fullyQualifiedClazz)) {
        // Special handling to ensure that the pretty-printer always recognizes Refaster references
        return inliner.maker().Ident(inliner.asName("Refaster"));
      }
      List<String> allImports = getAllImports(inliner);
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
          String.format(
              "either topLevelClass (%s) or fullyQualifiedClazz (%s) is null or empty",
              topLevelClazz, fullyQualifiedClazz));
      List<String> topLevelPath = Splitter.on('.').splitToList(topLevelClazz);
      String topClazz = Iterables.getLast(topLevelPath);
      List<String> qualifiedPath = Splitter.on('.').splitToList(fullyQualifiedClazz);
      boolean importTopLevelClazz = false, conflictTopLevelClazz = false;
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
      return inliner
          .maker()
          .Select(
              classReference(inliner, topLevelClazz, fullyQualifiedClazz), inliner.asName(member));
    }

    /* Returns a combined list of strings from importsInSource and importsToAdd. */
    private List<String> getAllImports(Inliner inliner) {
      List<String> allImports = new ArrayList<>(inliner.getImportsToAdd());
      if (inliner.getContext() != null
          && inliner.getContext().get(JCCompilationUnit.class) != null) {
        for (JCImport jcImport : inliner.getContext().get(JCCompilationUnit.class).getImports()) {
          JCFieldAccess qualified = (JCFieldAccess) jcImport.getQualifiedIdentifier();
          allImports.add(qualified.toString());
        }
      }
      return allImports;
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
          || !topLevelClazz.equals(fullyQualifiedClazz)) {
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
      inliner.addStaticImport(fullyQualifiedClazz + "." + member);
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
}
