`Preconditions.checkNotNull()` takes as an argument a reference that should be
non-null. Often a primitive is passed as the argument to check. The primitive
will be
[autoboxed](http://docs.oracle.com/javase/7/docs/technotes/guides/language/autoboxing.html)
into a boxed object, which is non-null, causing the check to always pass without
the condition being evaluated.

If the intent was to ensure that the primitive met some criterion (e.g., a
boolean that should be non-null), please use `Preconditions.checkState()` or
`Preconditions.checkArgument()` instead.
