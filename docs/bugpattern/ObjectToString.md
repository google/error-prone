Calling `toString` on objects that don't override `toString()` doesn't provide
useful information (just the class name and the `hashCode()`).

Consider overriding toString() function to return a meaningful String describing
the object.
