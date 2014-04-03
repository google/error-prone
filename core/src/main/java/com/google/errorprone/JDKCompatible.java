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

import java.util.Map;

/**
 * An abstraction over JDK version-specific APIs.
 */
public final class JDKCompatible {

  private static JDKCompatibleShim backingShim = tryJDK8();
  private static JDKCompatibleShim tryJDK8() {
    try {
      return (JDKCompatibleShim) Class.forName("com.google.errorprone.JDK8Shim").newInstance();
    } catch (LinkageError tryJDK7) {
      // JDK8Shim's prerequisites couldn't be found, assume that we're running on JRE 7 and see if
      // JDK7Shim is available.
      return tryJDK7();
    } catch (ClassNotFoundException tryJDK7) {
      // JDK8Shim doesn't exist; try JDK7Shim.
      return tryJDK7();
    } catch (InstantiationException e) {
      throw new LinkageError("Could not load JDKShim: " + e);
    } catch (IllegalAccessException e) {
      throw new LinkageError("Could not load JDKShim: " + e);
    }
  }
  private static JDKCompatibleShim tryJDK7() {
    try {
      return (JDKCompatibleShim) Class.forName("com.google.errorprone.JDK7Shim").newInstance();
    } catch (Exception e) {
      throw new LinkageError("Could not load JDKShim: " + e);
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
  public static int getEndPosition(DiagnosticPosition pos, Map<JCTree, Integer> map) {
    return backingShim.getEndPosition(pos, map);
  }
}
