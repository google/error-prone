Consider a method that doesn't override, is not overrideable, and never refers
to `this` either explicitly or implicitly. Such a method is already a static
method "in spirit"; it has simply disallowed static calls, for reasons that are
often not clear.

Adding an explicit `static` to such a method has several effects that you might
find desirable:

*   An actual static method will be able to call your method if it wants to,
    without having to figure out an instance to call it on.
*   It makes it clear that the method's behavior relies only on its parameters
    and not on instance state.
*   Instance fields and methods will be inaccessible within the body of the
    method, so (for example) while editing that code an auto-completion feature
    won't suggest them.

<!-- if we extend this to include package-visible members, then the ability to
     unit-test normally is another advantage. -->

If the method needs to access instance state in the future, just remove the
`static` keyword at that time. You'll have to revisit any calls that accrued in
static contexts, but that's probably no worse than those callers being out of
luck in the first place.

