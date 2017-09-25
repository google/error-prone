This check validates that all classes annotated with Error Prone's `@Immutable`
annotation (`com.google.errorprone.annotations.Immutable`) are deeply immutable.
It also checks that any class extending an `@Immutable`-annotated class or
implementing an `@Immutable`-annotated interface are also immutable.

Other versions of the annotation, such as
`javax.annotation.concurrent.Immutable`, are currently *not* enforced.

An object is immutable if its state cannot be observed to change after
construction. Immutable objects are inherently thread-safe.

A class is immutable if all instances of that class are immutable. The
immutability of a class can only be fully guaranteed if the class is final,
otherwise one must ensure all subclasses are also immutable.

A conservative definition of object immutability is:

*   All fields are final;
*   All reference fields are of immutable type, or null;
*   It is *properly constructed* (the `this` reference does not escape the
    constructor).

The requirement that all reference fields be immutable ensures *deep*
immutability, meaning all contained state is also immutable. A weaker property,
common with container classes, is *shallow* immutability, which allows some of
the object's fields to point to mutable objects. One example of shallow
immutability is guava's ImmutableList, which may contain mutable elements.

It is possible to implement immutable classes with some internal mutable state,
as long as callers can never observe changes to that state. For example, some
state may be lazily initialized to improve performance.

It is also technically possible to have an immutable object with non-final
fields (see the implementation of `String#hashCode()` for an example), but doing
this correctly requires subtle reasoning about safe data races and deep
knowledge of the Java Memory Model.

For more information about immutability, see:

*   Java Concurrency in Practice §3.4
*   Effective Java §15

## Suppression

Suppress false positives by adding an `@SuppressWarnings("Immutable")`
annotation to the enclosing element.

To suppress warnings in AutoValue classes, add `@AutoValue.CopyAnnotations` to
ensure the suppression is also applied to the generated sub-class:

```java
@AutoValue
@AutoValue.CopyAnnotations
@Immutable
@SuppressWarnings("Immutable")
class MyAutoValue {
  ...
}
```
