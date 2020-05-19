---
title: InterfaceWithOnlyStatics
summary: This interface only contains static fields and methods; consider making it a final class instead to prevent subclassing.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Interfaces should be used to define types. Using an interface as a collection of
static methods and fields violates that, and can lead to confusing type
hierarchies if the interface is then implemented to allow easy access to the
constants.

Prefer using a `public final` class instead to prohibit subclassing.

```java
public interface Constants {
  final float PI = 3.14159f;
}
```

```java
public final class Constants {
  public static final float PI = 3.14159f;

  private Constants() {}
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("InterfaceWithOnlyStatics")` to the enclosing element.

