package com.google.errorprone;

import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Wraps a Map<JCTree, Integer> (used for javac endpositions) so that the JCTree keys can be
 * searched by value equality.  JCTree does not implement equals and hashCode, so reference
 * equality would be used otherwise.
 *
 * @author Eddie Aftandilian (eaftan@google.com)
 */
class WrappedTreeMap extends AbstractMap<JCTree, Integer> {

  /**
   * A map from wrapped tree nodes to tree end positions.
   */
  private final Map<WrappedTreeNode, Integer> wrappedMap;

  public WrappedTreeMap(Map<JCTree, Integer> map) {
    wrappedMap = new HashMap<WrappedTreeNode, Integer>();
    for (Map.Entry<JCTree, Integer> entry : map.entrySet()) {
      if (wrappedMap.put(new WrappedTreeNode(entry.getKey()), entry.getValue()) != null) {
        throw new AssertionError("Node collision in WrappedTreeMap");
      }
    }
  }

  @Override
  public Set<Map.Entry<JCTree, Integer>> entrySet() {
    throw new UnsupportedOperationException("entrySet() not implemented on WrappedTreeMap");
  }

  @Override
  public Integer get(Object key) {
    if (!(key instanceof JCTree)) {
      return null;
    }
    WrappedTreeNode wrappedNode = new WrappedTreeNode((JCTree) key);
    return wrappedMap.get(wrappedNode);
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
     * equals compares start and preferred positions, kind of node, and tag. This is an
     * approximation and may not actually distinguish between unequal tree nodes.
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
      result = 31 * result + node.getTag();
      return result;
    }
  }

}
