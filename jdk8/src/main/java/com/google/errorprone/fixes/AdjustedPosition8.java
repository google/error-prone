package com.google.errorprone.fixes;

import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;

/**
 * Describes a tree position with adjustments to the start and end indices.
 */
public class AdjustedPosition8 extends AbstractAdjustedPosition {
  public AdjustedPosition8(JCTree position, int startPosAdjustment, int endPosAdjustment) {
    super(position, startPosAdjustment, endPosAdjustment);
  }

  @Override
  public int getEndPosition(EndPosTable endPositions) {
    return position.getEndPosition(endPositions) + endPositionAdjustment;
  }
}
