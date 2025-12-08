/*
 * Copyright 2014 The Error Prone Authors.
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

import com.sun.tools.javac.tree.JCTree;

/** Describes a tree position with adjustments to the start and end indices. */
public record AdjustedPosition(
    JCTree position, int startPositionAdjustment, int endPositionAdjustment)
    implements ErrorPronePosition {

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
    return position.getPreferredPosition();
  }

  @Override
  public int getEndPosition(ErrorProneEndPosTable endPositions) {
    return endPositions.getEndPosition(position) + endPositionAdjustment;
  }
}
