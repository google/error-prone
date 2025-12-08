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

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic;

/** A compatibility wrapper around {@code EndPosTable}. */
public interface ErrorProneEndPosTable {

  static ErrorProneEndPosTable create(CompilationUnitTree unit) {
    com.sun.tools.javac.tree.EndPosTable endPosTable =
        ((JCTree.JCCompilationUnit) unit).endPositions;
    return pos -> pos.getEndPosition(endPosTable);
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
