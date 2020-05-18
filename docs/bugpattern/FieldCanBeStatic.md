`final` fields initialized with a literal can elide a per-instance reference by
adding the `static` keyword. Since the field is `final` it is unmodifiable and
since its initializer is a literal the value is immutable and thus sharing a
per-class field is safe. This also allows a simpler constant load bytecode
instead of a field lookup.

That is, prefer this:

```java
static final String string = "string";
static final int number = 42;
```

Not this:

```java
final String string = "string";
final int number = 42;
```
