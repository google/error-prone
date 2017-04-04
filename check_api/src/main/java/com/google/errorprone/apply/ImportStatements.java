/*
 * Copyright 2012 Google Inc. All rights reserved.
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

package com.google.errorprone.apply;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCImport;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a list of import statements. Supports adding and removing import statements and pretty
 * printing the result as source code. Sorts and organizes the imports using the given {@code
 * importOrganizer}.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ImportStatements {

  private final ImportOrganizer importOrganizer;
  private int startPos = Integer.MAX_VALUE;
  private int endPos = -1;
  private final Set<String> importStrings;
  private boolean hasExistingImports;

  public static ImportStatements create(
      JCCompilationUnit compilationUnit, ImportOrganizer importOrganizer) {
    return new ImportStatements(
        (JCExpression) compilationUnit.getPackageName(),
        compilationUnit.getImports(),
        compilationUnit.endPositions,
        importOrganizer);
  }

  public ImportStatements(
      JCExpression packageTree, List<JCImport> importTrees, EndPosTable endPositions) {
    this(packageTree, importTrees, endPositions, ImportOrganizer.STATIC_FIRST_ORGANIZER);
  }

  ImportStatements(
      JCExpression packageTree,
      List<JCImport> importTrees,
      EndPosTable endPositions,
      ImportOrganizer importOrganizer) {

    // find start, end positions for current list of imports (for replacement)
    if (importTrees.isEmpty()) {
      // start/end positions are just after the package expression
      hasExistingImports = false;
      startPos =
          packageTree != null
              ? packageTree.getEndPosition(endPositions) + 2 // +2 for semicolon and newline
              : 0;
      endPos = startPos;
    } else {
      // process list of imports and find start/end positions
      hasExistingImports = true;
      for (JCImport importTree : importTrees) {
        int currStartPos = importTree.getStartPosition();
        int currEndPos = importTree.getEndPosition(endPositions);

        startPos = Math.min(startPos, currStartPos);
        endPos = Math.max(endPos, currEndPos);
      }
    }

    // sanity check for start/end positions
    Preconditions.checkState(startPos <= endPos);

    this.importOrganizer = importOrganizer;

    // convert list of JCImports to set of unique strings
    importStrings = new LinkedHashSet<>();
    importStrings.addAll(
        Lists.transform(
            importTrees,
            new Function<JCImport, String>() {
              @Override
              public String apply(JCImport input) {
                String importExpr = input.toString();
                return CharMatcher.whitespace()
                    .or(CharMatcher.is(';'))
                    .trimTrailingFrom(importExpr);
              }
            }));
  }

  /**
   * Return the start position of the import statements.
   */
  public int getStartPos() {
    return startPos;
  }

  /**
   * Return the end position of the import statements.
   */
  public int getEndPos() {
    return endPos;
  }

  /**
   * Add an import to the list of imports. If the import is already in the list, does nothing. The
   * import should be of the form "import foo.bar".
   *
   * @param importToAdd a string representation of the import to add
   * @return true if the import was added
   */
  public boolean add(String importToAdd) {
    return importStrings.add(importToAdd);
  }

  /**
   * Add all imports in a collection to this list of imports. Does not add any imports that are
   * already in the list.
   *
   * @param importsToAdd a collection of imports to add
   * @return true if any imports were added to the list
   */
  public boolean addAll(Collection<String> importsToAdd) {
    return importStrings.addAll(importsToAdd);
  }

  /**
   * Remove an import from the list of imports. If the import is not in the list, does nothing. The
   * import should be of the form "import foo.bar".
   *
   * @param importToRemove a string representation of the import to remove
   * @return true if the import was removed
   */
  public boolean remove(String importToRemove) {
    return importStrings.remove(importToRemove);
  }

  /**
   * Removes all imports in a collection to this list of imports. Does not remove any imports that
   * are not in the list.
   *
   * @param importsToRemove a collection of imports to remove
   * @return true if any imports were removed from the list
   */
  public boolean removeAll(Collection<String> importsToRemove) {
    return importStrings.removeAll(importsToRemove);
  }

  /** Returns a string representation of the imports as Java code in correct order. */
  @Override
  public String toString() {
    if (importStrings.size() == 0) {
      return "";
    }

    StringBuilder result = new StringBuilder();

    if (!hasExistingImports) {
      // insert a newline after the package expression, then add imports
      result.append('\n');
    }

    // output organized imports
    for (String importString : importOrganizer.organizeImports(importStrings)) {
      if (!importString.isEmpty()) {
        result.append(importString).append(';');
      }
      result.append('\n');
    }

    String replacementString = result.toString();
    if (!hasExistingImports) {
      return replacementString;
    } else {
      return CharMatcher.whitespace().trimTrailingFrom(replacementString); // trim last newline
    }
  }
}
