`@Scope` annotations should be applicable to TYPE (annotating classes that
should be scoped) and to METHOD (annotating `@Provides` methods to apply scoping
to the returned object.

If an annotation's use is restricted by `@Target` and it doesn't include those
two element types, the annotation can't be used where it should be able to be
used.
