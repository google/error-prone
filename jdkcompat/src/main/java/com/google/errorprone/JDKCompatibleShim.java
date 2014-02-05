package com.google.errorprone;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;

import javax.annotation.processing.Processor;
import javax.tools.JavaFileObject;

/**
 * See com.google.errorprone.JDKCompatible.
 */
interface JDKCompatibleShim {
  DiagnosticPosition getAdjustedPosition(JCTree position, int startPosAdjustment, int endPosAdjustment);
  DiagnosticPosition getIndexedPosition(int startPos, int endPos);
  ErrorProneEndPosMap getEndPosMap(JCCompilationUnit compilationUnit);
  void resetEndPosMap(JavaCompiler compiler, JavaFileObject sourceFile);
  int runCompile(Main main, String[] args, Context context, List<JavaFileObject> files,
      Iterable<? extends Processor> processors);
  int getJCTreeTag(JCTree node);
}