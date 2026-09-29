---
title: SimplifyTestParameter
summary: Explicitly specifying all values on a boolean or enum @TestParameter is unnecessary
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
When using `TestParameterInjector`, annotating a `boolean`, `Boolean`, or `Enum`
parameter or field with `@TestParameter` automatically injects all possible
values (note that it does not inject `null`). Explicitly specifying all values
is redundant, and for enums, omitting the explicit list ensures the test will
automatically run for any new values added to the enum in the future.

```java
@Test
public void someTest(
    @TestParameter({"true", "false"}) boolean foo,
    @TestParameter({"HEARTS", "DIAMONDS", "CLUBS", "SPADES"}) Suit suit) {
  ...
}
```

```java
@Test
public void someTest(@TestParameter boolean foo, @TestParameter Suit suit) {
  ...
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SimplifyTestParameter")` to the enclosing element.
