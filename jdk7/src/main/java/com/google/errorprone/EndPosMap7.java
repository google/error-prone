package com.google.errorprone;

import com.google.errorprone.ErrorProneEndPosMap;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import java.util.Map;
import java.util.Set;

public class EndPosMap7 implements ErrorProneEndPosMap {

  private Map<JCTree, Integer> map;

  EndPosMap7(Map<JCTree, Integer> map) {
    this.map = map;
  }

  @Override
  public int getEndPosition(DiagnosticPosition pos) {
    return pos.getEndPosition(map);
  }

  @Override
  public Set<Map.Entry<JCTree, Integer>> entrySet() {
    return map.entrySet();
  }
}
