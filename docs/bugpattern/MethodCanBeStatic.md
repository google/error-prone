Consider a method that doesn't override and is not overrideable. Note first that
such a method should declare only the parameters it actually needs; this is
widely accepted as a good general practice.

But if this is an *instance* method that never refers to `this` (either
explicitly or implicitly), the resulting situation is actually quite similar to
that of the unused parameter. This method is static "in spirit", yet calling it
requires an extra "parameter", so to speak -- a receiver.

Adding an explicit `static` keyword to such a method is conceptually similar to
removing that unused parameter. This has several desirable effects:

*   An actual static method can call your method if it wants to, without having
    to figure out an instance to call it on.
*   It makes it clear (and, indeed, guarantees) that the method's behavior
    relies only on its parameters and not on instance state.

<!-- if we extend this to include package-visible members, then the ability to
     unit-test normally is another advantage. -->

Another effect of adding `static` is that it renders instance fields and methods
inaccessible within the body of the method, so (for example) an auto-completion
feature won't suggest them.

A common reason to omit `static` is when you believe the method may need to
access instance state in the future. However, it usually works fine to just
remove `static` if and when it becomes necessary. You'd have to revisit any
calls that accrued in static contexts, but that's probably no worse than those
callers being out of luck in the first place.

