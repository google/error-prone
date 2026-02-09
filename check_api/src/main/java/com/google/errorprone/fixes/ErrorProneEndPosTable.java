/*
 * Copyright 2025 The Error Prone Authors.
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

package com.google.errorprone.fixes;

import com.google.common.base.Throwables;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.JCDiagnostic;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;

/** A compatibility wrapper around {@code EndPosTable}. */
public interface ErrorProneEndPosTable {

  static final MethodHandle GET_END_POS_HANDLE = getEndPosMethodHandle();

  private static MethodHandle getEndPosMethodHandle() {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    try {
      // JDK versions after https://bugs.openjdk.org/browse/JDK-8372948
      // (pos, unit) -> pos.getEndPosition()
      return MethodHandles.dropArguments(
          lookup.findVirtual(
              JCDiagnostic.DiagnosticPosition.class,
              "getEndPosition",
              MethodType.methodType(int.class)),
          1,
          JCCompilationUnit.class);
    } catch (ReflectiveOperationException e1) {
      try {
        // JDK versions before https://bugs.openjdk.org/browse/JDK-8372948
        // (pos, unit) -> pos.getEndPosition(unit.endPositions)
        Class<?> endPosTableClass = Class.forName("com.sun.tools.javac.tree.EndPosTable");
        return MethodHandles.filterArguments(
            lookup.findVirtual(
                JCDiagnostic.DiagnosticPosition.class,
                "getEndPosition",
                MethodType.methodType(int.class, endPosTableClass)),
            1,
            lookup
                .findVarHandle(JCCompilationUnit.class, "endPositions", endPosTableClass)
                .toMethodHandle(VarHandle.AccessMode.GET));
      } catch (ReflectiveOperationException e2) {
        e2.addSuppressed(e1);
        throw new LinkageError(e2.getMessage(), e2);
      }
    }
  }

  static ErrorProneEndPosTable create(CompilationUnitTree unit) {
    MethodHandle getEndPosition = MethodHandles.insertArguments(GET_END_POS_HANDLE, 1, unit);
    return pos -> {
      try {
        return (int) getEndPosition.invokeExact(pos);
      } catch (Throwable e) {
        Throwables.throwIfUnchecked(e);
        throw new AssertionError(e);
      }
    };
  }

  default int getEndPosition(Tree tree) {
    return getEndPosition((JCDiagnostic.DiagnosticPosition) tree);
  }

  default int getEndPosition(JCTree tree) {
    return getEndPosition((JCDiagnostic.DiagnosticPosition) tree);
  }

  int getEndPosition(JCDiagnostic.DiagnosticPosition pos);

  static int getEndPosition(Tree tree, CompilationUnitTree unit) {
    return getEndPosition((JCDiagnostic.DiagnosticPosition) tree, unit);
  }

  static int getEndPosition(JCTree tree, CompilationUnitTree unit) {
    return getEndPosition((JCDiagnostic.DiagnosticPosition) tree, unit);
  }

  static int getEndPosition(JCDiagnostic.DiagnosticPosition pos, CompilationUnitTree unit) {
    return create(unit).getEndPosition(pos);
  }
}
