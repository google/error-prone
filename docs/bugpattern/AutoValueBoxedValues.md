AutoValue classes reject `null` values, unless the property is annotated with
`@Nullable`. For this reason, the usage of boxed primitives (e.g. `Long`) is
discouraged, except when annotated as `@Nullable`. Otherwise they can be
replaced with the corresponding primitive. There could be some cases where the
usage of a boxed primitive might be intentional to avoid boxing the value again
after invoking the getter.

## Suppression

Suppress violations by using `@SuppressWarnings("AutoValueBoxedValues")` on the
relevant `abstract` getter and/or setter.
