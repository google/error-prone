The [Google Java Style Guide §4.8.4.3][style] requires each switch statement to
include a `default` statement group, even if it contains no code.

NOTE: A switch statement for an `enum` type may omit the `default` statement
group, if it includes explicit cases covering all possible values of that type.
See [MissingCasesInEnumSwitch] for more information.

Without a default, the reader does not always know whether execution might
silently "fall out" of the entire block, having executed no code within it. This
is undesirable for most of the same reasons silent fall-through is undesirable.

If the unhandled cases should be impossible, add a `default` clause that throws
`AssertionError`:

```java
enum State { READY, DONE, RUNNING, BLOCKED }

switch (state) {
  case READY:
    return true;
  case DONE:
    return false;
  default:
    throw new AssertionError("unexpected state: " + state);
}
```

If having execution fall out of the switch is intentional, add a `default`
clause with a comment:

```java
enum State { READY, DONE, RUNNING, BLOCKED }

switch (state) {
  case READY:
    return true;
  case DONE:
    return false;
  default: // continue below to handle RUNNING/BLOCKED/etc.
}
```

[style]: https://google.github.io/styleguide/javaguide.html#s4.8.4.3-switch-default
[MissingCasesInEnumSwitch]: https://errorprone.info/bugpattern/MissingCasesInEnumSwitch
