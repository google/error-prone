---
title: AssertThrowsMultipleStatements
summary: The lambda passed to assertThrows should contain exactly one statement
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
If the body of the lambda passed to `assertThrows` contains multiple statements,
executation of the lambda will stop at the first statement that throws an
exception and all subsequent statements will be ignored.

This means that:

*   Any set-up logic in the lambda will cause the test to incorrectly pass if it
    throws the expected exception.
*   Any assertions that run after the statement that throws will never be 
    executed.

Don't do this:

```java {.bad}
ImmutableList<Integer> xs;
assertThrows(
    UnsupportedOperationException.class,
    () -> {
        xs = ImmutableList.of(); // the test passes if this throws
        xs.add(0);
        assertThat(xs).isEmpty(); // this is never executed
    });
```

Do this instead:

```java {.good}
ImmutableList<Integer> xs = ImmutableList.of();
assertThrows(
    UnsupportedOperationException.class,
    () -> xs.add(0));
assertThat(xs).isEmpty();
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AssertThrowsMultipleStatements")` to the enclosing element.
