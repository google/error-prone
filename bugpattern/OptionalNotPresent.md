---
title: OptionalNotPresent
summary: One should not call optional.get() inside an if statement that checks !optional.isPresent
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Calling `get()` on an `Optional` that is not present will result in a
`NoSuchElementException`.

This check detects cases where `get()` is called whent the optional is
definitely not present, e.g.:

```java
if (!o.isPresent()) {
  return o.get(); // this will throw a NoSuchElementException
}
```

```java
if (o.isEmpty()) {
  return o.get(); // this will throw a NoSuchElementException
}
```

These cases are almost definitely bugs; the intent may have been to invert the
test:

```java
if (o.isPresent()) {
  return o.get();
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("OptionalNotPresent")` to the enclosing element.
