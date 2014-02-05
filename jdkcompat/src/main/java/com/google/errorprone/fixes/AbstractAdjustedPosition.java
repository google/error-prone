package com.google.errorprone.fixes;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

/**
 * Describes a tree position with adjustments to the start and end indices.
 */
public abstract class AbstractAdjustedPosition implements DiagnosticPosition {
  protected final JCTree position;
  protected final int startPositionAdjustment;
  protected final int endPositionAdjustment;

  public AbstractAdjustedPosition(JCTree position, int startPosAdjustment, int endPosAdjustment) {
    this.position = position;
    this.startPositionAdjustment = startPosAdjustment;
    this.endPositionAdjustment = endPosAdjustment;
  }

  @Override
  public int getStartPosition() {
    return position.getStartPosition() + startPositionAdjustment;
  }

  @Override
  public JCTree getTree() {
    return position;
  }

  @Override
  public int getPreferredPosition() {
    throw new UnsupportedOperationException();
  }
}
