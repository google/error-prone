Computing a hashcode for an array is tricky.  Typically you want a hashcode that
depends on the value of each element in the array, but many of the common ways
to do this actually return a hashcode based on the _identity_ of the array
rather than its contents.

This check flags attempts to compute a hashcode from an array that do not take
the contents of the array into account. There are several ways to mess this up:

  * Call the instance `.hashCode()` method on an array.

  * Call the JDK method `java.util.Objects#hashCode()` with an argument of array
    type.

  * Call the JDK method `java.util.Objects#hash()` or the Guava method
    `com.google.common.base.Objects#hashCode()` with multiple arguments, at
    least one of which is an array.

  * Call the JDK method `java.util.Objects#hash()` or the Guava method
    `com.google.common.base.Objects#hashCode()` with a single argument of
    _primitive_ array type. Because these are varags methods that take 
    `Object...`, the primitive array is autoboxed into a single-element Object
    array, and these methods use the identity hashcode of the primitive array
    rather than examining its contents. Note that calling these methods on an
    argument of _Object_ array type actually does the right thing because no
    boxing is needed.

Please use either `java.util.Arrays#hashCode()` (for single-dimensional arrays)
or `java.util.Arrays#deepHashCode()` (for multidimensional arrays) to compute a
hash value that depends on the contents of the array. If you really intended to
compute the identity hash code, consider using
`java.lang.System#identityHashCode()` instead for clarity.
