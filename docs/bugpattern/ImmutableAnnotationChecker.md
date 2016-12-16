Annotations should always be immutable.

Static state is dangerous to begin with, but much worse for annotations.
Annotation instances are usually constants, and it is very surprising if their
state ever changes, or if they are not thread-safe.

TIP: prefer [`@AutoAnnotation`] to writing annotation implementations by hand.

[`AutoAnnotation`]: https://github.com/google/auto/blob/master/value/src/main/java/com/google/auto/value/AutoAnnotation.java

To make annotation implementations immutable, ensure:

*   All fields are final.
*   The types of all fields are deeply immutable. For example, use
    `ImmutableList` and `ImmutableSet` instead of `List` and `Set`.
*   To signal to Error Prone that the type of a field is immutable, add
    `com.google.errorprone.annotations.Immutable` to its declaration.
*   If the implementation is a member class, ensure it is static to avoid
    capturing state from a mutable enclosing instance. If it is an anonymous
    class, convert it to a named member class so it can be made static.

TIP: annotating the declaration of an enum with `@Immutable` is unnecessary --
Error Prone assumes annotations are immutable by default.
