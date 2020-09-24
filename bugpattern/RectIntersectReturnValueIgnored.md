---
title: RectIntersectReturnValueIgnored
summary: Return value of android.graphics.Rect.intersect() must be checked
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
`android.graphics.Rect.intersect(Rect r)` and
`android.graphics.Rect.intersect(int, int, int, int)` do not always modify the
rectangle to the intersected result. If the rectangles do not intersect, no
change is made and the original rectangle is not modified. These methods return
false to indicate that this has happened.

If you don’t check the return value of these methods, you may end up drawing the
wrong rectangle.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("RectIntersectReturnValueIgnored")` to the enclosing element.
