---
title: UnusedException
summary: This catch block catches an exception and re-throws another, but swallows the caught exception rather than setting it as a cause. This can make debugging harder.
layout: bugpattern
tags: Style
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Catching an exception and throwing another is a common pattern. This is often
used to supply additional information, or to turn a checked exception into an
unchecked one.

```java
  try {
    ioLogic();
  } catch (IOException e) {
    throw new IllegalStateException(); // BAD
  }
```

Throwing a new exception without supplying the caught one as a cause means the
stack trace will terminate at the `catch` block, which will make debugging a
possible fault in `ioLogic()` far harder than is necessary.

Prefer wrapping the original exception instead,

```java
  try {
    ioLogic();
  } catch (IOException e) {
    throw new IllegalStateException(e); // GOOD
  }
```

Suppress false positives with `@SuppressWarnings("UnusedException")` on the
ignored exception. Consider also adding a comment to explain why the exception
should not be propagated.

```java
  try {
    ...
  } catch (@SuppressWarnings("UnusedException") IOException e) {
    throw new IllegalStateException();
  }
```

