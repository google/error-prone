---
title: OrphanedFormatString
summary: String literal contains format specifiers, but is not passed to a format method
layout: bugpattern
tags: ''
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Passing a string that contains format specifiers to a method that does not
perform string formatting is usually a mistake.

Do this:

```java {.good}
if (!isValid(arg)) {
  throw new IllegalArgumentException(String.format("invalid arg: %s", arg));
}
```

Not this:

```java {.bad}
if (!isValid(arg)) {
  throw new IllegalArgumentException("invalid arg: %s");
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("OrphanedFormatString")` to the enclosing element.
