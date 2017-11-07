---
title: FieldMissingNullable
summary: Fields that can be null should be annotated @Nullable
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
Fields that may be `null` should be annotated with `@Nullable`. For example, do
this:

```java {.good}
public class Foo {
  @Nullable private String message = "hello";
  public void reset() {
    message = null;
  }
}
```

Not this:

```java {.bad}
public class Foo {
  private String message = "hello";
  public void reset() {
    message = null;
  }
}
```

## Suppression
Suppress false positives by adding an `@SuppressWarnings("FieldMissingNullable")` annotation to the enclosing element.
