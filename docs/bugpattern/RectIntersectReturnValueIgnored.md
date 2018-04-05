`android.graphics.Rect.intersect(Rect r)` and
`android.graphics.Rect.intersect(int, int, int, int)` do not always modify the
rectangle to the intersected result. If the rectangles do not intersect, no
change is made and the original rectangle is not modified. These methods return
false to indicate that this has happened.

If you don’t check the return value of these methods, you may end up drawing the
wrong rectangle.
