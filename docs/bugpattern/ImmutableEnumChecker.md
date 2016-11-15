All fields in your enum class should be final and either be primitive or refer
to deeply immutable objects.

Static state is dangerous to begin with, but much worse for enums. We all think
of enum values as constants -- and even refer to them as "enum constants" -- and
would be very surprised if any of their state ever changed, or was not
thread-safe.

To make enums immutable, ensure:

*   All fields are final.
*   The types of all fields are deeply immutable. For example, use
    `ImmutableList` and `ImmutableSet` instead of `List` and `Set`.
*   To signal to Error Prone that the type of a field is immutable, add
    `com.google.errorprone.annotations.Immutable` to its declaration.

TIP: annotating the declaration of an enum with `@Immutable` is unnecessary --
Error Prone assumes enums are immutable by default.

Example:

```java
import com.google.errorprone.annotations.Immutable;

@Immutable
class Foo {
  final int id;
  public Foo(int id) {
    this.id = id;
  }
}
```

```java
// The declaration doesn't need to be annotated with @Immutable.
enum E {
  A(ImmutableList.of(new Foo(1), new Foo(2))),
  B(ImmutableList.of(new Foo(3)));

  // All fields are final, and deeply immutable: Error Prone knows
  // ImmutableList is immutable, and it recognizes the @Immutable
  // annotation on the declaration of Foo.
  private final ImmutableList<Foo> foos;

  private E(ImmutableList<Foo> foos) {
    this.foos = foos;
  }

  public ImmutableList<Foo> foos() {
    return foos;
  }
}
```

