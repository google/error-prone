When declaring type parameters, it's possible to declare a type parameter with
the same name as another type in scope, "shadowing" that type and potentially
causing confusing or unintended behavior.

```java
class Bar {
  ...
  public void doSomething(T object) {
    // Here, object is the static class T in this file
  }

  public <T> void doSomethingElse(T object) {
    // Here, object is a generic T
  }
  ...
  public static class T {...}
}
```

This checker warns when a type parameter shadows another type and suggests a
possible renaming for the type parameter.

Note, however, that in some cases it may be preferable to rename or delete the
shadowed type rather than the type parameter shadowing it, such as in cases
where the type parameter is always instantiated with the same type.
