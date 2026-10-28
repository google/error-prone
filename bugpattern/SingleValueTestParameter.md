---
title: SingleValueTestParameter
summary: A test parameter with only a single value should be a local variable in the
  test method
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
When using `TestParameterInjector`, parameterizing a test method parameter with
`@TestParameter` and only a single input value adds test runner and annotation
overhead without testing multiple cases. Instead, declare a local variable
initialized to that value inside the test method. This also avoids parsing
values from strings at runtime and, for enums, replaces string literals with
direct symbol references so cross-references and refactoring tools work
properly.

```java
@Test
public void someTest(
    @TestParameter("RED") Color color,
    @TestParameter boolean enabled) {
  ...
}
```

```java
@Test
public void someTest(@TestParameter boolean enabled) {
  Color color = Color.RED;
  ...
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SingleValueTestParameter")` to the enclosing element.
