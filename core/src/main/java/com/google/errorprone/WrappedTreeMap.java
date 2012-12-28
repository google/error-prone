package com.google.errorprone;

import com.sun.tools.javac.tree.JCTree;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
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
   * A set of the entries in the map.
   */
  private final Set<Map.Entry<JCTree, Integer>> entrySet;

  /**
   * A map from wrapped tree nodes to tree end positions.
   */
  private final Map<WrappedTreeNode, Integer> wrappedMap;

  public WrappedTreeMap(Map<JCTree, Integer> map) {
    wrappedMap = new HashMap<WrappedTreeNode, Integer>();
    entrySet = new HashSet<Map.Entry<JCTree, Integer>>();
    for (Map.Entry<JCTree, Integer> entry : map.entrySet()) {
      wrappedMap.put(new WrappedTreeNode(entry.getKey()), entry.getValue());
      entrySet.add(entry);
    }
  }

  @Override
  public Set<Map.Entry<JCTree, Integer>> entrySet() {
    return entrySet;
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
     * equals compares start position, kind of node, tag, and string representation. Note that
     * this is an approximation and may not actually distinguish between unequal tree nodes.
     */
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof WrappedTreeNode)) {
        return false;
      }
      WrappedTreeNode other = (WrappedTreeNode) o;

      return node.getStartPosition() == other.node.getStartPosition() &&
          node.getKind() == other.node.getKind() &&
          node.getTag() == other.node.getTag() &&
          node.toString().equals(other.node.toString());
    }

    @Override
    public int hashCode() {
      int result = 17;
      result = 31 * result + node.getStartPosition();
      result = 31 * result + node.getKind().ordinal();
      result = 31 * result + node.getTag();
      result = 31 * result + node.toString().hashCode();
      return result;
    }
  }

}