The following code is fine in Java 7, but poses a problem in Java 8.

```java
static class A {
  B c(D d) { return null; }
  static B c(A a, D d) { return null; }
}
```

The method reference `A::c` of the instance method `c` has an implicit
first parameter for the `this` pointer. So both methods that `A::c` could
resolve to are compatible with `BiFunction<A, D, B>`, and the method
reference is ambiguous.

```java
void f(BiFunction<A, D, B> f) { ... }
```

```
error: incompatible types: invalid method reference
    f(A::c);
      ^
    reference to c is ambiguous
      both method c(A,D) in A and method c(D) in A match
```

Consider renaming one of the methods to avoid the ambiguity.
