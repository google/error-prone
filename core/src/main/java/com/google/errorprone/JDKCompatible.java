/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import java.util.Map;

import javax.annotation.processing.Processor;
import javax.tools.JavaFileObject;

/**
 * An abstraction over JDK version-specific APIs.
 */
public final class JDKCompatible {

  private static final JDKCompatibleShim backingShim = new JDKShim();

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
   * TODO(user): kill this with fire if -Xjcov ever gets turned on by default
   * (https://code.google.com/p/error-prone/issues/detail?id=228)
   */
  public static void resetEndPosMap(JavaCompiler compiler, JavaFileObject sourceFile) {
    backingShim.resetEndPosMap(compiler, sourceFile);
  }

  /**
   * Run Main.compile() and return the exit code as an integer. (It changes to an enum in JDK8).
   */
  public static int runCompile(
      Main main,
      String[] args,
      Context context,
      com.sun.tools.javac.util.List<JavaFileObject> files,
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

  /**
   * Parse the given string as an expression.
   */
  public static JCExpression parseString(String string, Context context) {
    return backingShim.parseString(string, context);
  }

  /**
   * Returns the value of {@code tree} if it is determined to be a constant (always evaluates to the
   * same numeric value), and null otherwise. Note that returning null does not necessarily mean the
   * expression is *not* a constant.
   */
  public static Number numberValue(Tree tree, TreePath path, Context context) {
    return backingShim.numberValue(tree, path, context);
  }

  /**
   * Wraps DataFlow.dataflow and nullness propagation, and returns true if {@code tree} is a
   * non-null expression.
   */
  public static boolean isDefinitelyNonNull(
      Tree tree, MethodTree enclosingMethod, TreePath path, Context context) {
    return backingShim.isDefinitelyNonNull(tree, enclosingMethod, path, context);
  }
}
