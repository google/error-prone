In a Java `record`, using the accessor method (like `d()`) inside a **compact
constructor** (the one without arguments, `Foo { ... }`) reads the record's
underlying field before it has been set. This means the method will always
return `null` (for objects) or `0`/`false` (for primitives), regardless of what
arguments were passed to the constructor.

## Examples

### Bad: Using the accessor method

```java
record User(String name) {
  User {
    // BUG: name() reads the uninitialized field 'this.name', which is currently
    // null. This throws a NullPointerException immediately.
    if (name().isEmpty()) {
      throw new IllegalArgumentException("Name cannot be empty");
    }
  }
}
```

### Good: Using the parameter name directly

```java
record User(String name) {
  User {
    // CORRECT: Reads the constructor parameter 'name'.
    if (name.isEmpty()) {
      throw new IllegalArgumentException("Name cannot be empty");
    }
  }
}
```

## Explanation

The **Compact Constructor** in Java is a special initialization block that runs
*before* the record's fields are automatically assigned.

1.  **Code Execution:** The code inside your compact constructor `User { ... }`
    runs first.
2.  **Field Assignment:** The compiler automatically assigns the parameters to
    the fields (`this.name = name;`) only *after* your code block finishes
    ([JLS §8.10.4.2](https://docs.oracle.com/javase/specs/jls/se25/html/jls-8.html#jls-8.10.4.2)).

When you call `name()`, it attempts to read `this.name`. Since the assignment
hasn't happened yet, `this.name` still holds its default value, which is `null`
([JLS §4.12.5](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.12.5)).

To fix this, refer to the component by its name (e.g., `name`). This accesses
the **parameter** passed to the constructor, which holds the correct value.
