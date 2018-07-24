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

If you have an immutable class with mutable fields as described above, you can
mark it as such by [suppressing the Immutable check](#suppression) on it. This
will allow your class to be included in other `@Immutable` classes.

For more information about immutability, see:

*   Java Concurrency in Practice §3.4
*   Effective Java §15

## Type Parameters

When an `@Immutable` class has type parameters that are used in the type of that
class's fields, that class is called an immutable generic container. Usages of
immutable generic container classes, such as `ImmutableList`, are only actually
deemed immutable if the arguments to all such type parameters are also deemed
immutable. For example, an `ImmutableList<String>` is deemed immutable since
`String`s are immutable. However, an `ImmutableList<Object>` is not deemed
immutable since `Object`s are not provably immutable.

When creating generic container classes, Error Prone requires that you declare
whether that container is allowed to be used with mutable, or only with
immutable type parameters.

### `@Immutable(containerOf = ...)`

If you want to allow your immutable generic container to possibly contain
mutable types, use `@Immutable`'s `containerOf` method:

```java
@Immutable(containerOf = "T")
class ImmutableHolder<T> {
  final T ref;
  ...
}
```

Error Prone will allow you to instantiate an `ImmutableHolder<String>` and use
it as a field in another `@Immutable` class. You may instantiate an
`ImmutableHolder<Object>`, but since it is mutable, Error Prone would report an
error if that was a field of another `@Immutable` class.

### `@ImmutableTypeParameter`

If you want to allow your `@Immutable` generic container to only contain
immutable types, use `@ImmutableTypeParameter`:

```java
@Immutable
class ImmutableContainer<@ImmutableTypeParameter T> {
  final T ref;
  ...
}
```

Error Prone will allow you to instantiate a `ImmutableContainer<String>` and use
it as a field in another `@Immutable` class. However, it is a compiler error to
instantiate an `ImmutableContainer<Object>`.

You can also use `@ImmutableTypeParameter` to annotate a method's type
parameters:

```java
class SomeMutableClass {
  <@ImmutableTypeParameter T> ImmutableList<T> putInImmutableList(T t) {
    return ImmutableList.of(t);
  }
}
```

### Type Parameters Not Used in Fields

If your `@Immutable` class has a type parameter that is not used in the type of
your class's fields, then there is no need to use `containerOf` or
`@ImmutableTypeParameter`:

```java
@Immutable
class NonContainer<T> {
  ... // No fields whose type contains T
  void process(T element) {
    // process 'element', which won't violate NonContainer's immutability.
  }
}
```

## Suppression

Suppress false positives by adding an `@SuppressWarnings("Immutable")`
annotation to the enclosing element, or the offending field.

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
