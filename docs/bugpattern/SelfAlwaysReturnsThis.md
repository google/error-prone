A common pattern for abstract `Builders` is to declare an instance method named
`self()`, which subtypes override and implement as `return this` (see Effective
Java 3rd Edition, Item 2).

Returning anything other than `this` from an instance method named `self()` with
a return type that matches the enclosing class will be confusing for readers and
callers.

## Casting

If an unchecked cast is required, use a single-statement cast, with the
suppression on the method (rather than the statement). For example

```java
  @SuppressWarnings("unchecked")
  default U self() {
    return (U) this;
  }
```

Instead of:

```java
  default U self() {
    @SuppressWarnings("unchecked")
    U self = (U) this;
    return self;
  }
```
