---
title: MissingDefault
summary: The Google Java Style Guide requires each switch statement includes a default statement group, even if it contains no code.
layout: bugpattern
category: JDK
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The [Google Java Style Guide §4.8.4.3][style] requires each switch statement to
includes a `default` statement group, even if it contains no code. Without it,
the reader does not always know whether execution might silently "fall out" of
the entire block, having executed no code within it. This is undesirable for
most of the same reasons silent fall-through is undesirable.

If the unhandled cases should be impossible, add a `default` clause that throws
`AssertionError`:

```java
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
switch (state) {
  case READY:
    return true;
  case DONE:
    return false;
  default:
    // fall out
}
```

[style]: https://google.github.io/styleguide/javaguide.html#s4.8.4-switch

## Suppression
Suppress false positives by adding an `@SuppressWarnings("MissingDefault")` annotation to the enclosing element.
