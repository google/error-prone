package com.google.errorprone.fixes;

import com.sun.tools.javac.tree.JCTree;

import java.util.Map;

/**
 * Describes a tree position with adjustments to the start and end indices.
 */
public class AdjustedPosition7 extends AbstractAdjustedPosition {
  public AdjustedPosition7(JCTree position, int startPosAdjustment, int endPosAdjustment) {
    super(position, startPosAdjustment, endPosAdjustment);
  }

  @Override
  public int getEndPosition(Map<JCTree, Integer> endPositions) {
    return position.getEndPosition(endPositions) + endPositionAdjustment;
  }
}
