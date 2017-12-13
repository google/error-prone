---
title: LogicalAssignment
summary: Assignment where a boolean expression was expected; use == if this assignment wasn't expected or add parentheses for clarity.
layout: bugpattern
tags: LikelyError
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
When an assignment expression is used as the condition of a loop, it isn't clear
to the reader whether the assignment was deliberate or it was intended to be an
equality test. Parenthesis should be used around assignments in loop conditions
to make it clear to the reader that the assignment is deliberate.

That is, instead of this:

```java
void f(boolean x) {
  while (x = checkSomething()) {
    // ...
  }
}
```

Prefer `while ((x = checkSomething())) {` or `while (x == checkSomething()) {`.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("LogicalAssignment")` to the enclosing element.
