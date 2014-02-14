package com.google.errorprone;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;

import java.util.Map;

import javax.annotation.processing.Processor;
import javax.tools.JavaFileObject;

/**
 * An abstraction over JDK version-specific APIs.
 */
public final class JDKCompatible {

  private static JDKCompatibleShim backingShim;
  static {
    try {
      backingShim = (JDKCompatibleShim) Class.forName("com.google.errorprone.JDK8Shim").newInstance();
    } catch (Exception e) {
    }

    try {
      backingShim = (JDKCompatibleShim) Class.forName("com.google.errorprone.JDK7Shim").newInstance();
    } catch (Exception e) {
    }

    if (backingShim == null) {
      throw new LinkageError("Could not load JDKShim.");
    }
  }

  /** AdjustedPosition factory. */
  public static DiagnosticPosition getAdjustedPosition(JCTree position, int startPosAdjustment,
      int endPosAdjustment) {
    return backingShim.getAdjustedPosition(position, startPosAdjustment, endPosAdjustment);
  }

  /** IndexedPosition factory. */
  public static DiagnosticPosition getIndexedPosition(int startPos, int endPos) {
    return backingShim.getIndexedPosition(startPos, endPos);
  }

  /** ErrorProneEndPosMap factory. */
  public static ErrorProneEndPosMap getEndPosMap(JCCompilationUnit compilationUnit) {
    return backingShim.getEndPosMap(compilationUnit);
  }

  /**
   * In JDK 8, the EndPosTable is guarded against being set twice. Use reflection to unset it
   * so re-parsing for end positions works.
   *
   * TODO(cushon): kill this with fire if -Xjcov ever gets turned on by default
   * (https://code.google.com/p/error-prone/issues/detail?id=228)
   */
  public static void resetEndPosMap(JavaCompiler compiler, JavaFileObject sourceFile) {
    backingShim.resetEndPosMap(compiler, sourceFile);
  }

  /**
   * Run Main.compile() and return the exit code as an integer. (It changes to an enum in JDK8).
   */
  public static int runCompile(Main main, String[] args, Context context, List<JavaFileObject> files,
      Iterable<? extends Processor> processors) {
    return backingShim.runCompile(main, args, context, files, processors);
  }

  /**
   * Returns the node's tag as an integer. (The tag type becomes an enum in JDK8).
   */
  public static int getJCTreeTag(JCTree node) {
    return backingShim.getJCTreeTag(node);
  }

  /**
   * Provides a wrapper for javac's TreeInfo#getEndPos().
   *
   * End positions are only recorded once per group of nodes with the same end
   * position (e.g. in the binop "2 + 3", the AST nodes for the literal 2 and
   * its parent binop only get one end position.) Implementing WrappedTreeMap
   * correctly requires calling TreeInfo#getEndPos(), which figures out which
   * node to use as the key for the endposmap lookup.
   */
  public static int getEndPosition(JCTree tree, Map<JCTree, Integer> map) {
    return backingShim.getEndPosition(tree, map);
  }
}
