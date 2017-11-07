---
title: ReturnMissingNullable
summary: Methods that can return null should be annotated @Nullable
layout: bugpattern
tags: ''
severity: SUGGESTION
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Methods that may return `null` should be annotated with `@Nullable`. For
example, do this:

```java {.good}
public class Foo {
  @Nullable private String message = null;
  @Nullable public String getMessage() {
    return message;
  }
}
```

Not this:

```java {.bad}
public class Foo {
  private String message = null;
  public String getMessage() {
    return message;
  }
}
```

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ReturnMissingNullable")` annotation to the enclosing element.
