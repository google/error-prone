If a conditional expression evalutes to `null`, unboxing it will result in a
`NullPointerException`.

For example:

```java
int x = flag ? foo : null:
```

If `flag` is false, `null` will be auto-unboxed from an `Integer` to `int`,
resulting in a NullPointerException.
