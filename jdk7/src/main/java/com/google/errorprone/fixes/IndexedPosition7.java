package com.google.errorprone.fixes;

import com.sun.tools.javac.tree.JCTree;

import java.util.Map;

/**
 * Describes a position that only has a start and end index.
 */
public class IndexedPosition7 extends AbstractIndexedPosition {
  public IndexedPosition7(int startPos, int endPos) {
    super(startPos, endPos);
  }

  @Override
  public int getEndPosition(Map<JCTree, Integer> endPosTable) {
    return endPos;
  }
}
