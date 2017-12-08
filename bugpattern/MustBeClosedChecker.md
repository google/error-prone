---
title: MustBeClosedChecker
summary: The result of this method must be closed.
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Methods or constructors annotated with `@MustBeClosed` require that the returned
resource is closed. This is enforced by checking that invocations occur within
the resource variable initializer of a try-with-resources statement:

```java
try (AutoCloseable resource = createTheResource()) {
  doSomething(resource);
}
```

or the `return` statement of another method annotated with `@MustBeClosed`:

```java
@MustBeClosed
AutoCloseable createMyResource() {
  return createTheResource();
}
```

To support legacy code, the following pattern is also supported:

```java
AutoCloseable resource = createTheResource();
try {
  doSomething(resource);
} finally {
  resource.close();
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MustBeClosedChecker")` to the enclosing element.
