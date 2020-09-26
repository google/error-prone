Classes that override `hashCode()` should consider also overriding `toString()`
with details to aid debugging and diagnostics, instead of relying on the default
`Object.toString()` implementation.

`Object.toString()` returns a string consisting of the class' name and the
instances' hash code. When `hashCode()` is overridden this can be misleading, as
users typically expect this default `toString()` to be (semi)unique
per-instance, especially when debugging.

See also
[`MoreObjects.toStringHelper()`](https://guava.dev/releases/snapshot/api/docs/com/google/common/base/MoreObjects.html#toStringHelper-java.lang.Object-)
