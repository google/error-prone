All fields in your enum class should be final and either be primitive or refer
to deeply immutable objects.

Static state is dangerous to begin with, but much worse for enums. We all think
of enum values as constants -- and even refer to them as "enum constants" -- and
would be very surprised if any of their state ever changed, or was not
thread-safe.

To make enums immutable, ensure:

*   All fields are final.
*   The types of all fields of the enum are deeply immutable. For example, use
    `ImmutableList` and `ImmutableSet` instead of `List` and `Set`.

    *   Types are considered immutable if they are primitives, in a set of types
        that are built in to Error Prone (e.g. `java.lang.String`,
        `java.util.UUID`), or are annotated with
        `com.google.errorprone.annotations.Immutable`.
    *   If the type you're using inside the enum can be annotated with
        `@Immutable`, you should do that:

        ```java
        // WARNING: E is not immutable, since MyValueObject isn't Immutable
        enum E {
          private final MyValueObject mvo = new MyValueObject();
        }

        // Add @Immutable here
        final class MyValueObject {}
        ```

        Note that MyValueObject must actually be immutable. The
        [Immutable](Immutable.md) checker will raise a compile-time error if the
        `MyValueObject` class isn't actually immutable.

    *   If the type you're using inside the enum is not considered immutable,
        and you can't annotate the type because it's outside the project,
        consider using an immutable replacement of the type, or
        [suppress this check on the enum](#suppression) with a comment about why
        the fields in question are immutable.

TIP: annotating the declaration of the enum class with `@Immutable` is
unnecessary -- Error Prone assumes enums are immutable by default.

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
  A("A", ImmutableList.of(new Foo(1), new Foo(2))),
  B("B", ImmutableList.of(new Foo(3)));

  // All fields are final, and deeply immutable:
  // Error Prone knows String is immutable.
  private final String name;
  // Error Prone knows ImmutableList<T> is an immutable collection of some
  // objects, and it recognizes the @Immutable annotation on the declaration of
  // Foo, so it can safely determine that this ImmutableList is deeply
  // immutable.
  private final ImmutableList<Foo> foos;

  private E(String name, ImmutableList<Foo> foos) {
    this.name = name;
    this.foos = foos;
  }

  public ImmutableList<Foo> foos() {
    return foos;
  }

  public String name() {
    return foos;
  }

}
```

TIP: Instead of creating an enum with functional interface fields (`Predicate`,
`Function`, etc.), declare abstract methods that are overridden by each
constant. For example, do this:

```java {.good}
enum Types {
  STRING {
    @Override public boolean hasCompatibleType(Object o) {
      return o instanceof String;
    }
  },
  NUMBER {
    @Override public boolean hasCompatibleType(Object o) {
      return o instanceof Number;
    }
  },
  // ...

  public abstract boolean hasCompatibleType(Object o);
}
```

... not this:

```java {.bad}
enum Types {
  STRING(o -> o instanceof String),
  NUMBER(o -> o instanceof Number),
  // ...

  final Predicate<Object> hasCompatibleType;

  Types(Predicate<Object> hasCompatibleType) {
    this.hasCompatibleType = hasCompatibleType;
  }
}
```

This has several advantages on top of sidestepping this checker, e.g. not tying
you to a particular functional interface type -- your callers should e.g. use
`STRING::hasCompatibleType` instead of `STRING.hasCompatibleType` which only
works for one interface type.

