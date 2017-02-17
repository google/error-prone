A non-override, non-overrideable method that never accesses `this` (explicitly
or implicitly) is already a static method in spirit. By adding `static`
explicitly,

*   Actual static methods will be able to call it (without conjuring up an
    instance of your type unnecessarily), which is reasonable for them to do.
*   Instance fields and methods will become inaccessible within the body of the
    method, so (for example) your IDE's autocompletion feature won't suggest
    them.

<!-- if we extend this to include package-visible members, then the ability to
     unit-test normally is another advantage. -->

Of course, if the method develops a need to access instance state in the future,
just remove the `static` keyword at that time. If any calls had accrued in
static contexts, you'll have to revisit them, but that should be no worse than
if those callers were unable to use the method in the first place.

