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
