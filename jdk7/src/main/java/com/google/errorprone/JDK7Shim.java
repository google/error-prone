package com.google.errorprone;

import com.google.errorprone.fixes.AdjustedPosition7;
import com.google.errorprone.fixes.IndexedPosition7;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import javax.annotation.processing.Processor;
import javax.tools.JavaFileObject;

public class JDK7Shim implements JDKCompatibleShim {

  @Override
  public DiagnosticPosition getAdjustedPosition(JCTree position, int startPosAdjustment,
      int endPosAdjustment) {
    return new AdjustedPosition7(position, startPosAdjustment, endPosAdjustment);
  }

  @Override
  public DiagnosticPosition getIndexedPosition(int startPos, int endPos) {
    return new IndexedPosition7(startPos, endPos);
  }

  @Override
  public EndPosMap7 getEndPosMap(JCCompilationUnit compilationUnit) {
    if (compilationUnit.endPositions == null) {
      return null;
    }
    return new EndPosMap7(compilationUnit.endPositions);
  }

  @Override
  public void resetEndPosMap(JavaCompiler compiler, JavaFileObject sourceFile) {
    // Nothing required for JDK <= 7.
  }

  @Override
  public int runCompile(Main main, String[] args, Context context, List<JavaFileObject> files,
      Iterable<? extends Processor> processors) {
    return main.compile(args, context, files, processors);
  }

  @Override
  public int getJCTreeTag(JCTree node) {
    return node.getTag();
  }
}
