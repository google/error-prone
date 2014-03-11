package com.google.errorprone;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import java.util.Map;
import java.util.Set;

public class EndPosMap7 implements ErrorProneEndPosMap {

  private Map<JCTree, Integer> map;

  EndPosMap7(Map<JCTree, Integer> map) {
    this.map = map;
  }

  /**
   * The JDK7 implementation of endPosMap returns null if there's no mapping for the given key.
   */
  @Override
  public Integer getEndPosition(DiagnosticPosition pos) {
    return pos.getEndPosition(map);
  }

  @Override
  public Set<Map.Entry<JCTree, Integer>> entrySet() {
    return map.entrySet();
  }

  public static int getEndPos(DiagnosticPosition pos, Map<JCTree, Integer> map) {
    return pos.getEndPosition(map);
  }
}
