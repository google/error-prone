package com.google.errorprone;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import java.util.Map;
import java.util.Set;

/**
 * The implementation of end position table changes between JDK 7 and JDK 8. The class provides an
 * abstraction over those differences.
 */
public interface ErrorProneEndPosMap {
  Integer getEndPosition(DiagnosticPosition pos);
  Set<Map.Entry<JCTree, Integer>> entrySet();
}
