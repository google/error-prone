Non-record concrete `value` classes ([JEP 401][jep-401]) should explicitly
override `equals` and `hashCode`.

Unlike a `value record`—which automatically synthesizes component-wise `equals`
and `hashCode` methods that call `.equals()` and `.hashCode()` on reference
components—a non-record `value class` that omits `equals` and `hashCode`
inherits `Object.equals` (`return this == obj;`) and `Object.hashCode`
(`System.identityHashCode(this)`).

For a `value class`, `Object.equals` and `this == obj` perform a state-wise
identity comparison of the class's fields:

*   **Reference fields are compared by identity**: Any reference field (such as
    `String`, collections, or arrays) is compared using reference equality
    (`==`) rather than `.equals()`, and hashed using `System.identityHashCode`
    rather than `.hashCode()`.
*   **Representation leakage**: Any private representation details—such as
    defensive array copies, unnormalized state (e.g., `Rational(1, 2)` vs.
    `Rational(2, 4)`), or internal fields that should not participate in
    equality—are directly exposed in equality comparisons.
*   **Evolution hazard**: Even if a `value class` initially contains only
    primitive or `enum` fields where `Object.equals` happens to match logical
    equality, omitting `equals` and `hashCode` creates a hazard when the class
    evolves: adding a reference field (such as `String`) or an auxiliary field
    later will silently break `.equals()` and `.hashCode()`. Requiring an
    explicit definition upfront ensures equality semantics remain intentional as
    fields change.

### How to fix

If the class is a simple data carrier whose equality is defined by its
components, prefer a `value record`:

```java
value record UserId(String id) {}
```

If the class requires encapsulation (such as private fields, normalization, or
defensive copies), explicitly implement `equals` and `hashCode`:

```java
value class UserId {
  private final String id;

  UserId(String id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof UserId other && Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
```

If all fields of the `value class` are primitives or enums and state-wise
comparison via `Object.equals` / `Object.hashCode` is genuinely intended,
delegate explicitly so that the choice is clear to readers:

```java
value class Meters {
  private final int value;

  Meters(int value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
```

[jep-401]: https://openjdk.org/jeps/401
