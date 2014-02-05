package com.google.errorprone.fixes;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

/**
 * Describes a position that only has a start and end index.
 */
public abstract class AbstractIndexedPosition implements DiagnosticPosition {

  final int startPos;
  final int endPos;

  public AbstractIndexedPosition(int startPos, int endPos) {
    this.startPos = startPos;
    this.endPos = endPos;
  }

  @Override
  public JCTree getTree() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getStartPosition() {
    return startPos;
  }

  @Override
  public int getPreferredPosition() {
    throw new UnsupportedOperationException();
  }
}
