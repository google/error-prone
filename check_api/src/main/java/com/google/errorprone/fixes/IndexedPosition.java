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

package com.google.errorprone.fixes;

import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

/** Describes a position that only has a start and end index. */
public class IndexedPosition implements DiagnosticPosition {

  final int startPos;
  final int endPos;

  public IndexedPosition(int startPos, int endPos) {
    this.startPos = startPos;
    this.endPos = endPos;
  }

  @Override
  public JCTree getTree() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getStartPosition() {
    return startPos;
  }

  @Override
  public int getPreferredPosition() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getEndPosition(EndPosTable endPosTable) {
    return endPos;
  }
}
