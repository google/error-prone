package com.google.errorprone;

import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Position;

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
class WrappedTreeMap extends AbstractMap<JCTree, Integer> implements ErrorProneEndPosMap {
  /**
   * A map from wrapped tree nodes to tree end positions.
   */
  private final Map<WrappedTreeNode, Integer> wrappedMap;

  private final Log log;

  /**
   * Boolean value that's set to true once wrappedMap has been built.
   * Used to detect collisions during map construction.
   */
  private final boolean built;

  public WrappedTreeMap(Log log, ErrorProneEndPosMap map) {
    this.log = log;
    wrappedMap = new HashMap<WrappedTreeNode, Integer>();
    for (Map.Entry<JCTree, Integer> entry : map.entrySet()) {
      wrappedMap.put(new WrappedTreeNode(entry.getKey()), entry.getValue());
    }
    built = true;
  }

  @Override
  public Set<Map.Entry<JCTree, Integer>> entrySet() {
    throw new UnsupportedOperationException("entrySet() not implemented on WrappedTreeMap");
  }

  /**
   * This should never be called directly. It exists to complete a minimal implementation of
   * Map<JCTree, Integer> so WrappedTreeMap can be used with TreeInfo#getEndPos() below in the
   * implementation of getEndPosition().
   */
  @Override
  public Integer get(Object key) {
    if (!(key instanceof JCTree)) {
      return null;
    }
    WrappedTreeNode wrappedNode = new WrappedTreeNode((JCTree) key);
    return wrappedMap.get(wrappedNode);
  }

  @Override
  public Integer getEndPosition(DiagnosticPosition pos) {
    // If two nodes share an end position, there's only one entry in the table.
    // Call TreeInfo#getEndPos() to figure out which node is the key for the
    // current node's entry.
    return JDKCompatible.getEndPosition(pos.getTree(), this);
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
          && !objectsEquals(((JCLiteral) node).getValue(), ((JCLiteral) other.node).getValue())) {
        return false;
      }

      return node.getStartPosition() == other.node.getStartPosition() &&
          node.getPreferredPosition() == other.node.getPreferredPosition() &&
          thisKind == otherKind &&
          node.getTag() == other.node.getTag();
    }

    // Can't use Objects.equals() because we still support Java 6.
    private boolean objectsEquals(Object a, Object b) {
      return (a == b) || ((a != null) && a.equals(b));
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
      result = 31 * result + JDKCompatible.getJCTreeTag(node);
      if (node instanceof JCLiteral) {
        Object value = ((JCLiteral) node).getValue();
        if (value != null) {
          result = 31 * result + value.hashCode();
        }
      }
      return result;
    }
  }

}
