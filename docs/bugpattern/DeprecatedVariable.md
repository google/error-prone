`@Deprecated` annotations should not be applied to local variables and
parameters, since they have no effect there.

The
[javadoc for `@Deprecated`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Deprecated.html)
says

> Use of the `@Deprecated` annotation on a local variable declaration or on a
> parameter declaration or a package declaration has no effect on the warnings
> issued by a compiler.
