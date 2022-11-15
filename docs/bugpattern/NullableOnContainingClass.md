The correct syntax to apply a `TYPE_USE` annotation to an inner class is
`A.@Nullable B`.

For a `TYPE_USE` `@Nullable` annotation, `@Nullable A.B` is legal Java if `B` is
a non-static inner class:

```java
class A {
  @Target(TYPE_USE)
  @interface Nullable {}

  class B {}
  static class C {}

  void test(A.@Nullable B x) {} // B is annotated ('A' is the enclosing instance type)
  void test(A.@Nullable C x) {} // C is annotated ('A' is a 'scoping construct' here)
}
```

```java
  void test(@Nullable A.B x) {} // compiles, but likely incorrect: annotates the enclosing instance type 'A', which can never be null
  void test(@Nullable A.C x) {} // compile error: 'A' cannot be annotated
```

However, for `@Nullable` (and `@NonNull`, and friends), annotating the outer
class is meaningless. The reference to the outer class (`A.this`) can never be
`null`, so any nullability annotations are redundant.
