---
title: EmptyCatch
summary: Caught exceptions should not be ignored
layout: bugpattern
tags: Style
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The [Google Java Style Guide §6.2][style] states:

> It is very rarely correct to do nothing in response to a caught exception.
> (Typical responses are to log it, or if it is considered "impossible", rethrow
> it as an AssertionError.)
>
> When it truly is appropriate to take no action whatsoever in a catch block,
> the reason this is justified is explained in a comment.

When writing tests that expect an exception to be thrown, prefer using
[`Assert.assertThrows`][assertthrows] instead of writing a try-catch. That is,
prefer this:

```java
assertThrows(NoSuchElementException.class, () -> emptyStack.pop());
```

instead of this:

```java
try {
  emptyStack.pop();
  fail();
} catch (NoSuchElementException expected) {
}
```

[style]: https://google.github.io/styleguide/javaguide.html#s6.2-caught-exceptions

[assertthrows]: https://junit.org/junit4/javadoc/latest/org/junit/Assert.html#assertThrows(java.lang.Class,%20org.junit.function.ThrowingRunnable)

