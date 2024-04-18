---
title: UseCorrectAssertInTests
summary: Java assert is used in testing code. For testing purposes, prefer using Truth-based
  assertions.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Java assert statements are not run unless explicitly enabled via runtime flags
to the JVM invocation.

If asserts are not enabled, then a test using assert would continue to pass even
if a bug is introduced since these statements will not be executed. To avoid
this, use one of the assertion libraries that are always enabled, such as
JUnit's `org.junit.Assert` or Google's Truth library. These will also produce
richer contextual failure diagnostics to aid and accelerate debugging.

Don't do this:

```java
@Test
public void testArray() {
  String[] arr = getArray();

  assert arr != null;
  assert arr.length == 1;
  assert arr[0].equals("hello");
}
```

Do this instead:

```java
import static com.google.common.truth.Truth.assertThat;

@Test
public void testArray() {
  String[] arr = getArray();

  assertThat(arr).isNotNull();
  assertThat(arr).hasLength(1);
  assertThat(arr[0]).isEqualTo("hello");
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UseCorrectAssertInTests")` to the enclosing element.
