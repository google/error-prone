/*
 * Copyright 2021 The Error Prone Authors.
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

/** A {@link DiagnosticPosition} with a fixed position. */
public final class FixedPosition implements DiagnosticPosition {
  private final JCTree tree;
  private final int startPosition;

  public FixedPosition(Tree tree, int startPosition) {
    this.tree = (JCTree) tree;
    this.startPosition = startPosition;
  }

  @Override
  public JCTree getTree() {
    return tree;
  }

  @Override
  public int getStartPosition() {
    return startPosition;
  }

  @Override
  public int getPreferredPosition() {
    return startPosition;
  }

  @Override
  public int getEndPosition(EndPosTable endPosTable) {
    return startPosition;
  }
}
