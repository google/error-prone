The called method is annotated with `@CompatibleWith`, which enforces that the
parameter passed to the method can potentially be [cast][jls] to the appropriate
generic type. However, the type of the parameter passed can't be cast to the
appropriate generic type.

This is useful when a method can't just take a parameter of the generic type to
allow developers to safely operate with instances held with a wildcard type when
using an instance as both a _consumer_ and _producer_ of values. This should
_not_ be the default, as most interfaces are either one or the other. Containers
and container-like class are the most likely places to use this tool.

TIP: More explanation can be found on the page for [CollectionIncompatibleType]

```java
interface Container<T> {
  void add(T thing);
  boolean contains(@CompatibleWith("T") Object thing);
  boolean containsAsT(T thing);
}

void containmentCheck(Container<? extends Number> container) {
  container.contains(2); // OK, int can be cast to Number
  container.contains(2.0); // OK, double can be cast to Number
  container.contains("a"); // Not OK, String can't be cast to number

  // Does not compile, since, for example, container might be Container<Double>, and Integer
  // can't be cast to Double.
  container.containsAsT(2);

  Container<String> stringContainer = ...;
  stringContainer.contains("a"); // OK
  // OK, since Object *could* be cast to String
  stringContainer.contains(new Object() {});
}
```

[CollectionIncompatibleType]: CollectionIncompatibleType
[jls]: https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.5.1
