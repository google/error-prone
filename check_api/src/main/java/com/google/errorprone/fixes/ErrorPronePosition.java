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

import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

/** A compatibility wrapper around {@link DiagnosticPosition}. */
public interface ErrorPronePosition extends DiagnosticPosition {
  static ErrorPronePosition from(Tree node) {
    return from((DiagnosticPosition) node);
  }

  static ErrorPronePosition from(JCTree node) {
    return from((DiagnosticPosition) node);
  }

  static ErrorPronePosition from(DiagnosticPosition pos) {
    return new ErrorPronePosition() {
      @Override
      public int getStartPosition() {
        return pos.getStartPosition();
      }

      @Override
      public int getPreferredPosition() {
        return pos.getPreferredPosition();
      }

      @Override
      public JCTree getTree() {
        return pos.getTree();
      }

      @Override
      public int getEndPosition(EndPosTable endPosTable) {
        return pos.getEndPosition(endPosTable);
      }

      @Override
      public int getEndPosition(ErrorProneEndPosTable endPosTable) {
        return endPosTable.getEndPosition(pos);
      }
    };
  }

  @Override
  int getStartPosition();

  @Override
  int getPreferredPosition();

  @Override
  JCTree getTree();

  int getEndPosition(ErrorProneEndPosTable endPosTable);
}
