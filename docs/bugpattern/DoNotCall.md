This check prevents calls to methods annotated with Error Prone's `@DoNotCall`
annotation (`com.google.errorprone.annotations.DoNotCall`).

The annotation can be used on methods that are required to satisfy the contract
of an interface, but that are not supported.

A method annotated with `@DoNotCall` should always be `final` or `abstract`. If
an `abstract` method is annotated `@DoNotCall` Error Prone will ensure all
implementations of that method also have the annotation.

TIP: Marking methods annotated with `@DoNotCall` as `@Deprecated` is
recommended, since it provides IDE users with more immediate feedback.

Example:

`java.util.Collection#add` should never be called on an immutable collection
implementation:

```java
package com.google.common.collect.ImmutableList;

class ImmutableList<E> implements List<E> {

 // ...

 /**
  * Guaranteed to throw an exception and leave the list unmodified.
  *
  * @deprecated Unsupported operation.
  */
 @Deprecated
 @DoNotCall("guaranteed to throw an exception and leave the list unmodified")
 @Override
 public final void add(E e) {
   throw new UnsupportedOperationException();
 }
}
```
