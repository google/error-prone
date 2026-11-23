---
title: AlmostJavadoc
summary: This comment contains Javadoc or HTML tags, but isn't started with a double
  asterisk (/**); is it meant to be Javadoc?
layout: bugpattern
tags: Style
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
This comment contains Javadoc or HTML tags, and is in the right position for a
Javadoc, but doesn't start with a double asterisk. Should it be a Javadoc?

```java
/* Frobnicates the {@link Foo}s. */
class Frobnicator {
  Foo frobnicate(Foo foo);
}
```

```java
/** Frobnicates the {@link Foo}s. */
class Frobnicator {
  Foo frobnicate(Foo foo);
}
```

## Suppression

Suppress by applying `@SuppressWarnings("AlmostJavadoc")` to the element being
documented (or not documented).

