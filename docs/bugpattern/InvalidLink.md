This error is triggered by a Javadoc `@link` tag that either is syntactically
invalid or can't be resolved. See [javadoc documentation][javadoc] for an
explanation of how to correctly format the contents of this tag.

[javadoc]: https://docs.oracle.com/javase/8/docs/technotes/tools/unix/javadoc.html#JSSOR654

### Linking to generic types

Use the erased type of method parameters in `@link` tags. For example, write
`{@link #foo(List)}` instead of `{@link #foo(List<Bah>)}` and
`{@link #add(Object)}` instead of `{@link #add(E)}`.

### Limitations

This check is very limited in terms of which unresolved links it can be *sure*
are unresolvable. Code within Google is often compiled on a per-package or even
per-file basis, and Error Prone only has visibility into the current
compilation.
