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
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCImport;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents a list of import statements. Supports adding and removing import statements and pretty
 * printing the result as source code. Correctly sorts the imports according to Google Java Style
 * Guide rules.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ImportStatements {

  /** An {@link Ordering} that sorts import statements according to the Google Java Style Guide. */
  private static final Ordering<String> IMPORT_ORDERING =
      new Ordering<String>() {
        @Override
        public int compare(String s1, String s2) {
          return ComparisonChain.start()
              .compareTrueFirst(isStatic(s1), isStatic(s2))
              .compare(s1, s2)
              .result();
        }
      };

  private int startPos = Integer.MAX_VALUE;
  private int endPos = -1;
  private final SortedSet<String> importStrings;
  private boolean hasExistingImports;

  public static ImportStatements create(JCCompilationUnit compilationUnit) {
    return new ImportStatements((JCExpression) compilationUnit.getPackageName(),
        compilationUnit.getImports(), compilationUnit.endPositions);
  }

  public ImportStatements(JCExpression packageTree, List<JCImport> importTrees,
      EndPosTable endPositions) {

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

    // convert list of JCImports to list of strings
    importStrings = new TreeSet<>(IMPORT_ORDERING);
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

  /** Returns a string representation of the imports as Java code in proper Google Java Style. */
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

    // output sorted imports, with a line break between static and non-static imports
    boolean first = true;
    boolean prevIsStatic = true;
    for (String importString : importStrings) {
      boolean isStatic = isStatic(importString);
      if (!first && prevIsStatic && !isStatic) {
        result.append('\n');
      }
      result.append(importString).append(";\n");
      prevIsStatic = isStatic;
      first = false;
    }

    String replacementString = result.toString();
    if (!hasExistingImports) {
      return replacementString;
    } else {
      return CharMatcher.whitespace().trimTrailingFrom(replacementString); // trim last newline
    }
  }

  private static boolean isStatic(String importString) {
    return importString.startsWith("import static");
  }
}
