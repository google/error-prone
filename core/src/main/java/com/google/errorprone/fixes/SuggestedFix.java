/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.errorprone.fixes;

import com.google.errorprone.ErrorProneEndPosMap;
import com.google.errorprone.JDKCompatible;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class SuggestedFix {

  public String toString(JCCompilationUnit compilationUnit) {
    StringBuilder result = new StringBuilder("replace ");
    for (Replacement replacement : getReplacements(JDKCompatible.getEndPosMap(compilationUnit))) {
      result
          .append("position " + replacement.startPosition + ":" + replacement.endPosition)
          .append(" with \"" + replacement.replaceWith + "\" ");
    }
    return result.toString();
  }

  private Collection<Pair<DiagnosticPosition, String>> nodeReplacements =
      new ArrayList<Pair<DiagnosticPosition, String>>();
  private Collection<Pair<DiagnosticPosition, DiagnosticPosition>> nodeSwaps =
      new ArrayList<Pair<DiagnosticPosition, DiagnosticPosition>>();
  private Collection<Pair<DiagnosticPosition, String>> prefixInsertions =
      new ArrayList<Pair<DiagnosticPosition, String>>();
  private Collection<Pair<DiagnosticPosition, String>> postfixInsertions =
      new ArrayList<Pair<DiagnosticPosition, String>>();
  private Collection<String> importsToAdd = new ArrayList<String>();
  private Collection<String> importsToRemove = new ArrayList<String>();

  public Set<Replacement> getReplacements(ErrorProneEndPosMap endPositions) {
    if (endPositions == null) {
      throw new IllegalArgumentException(
          "Cannot produce correct replacements without endPositions." +
              " Pass -Xjcov to the compiler to enable endPositions.");
    }
    TreeSet<Replacement> replacements = new TreeSet<Replacement>(
      new Comparator<Replacement>() {
        @Override
        public int compare(Replacement o1, Replacement o2) {
          int a = o2.startPosition;
          int b = o1.startPosition;
          return (a < b) ? -1 : ((a > b) ? 1 : 0);
        }
      });
    // TODO(eaftan): The next four for-loops are all doing the same thing.  Collapse them
    // into a single one.  Will need to make AdjustedTreePosition implement DiagnosticPosition.
    for (Pair<DiagnosticPosition, String> prefixInsertion : prefixInsertions) {
      DiagnosticPosition pos = prefixInsertion.fst;
      replacements.add(new Replacement(
          pos.getStartPosition(),
          pos.getStartPosition(),
          prefixInsertion.snd));
    }
    for (Pair<DiagnosticPosition, String> postfixInsertion : postfixInsertions) {
      DiagnosticPosition pos = postfixInsertion.fst;
      replacements.add(new Replacement(
          endPositions.getEndPosition(pos),
          endPositions.getEndPosition(pos),
          postfixInsertion.snd));
    }
    for (Pair<DiagnosticPosition, String> nodeReplacement : nodeReplacements) {
      DiagnosticPosition pos = nodeReplacement.fst;
      replacements.add(new Replacement(
          pos.getStartPosition(),
          endPositions.getEndPosition(pos),
          nodeReplacement.snd));
    }
    for (Pair<DiagnosticPosition, DiagnosticPosition> nodeSwap : nodeSwaps) {
      DiagnosticPosition pos1 = nodeSwap.fst;
      DiagnosticPosition pos2 = nodeSwap.snd;
      replacements.add(new Replacement(
          pos1.getStartPosition(),
          endPositions.getEndPosition(pos1),
          nodeSwap.snd.toString()));
      replacements.add(new Replacement(
          pos2.getStartPosition(),
          endPositions.getEndPosition(pos2),
          nodeSwap.fst.toString()));
    }
    return replacements;
  }

  public SuggestedFix replace(Tree node, String replaceWith) {
    nodeReplacements.add(
        new Pair<DiagnosticPosition, String>((DiagnosticPosition) node, replaceWith));
    return this;
  }

  /**
   * Replace the characters from startPos, inclusive, until endPos, exclusive, with the
   * given string.
   *
   * @param startPos The position from which to start replacing, inclusive
   * @param endPos The position at which to end replacing, exclusive
   * @param replaceWith The string to replace with
   */
  public SuggestedFix replace(int startPos, int endPos, String replaceWith) {
    nodeReplacements.add(new Pair<DiagnosticPosition, String>(
        JDKCompatible.getIndexedPosition(startPos, endPos), replaceWith));
    return this;
  }

  /**
   * Replace a tree node with a string, but adjust the start and end positions as well.
   * For example, if the tree node begins at index 10 and ends at index 30, this call will
   * replace the characters at index 15 through 25 with "replacement":
   * <pre>
   * {@code fix.replace(node, "replacement", 5, -5)}
   * </pre>
   *
   * @param node The tree node to replace
   * @param replaceWith The string to replace with
   * @param startPosAdjustment The adjustment to add to the start position (negative is OK)
   * @param endPosAdjustment The adjustment to add to the end position (negative is OK)
   */
  public SuggestedFix replace(Tree node, String replaceWith, int startPosAdjustment,
      int endPosAdjustment) {
    nodeReplacements.add(new Pair<DiagnosticPosition, String>(
        JDKCompatible.getAdjustedPosition((JCTree) node, startPosAdjustment, endPosAdjustment),
        replaceWith));
    return this;
  }

  public SuggestedFix prefixWith(Tree node, String prefix) {
    prefixInsertions.add(new Pair<DiagnosticPosition, String>((DiagnosticPosition) node, prefix));
    return this;
  }

  public SuggestedFix postfixWith(Tree node, String postfix) {
    postfixInsertions.add(new Pair<DiagnosticPosition, String>((DiagnosticPosition) node, postfix));
    return this;
  }

  public SuggestedFix delete(Tree node) {
    return replace(node, "");
  }

  public SuggestedFix swap(Tree node1, Tree node2) {
    nodeSwaps.add(new Pair<DiagnosticPosition, DiagnosticPosition>((DiagnosticPosition) node1, (DiagnosticPosition) node2));
    return this;
  }

  /**
   * Add an import statement as part of this SuggestedFix.
   * Import string should be of the form "foo.bar.baz".
   */
  public SuggestedFix addImport(String importString) {
    importsToAdd.add("import " + importString);
    return this;
  }

  /**
   * Add a static import statement as part of this SuggestedFix.
   * Import string should be of the form "foo.bar.baz".
   */
  public SuggestedFix addStaticImport(String importString) {
    importsToAdd.add("import static " + importString);
    return this;
  }

  /**
   * Remove an import statement as part of this SuggestedFix.
   * Import string should be of the form "foo.bar.baz".
   */
  public SuggestedFix removeImport(String importString) {
    importsToRemove.add("import " + importString);
    return this;
  }

  /**
   * Remove a static import statement as part of this SuggestedFix.
   * Import string should be of the form "foo.bar.baz".
   */
  public SuggestedFix removeStaticImport(String importString) {
    importsToRemove.add("import static " + importString);
    return this;
  }

  public Collection<String> getImportsToAdd() {
    return importsToAdd;
  }

  public Collection<String> getImportsToRemove() {
    return importsToRemove;
  }
}