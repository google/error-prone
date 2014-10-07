/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import com.google.common.base.Objects;

import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Position;

import java.util.HashMap;
import java.util.Map;

/**
 * Wraps a Map<JCTree, Integer> (used for javac endpositions) so that the JCTree keys can be
 * searched by value equality.  JCTree does not implement equals and hashCode, so reference
 * equality would be used otherwise.
 *
 * @author Eddie Aftandilian (eaftan@google.com)
 */
class WrappedTreeMap implements EndPosTable {
  /**
   * A map from wrapped tree nodes to tree end positions.
   */
  private final Map<WrappedTreeNode, Integer> backingMap;

  private final Log log;

  /**
   * Boolean value that's set to true once wrappedMap has been built.
   * Used to detect collisions during map construction.
   */
  private final boolean built;

  public WrappedTreeMap(Log log, EndPosTable endPosMap) {
    this.log = log;
    backingMap = new HashMap<>();
    for (Map.Entry<JCTree, Integer> entry : EndPosTableUtil.getEntries(endPosMap)) {
      backingMap.put(new WrappedTreeNode(entry.getKey()), entry.getValue());
    }
    built = true;
  }

  @Override
  public int getEndPos(JCTree tree) {
    if (tree == null) {
      return Position.NOPOS;
    }
    WrappedTreeNode wrappedNode = new WrappedTreeNode(tree);
    Integer result = backingMap.get(wrappedNode);
    if (result == null) {
      return Position.NOPOS;
    }
    return result;
  }

  /**
   * Wraps a node of type JCTree so that we can do an equals/hashCode comparison.
   */
  public class WrappedTreeNode {

    private final JCTree node;

    public WrappedTreeNode(JCTree node) {
      this.node = node;
    }

    /**
     * equals compares start and preferred positions, kind of node, and tag. Additionally, literal
     * nodes have their literal values compared.  This is an approximation and may not actually
     * distinguish between unequal tree nodes.
     *
     * Note: Do not include node.toString() as part of the hash or the equals.  We generate
     * the WrappedTreeMap after the parse phase, but we compare after the flow phase.  The
     * attribute phase may alter the structure of the AST nodes such that their string
     * representations no longer match.  For example, annotation nodes after the parse phase look
     * like:
     * @SuppressWarnings("foo")
     * But after the flow phase, they look like:
     * @SuppressWarnings(value = "foo")
     */
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof WrappedTreeNode)) {
        return false;
      }

      WrappedTreeNode other = (WrappedTreeNode) o;
      boolean res = equals(other);
      if (!built && res && (other != this)) {
        log.rawWarning(Position.NOPOS,
            "error-prone WrappedTreeMap collision between " + node + " and " + other.node + ", " +
            "suggested fixes may be wrong.  " +
            "Please report at https://code.google.com/p/error-prone/issues/entry.");
      }
      return res;
    }

    private boolean equals(WrappedTreeNode other) {
      // LetExpr and TypeBoundKind throw an AssertionError on getKind(). Ignore them for computing
      // equality.
      Kind thisKind;
      Kind otherKind;
      try {
        thisKind = node.getKind();
      } catch (AssertionError e) {
        thisKind = null;
      }
      try {
        otherKind = other.node.getKind();
      } catch (AssertionError e) {
        otherKind = null;
      }

      // Literal nodes with unequal values are never equal.
      if (node instanceof JCLiteral && other.node instanceof JCLiteral
          && !Objects.equal(((JCLiteral) node).getValue(), ((JCLiteral) other.node).getValue())) {
        return false;
      }

      return node.getStartPosition() == other.node.getStartPosition() &&
          node.getPreferredPosition() == other.node.getPreferredPosition() &&
          thisKind == otherKind &&
          node.getTag() == other.node.getTag();
    }

    /**
     * Note: Do not include node.toString() as part of the hash or the equals.  We generate
     * the WrappedTreeMap after the parse phase, but we compare after the flow phase.  The
     * attribute phase may alter the structure of the AST nodes such that their string
     * representations no longer match.  For example, annotation nodes after the parse phase look
     * like:
     * @SuppressWarnings("foo")
     * But after the flow phase, they look like:
     * @SuppressWarnings(value = "foo")
     */
    @Override
    public int hashCode() {
      int result = 17;
      result = 31 * result + node.getStartPosition();
      result = 31 * result + node.getPreferredPosition();
      try {
        result = 31 * result + node.getKind().ordinal();
      } catch (AssertionError e) {
        // getKind() throws an AssertionError for LetExpr and TypeBoundKind. Ignore it for 
        // calculating the hash code.
      }
      result = 31 * result + node.getTag().ordinal();
      if (node instanceof JCLiteral) {
        Object value = ((JCLiteral) node).getValue();
        if (value != null) {
          result = 31 * result + value.hashCode();
        }
      }
      return result;
    }
  }

  @Override
  public void storeEnd(JCTree tree, int endpos) {
    throw new IllegalStateException();
  }

  @Override
  public int replaceTree(JCTree oldtree, JCTree newtree) {
    throw new IllegalStateException();
  }
}
