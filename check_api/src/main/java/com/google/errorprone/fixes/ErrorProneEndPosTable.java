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

import static java.util.Objects.requireNonNull;

import com.google.common.base.Throwables;
import com.google.errorprone.SourcePositionException;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Position;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import org.jspecify.annotations.Nullable;

/** A compatibility wrapper around {@code EndPosTable}. */
public interface ErrorProneEndPosTable {

  @Nullable MethodHandle GET_END_POS_HANDLE = getEndPosMethodHandle();

  MethodHandle GET_END_POS_WITH_UNIT_HANDLE = getEndPosWithUnitMethodHandle();

  private static @Nullable MethodHandle getEndPosMethodHandle() {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    try {
      // JDK versions after https://bugs.openjdk.org/browse/JDK-8372948
      // pos -> pos.getEndPosition()
      return lookup.findVirtual(
          JCDiagnostic.DiagnosticPosition.class,
          "getEndPosition",
          MethodType.methodType(int.class));
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }

  private static MethodHandle getEndPosWithUnitMethodHandle() {
    if (GET_END_POS_HANDLE != null) {
      // JDK versions after https://bugs.openjdk.org/browse/JDK-8372948
      // (pos, unit) -> pos.getEndPosition()
      return MethodHandles.dropArguments(GET_END_POS_HANDLE, 1, JCCompilationUnit.class);
    }
    MethodHandles.Lookup lookup = MethodHandles.lookup();
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
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  static void checkExplicitSource(Tree tree) {
    checkExplicitSource((JCDiagnostic.DiagnosticPosition) tree);
  }

  static void checkExplicitSource(JCTree tree) {
    checkExplicitSource((JCDiagnostic.DiagnosticPosition) tree);
  }

  static void checkExplicitSource(JCDiagnostic.DiagnosticPosition pos) {
    if (pos == null) {
      return;
    }
    if (GET_END_POS_HANDLE == null) {
      // End positions aren't stored directly on the tree, so skip validation to avoid having
      // to plumb an EndPosTable through fix creation, and instead rely on validation later when
      // fixes are applied.
      // TODO: cushon - perform eager validation once JDK 27 is the minimum supported JDK
      return;
    }
    int start = pos.getStartPosition();
    int end;
    try {
      end = (int) GET_END_POS_HANDLE.invoke(pos);
    } catch (Throwable e) {
      Throwables.throwIfUnchecked(e);
      throw new AssertionError(e);
    }
    if (start == Position.NOPOS || end == Position.NOPOS || start == end) {
      throw new SourcePositionException(start, end);
    }
  }

  static ErrorProneEndPosTable create(CompilationUnitTree unit) {
    MethodHandle getEndPosition =
        MethodHandles.insertArguments(GET_END_POS_WITH_UNIT_HANDLE, 1, unit);
    return pos -> {
      requireNonNull(pos);
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
