There are various libraries available to check if a variable is null or not.
Most notable ones are : * com.google.common.base.Preconditions.checkNotNull *
com.google.common.base.Verify.verifyNotNull * java.util.Objects.requireNonNull

These methods generally takes two arguments. The first is the reference that
should be non-null. The second is the error message to print (usually a string
literal).

There are some common scenarios where the first argument can't be null but is
still checked to be null and is thus redundant.

*   By specification, a constructor cannot return null. So if the argument is an
    object creation or array creation expression, checking can be skipped.

*   Often the order of the two arguments is swapped, and the reference is never
    actually checked for nullity. This check ensures that the first argument to
    such methods is not a literal.

*   When a primitive is passed as the argument to check, the primitive will be
    [autoboxed](http://docs.oracle.com/javase/7/docs/technotes/guides/language/autoboxing.html)
    into a boxed object, which is non-null, causing the check to always pass
    without the condition being evaluated. If the intent was to ensure that the
    primitive met some criterion (e.g., a boolean that should be non-null),
    please use `Preconditions.checkState()` or `Preconditions.checkArgument()`
    instead.
