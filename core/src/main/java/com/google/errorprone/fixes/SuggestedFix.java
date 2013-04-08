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

import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class SuggestedFix {

  public String toString(JCCompilationUnit compilationUnit) {
    StringBuilder result = new StringBuilder("replace ");
    for (Replacement replacement : getReplacements(compilationUnit.endPositions)) {
      result
          .append("position " + replacement.startPosition + ":" + replacement.endPosition)
          .append(" with \"" + replacement.replaceWith + "\" ");
    }
    return result.toString();
  }

  private Collection<Pair<Tree, String>> nodeReplacements = new ArrayList<Pair<Tree, String>>();
  private Collection<Pair<AdjustedTreePosition, String>> adjustedNodeReplacements =
      new ArrayList<Pair<AdjustedTreePosition, String>>();
  private Collection<Pair<Tree, Tree>> nodeSwaps = new ArrayList<Pair<Tree, Tree>>();
  private Collection<Pair<Tree, String>> prefixInsertions = new ArrayList<Pair<Tree, String>>();
  private Collection<Pair<Tree, String>> postfixInsertions = new ArrayList<Pair<Tree, String>>();
  private Collection<String> importsToAdd = new ArrayList<String>();
  private Collection<String> importsToRemove = new ArrayList<String>();

  public Set<Replacement> getReplacements(Map<JCTree, Integer> endPositions) {
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
    for (Pair<Tree, String> prefixInsertion : prefixInsertions) {
      DiagnosticPosition pos = (JCTree) prefixInsertion.fst;
      replacements.add(new Replacement(
          pos.getStartPosition(),
          pos.getStartPosition(),
          prefixInsertion.snd));
    }
    for (Pair<Tree, String> postfixInsertion : postfixInsertions) {
      DiagnosticPosition pos = (JCTree) postfixInsertion.fst;
      replacements.add(new Replacement(
          pos.getEndPosition(endPositions),
          pos.getEndPosition(endPositions),
          postfixInsertion.snd));
    }
    for (Pair<Tree, String> nodeReplacement : nodeReplacements) {
      DiagnosticPosition pos = (JCTree) nodeReplacement.fst;
      replacements.add(new Replacement(
          pos.getStartPosition(),
          pos.getEndPosition(endPositions),
          nodeReplacement.snd));
    }
    for (Pair<AdjustedTreePosition, String> adjustedNodeReplacement : adjustedNodeReplacements) {
      AdjustedTreePosition adjustedPos = adjustedNodeReplacement.fst;
      replacements.add(new Replacement(
          adjustedPos.getStartPosition(),
          adjustedPos.getEndPosition(endPositions),
          adjustedNodeReplacement.snd));
    }
    for (Pair<Tree, Tree> nodeSwap : nodeSwaps) {
      DiagnosticPosition pos1 = (JCTree) nodeSwap.fst;
      DiagnosticPosition pos2 = (JCTree) nodeSwap.snd;
      replacements.add(new Replacement(
          pos1.getStartPosition(),
          pos1.getEndPosition(endPositions),
          nodeSwap.snd.toString()));
      replacements.add(new Replacement(
          pos2.getStartPosition(),
          pos2.getEndPosition(endPositions),
          nodeSwap.fst.toString()));
    }
    return replacements;
  }

  public SuggestedFix replace(Tree node, String replaceWith) {
    nodeReplacements.add(new Pair<Tree, String>(node, replaceWith));
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
   * @param startPositionAdjustment The adjustment to add to the start position (negative is OK)
   * @param endPositionAdjustment The adjustment to add to the end position (negative is OK)
   */
  public SuggestedFix replace(Tree node, String replaceWith, int startPositionAdjustment,
      int endPositionAdjustment) {
    adjustedNodeReplacements.add(new Pair<AdjustedTreePosition, String>(
        new AdjustedTreePosition((DiagnosticPosition) node, startPositionAdjustment,
            endPositionAdjustment),
        replaceWith));
    return this;
  }

  public SuggestedFix prefixWith(Tree node, String prefix) {
    prefixInsertions.add(new Pair<Tree, String>(node, prefix));
    return this;
  }

  public SuggestedFix postfixWith(Tree node, String postfix) {
    postfixInsertions.add(new Pair<Tree, String>(node, postfix));
    return this;
  }

  public SuggestedFix delete(Tree node) {
    return replace(node, "");
  }

  public SuggestedFix swap(Tree node1, Tree node2) {
    nodeSwaps.add(new Pair<Tree, Tree>(node1, node2));
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

  /**
   * Describes a tree position with adjustments to the start and end indices.
   */
  private static class AdjustedTreePosition {
    private final DiagnosticPosition position;
    private final int startPositionAdjustment;
    private final int endPositionAdjustment;

    public AdjustedTreePosition(DiagnosticPosition position, int startPositionAdjustment,
        int endPositionAdjustment) {
      this.position = position;
      this.startPositionAdjustment = startPositionAdjustment;
      this.endPositionAdjustment = endPositionAdjustment;
    }

    public int getStartPosition() {
      return position.getStartPosition() + startPositionAdjustment;
    }

    public int getEndPosition(Map<JCTree, Integer> endPositions) {
      return position.getEndPosition(endPositions) + endPositionAdjustment;
    }
  }
}
