Consider using `requireNonNull(value)` which is shorter and more readable than

```
if (value == null) {
  throw new NullPointerException(...);
}
```

The main reason to prefer `requireNonNull` is for readability, but it has no
performance downsides and may have a small performance benefit.

The method is annotated with `@ForceInline` and receives special treatment from
the JVM to ensure it is inlined into equivalent code to the `if`/`throw`. It
also results in slightly smaller Java bytecode, which can improve other JIT
inlining decisions.
