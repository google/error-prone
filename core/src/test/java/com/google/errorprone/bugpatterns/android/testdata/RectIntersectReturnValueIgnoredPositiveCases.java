/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.android.testdata;

import android.graphics.Rect;

/** @author avenet@google.com (Arnaud J. Venet) */
public class RectIntersectReturnValueIgnoredPositiveCases {
  void checkSimpleCall(Rect rect, int aLeft, int aTop, int aRight, int aBottom) {
    // BUG: Diagnostic contains: Return value of android.graphics.Rect.intersect() must be checked
    rect.intersect(aLeft, aTop, aRight, aBottom);
  }

  void checkOverload(Rect rect1, Rect rect2) {
    // BUG: Diagnostic contains: Return value of android.graphics.Rect.intersect() must be checked
    rect1.intersect(rect2);
  }

  class RectContainer {
    int xPos;
    int yPos;
    Rect rect;

    boolean intersect(int length, int width) {
      // BUG: Diagnostic contains: Return value of android.graphics.Rect.intersect() must be checked
      rect.intersect(xPos, yPos, xPos + length, yPos + width);
      return true;
    }
  }

  void checkInMethod(int length, int width) {
    RectContainer container = new RectContainer();
    container.intersect(length, width);
  }

  void checkInField(RectContainer container) {
    // BUG: Diagnostic contains: Return value of android.graphics.Rect.intersect() must be checked
    container.rect.intersect(
        container.xPos, container.yPos, container.xPos + 10, container.yPos + 20);
  }
}
