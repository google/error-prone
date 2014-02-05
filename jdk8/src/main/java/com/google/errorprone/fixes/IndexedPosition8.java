package com.google.errorprone.fixes;

import com.sun.tools.javac.tree.EndPosTable;

/**
 * Describes a position that only has a start and end index.
 */
public class IndexedPosition8 extends AbstractIndexedPosition {
  public IndexedPosition8(int startPos, int endPos) {
    super(startPos, endPos);
  }

  @Override
  public int getEndPosition(EndPosTable endPosTable) {
    return endPos;
  }
}
