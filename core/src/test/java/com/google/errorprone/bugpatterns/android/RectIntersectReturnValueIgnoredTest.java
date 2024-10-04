/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.android;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author avenet@google.com (Arnaud J. Venet)
 */
@RunWith(JUnit4.class)
public class RectIntersectReturnValueIgnoredTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(RectIntersectReturnValueIgnored.class, getClass())
          .addSourceLines(
              "Rect.java",
              """
              package android.graphics;

              public class Rect {
                public boolean intersect(int x, int y, int x2, int y2) {
                  return false;
                }

                public boolean intersect(Rect other) {
                  return false;
                }

                public void setEmpty() {}
              }""")
          .setArgs(ImmutableList.of("-XDandroidCompatible=true"));

  @Test
  public void positiveCases() {
    compilationHelper
        .addSourceLines(
            "RectIntersectReturnValueIgnoredPositiveCases.java",
            """
package com.google.errorprone.bugpatterns.android.testdata;

import android.graphics.Rect;

/**
 * @author avenet@google.com (Arnaud J. Venet)
 */
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
}""")
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "RectIntersectReturnValueIgnoredNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.android.testdata;

            import android.graphics.Rect;

            /**
             * @author avenet@google.com (Arnaud J. Venet)
             */
            public class RectIntersectReturnValueIgnoredNegativeCases {
              boolean checkSimpleCall(Rect rect, int aLeft, int aTop, int aRight, int aBottom) {
                return rect.intersect(aLeft, aTop, aRight, aBottom);
              }

              boolean checkOverload(Rect rect1, Rect rect2) {
                return rect1.intersect(rect2);
              }

              void checkInTest(Rect rect, int aLeft, int aTop, int aRight, int aBottom) {
                if (!rect.intersect(aLeft, aTop, aRight, aBottom)) {
                  rect.setEmpty();
                }
              }

              class InternalScope {
                class Rect {
                  int left;
                  int right;
                  int top;
                  int bottom;

                  boolean intersect(int aLeft, int aTop, int aRight, int aBottom) {
                    throw new RuntimeException("Not implemented");
                  }
                }

                void checkHomonym(Rect rect, int aLeft, int aTop, int aRight, int aBottom) {
                  rect.intersect(aLeft, aTop, aRight, aBottom);
                }
              }

              class RectContainer {
                int xPos;
                int yPos;
                Rect rect;

                boolean intersect(int length, int width) {
                  return rect.intersect(xPos, yPos, xPos + length, yPos + width);
                }
              }

              void checkInMethod(int length, int width) {
                RectContainer container = new RectContainer();
                container.intersect(length, width);
              }
            }""")
        .doTest();
  }
}
