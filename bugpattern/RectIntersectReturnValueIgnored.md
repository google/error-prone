---
title: RectIntersectReturnValueIgnored
summary: Return value of android.graphics.Rect.intersect() must be checked
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
`android.graphics.Rect.intersect(Rect r)` and `android.graphics.Rect.intersect(int, int, int, int)` do not always modify the rectangle to the intersected result. If the rectangles do not intersect, no change is made and the original rectangle is not modified. These methods return false to indicate that this has happened.

If you don’t check the return value of these methods, you may end up drawing the wrong rectangle.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("RectIntersectReturnValueIgnored")` annotation to the enclosing element.

----------

### Positive examples
__RectIntersectReturnValueIgnoredPositiveCases.java__

{% highlight java %}
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
{% endhighlight %}

### Negative examples
__RectIntersectReturnValueIgnoredNegativeCases.java__

{% highlight java %}
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
}
{% endhighlight %}

