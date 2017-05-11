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

/** Describes a tree position with adjustments to the start and end indices. */
public class AdjustedPosition implements DiagnosticPosition {
  protected final JCTree position;
  protected final int startPositionAdjustment;
  protected final int endPositionAdjustment;

  public AdjustedPosition(JCTree position, int startPosAdjustment, int endPosAdjustment) {
    this.position = position;
    this.startPositionAdjustment = startPosAdjustment;
    this.endPositionAdjustment = endPosAdjustment;
  }

  @Override
  public int getStartPosition() {
    return position.getStartPosition() + startPositionAdjustment;
  }

  @Override
  public JCTree getTree() {
    return position;
  }

  @Override
  public int getPreferredPosition() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getEndPosition(EndPosTable endPositions) {
    return position.getEndPosition(endPositions) + endPositionAdjustment;
  }
}
