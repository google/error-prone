---
title: InterruptedExceptionSwallowed
summary: This catch block appears to be catching an explicitly declared InterruptedException as an Exception/Throwable and not handling the interruption separately.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
This check warns when an `InterruptedException` could be thrown but isn't being
individually handled.

It is important for correctness and performance that thread interruption is
handled properly, however `try` blocks that catch `Exception` or `Throwable` (or
methods that `throws` either type) make it difficult to recognize that
interruption may occur.

For advice on how to handle `InterruptedException`, see https://www.ibm.com/developerworks/library/j-jtp05236/index.html

## Suppression

Where possible, the best option is to enumerate the specific exceptions being
thrown/caught, so that `InterruptedException` is not possible. For example use
[multiple exception types](https://docs.oracle.com/javase/8/docs/technotes/guides/language/catch-multiple.html)
in a catch block instead of catching `Exception`.

Suppress false positives by adding an
`@SuppressWarnings("InterruptedExceptionSwallowed")` annotation to the enclosing
element, or the caught exception.

```java
try {
  future.get();
} catch (@SuppressWarnings("InterruptedExceptionSwallowed") Exception e) {
  throw new IllegalStateException(e);
}
```

