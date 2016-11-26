If a method's formal parameter is annotated with `@CompileTimeConstant`, the
method will always be invoked with an argument that is a static constant. If the
parameter itself is non-final, then it is a mutable reference to immutable data.
This is rarely useful, and can be confusing when trying to use the parameter in
a context that requires an compile-time constant. For example:

```java
void f(@CompileTimeConstant y) {}
void g(@CompileTimeConstant x) {
  x = f(x); // x is not a constant
}
```

