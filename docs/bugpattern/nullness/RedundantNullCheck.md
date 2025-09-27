A null check (e.g., `x == null` or `x != null`) is redundant if it is performed
on an expression that is statically determined to be non-null according to
language semantics or nullness annotations. This check can optionally be
configured to flag redundant calls to `Objects.requireNonNull(x)` as well.

Within a `@NullMarked` scope, types are non-null by default unless explicitly
annotated with `@Nullable`. Therefore, checking a variable or method return
value (that isn't `@Nullable`) for nullness is unnecessary, as it should never
be null.

Example:

```java
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class MyClass {
  void process(String definitelyNonNull) {
    // BUG: Diagnostic contains: RedundantNullCheck
    if (definitelyNonNull == null) {
      System.out.println("This will never happen");
    }
    // ...
  }

  String getString() {
    return "hello";
  }

  @Nullable String getNullableString() {
    return null;
  }

  void anotherMethod() {
    String s = getString();
    // BUG: Diagnostic contains: RedundantNullCheck
    if (s == null) {
      // s is known to be non-null because getString() is not @Nullable
      // and we are in a @NullMarked scope.
      System.out.println("Redundant check");
    }

    String nullableStr = getNullableString();
    if (nullableStr == null) { // This check is NOT redundant
      System.out.println("Nullable string might be null");
    }
  }
}
```

This check helps to clean up code and reduce clutter by removing unnecessary
null checks, making the code easier to read and maintain. It also reinforces the
contract provided by `@NullMarked` and `@Nullable` annotations.

## Configuration

By default, this check only flags redundant null checks using `== null` and `!=
null`. To also flag redundant calls to `Objects.requireNonNull(x)`, enable it
with the following flag:

```
-XepOpt:RedundantNullCheck:CheckRequireNonNull=true
```

## Suppression

Suppress false positives by adding the suppression annotation
`@SuppressWarnings("RedundantNullCheck")` to the enclosing element.
