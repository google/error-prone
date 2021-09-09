This error is triggered by a Javadoc `@link` tag that either is syntactically
invalid or can't be resolved. See [javadoc documentation][javadoc] for an
explanation of how to correctly format the contents of this tag.

https://docs.oracle.com/javase/8/docs/technotes/tools/unix/javadoc.html#JSSOR654

### Linking to generic types

Use the erased type of method parameters in `@link` tags. For example, write
`{@link #foo(List)}` instead of `{@link #foo(List<Bah>)}`. Javadoc does yet not
support generics in `@link` tags, due to a bug:
[JDK-5096551](https://bugs.openjdk.java.net/browse/JDK-5096551).
