---
title: TestParametersNotInitialized
summary: This test has @TestParameter fields but is using the default JUnit4 runner.
  The parameters will not be initialised beyond their default value.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
`@TestParameter` fields are only injected when the test class is run with
`@RunWith(TestParameterInjector.class)`. If the test class uses another runner
such as `@RunWith(JUnit4.class)`, JUnit does not initialize `@TestParameter`
fields, leaving them at their default Java values (`null`, `0`, or `false`).

```java
@RunWith(JUnit4.class)
public final class MyTest {
  @TestParameter boolean enabled;

  @Test
  public void myTest() {
    // `enabled` is always `false`!
  }
}
```

```java
@RunWith(TestParameterInjector.class)
public final class MyTest {
  @TestParameter boolean enabled;

  @Test
  public void myTest() {
    // Runs with `enabled = true` and `enabled = false`.
  }
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("TestParametersNotInitialized")` to the enclosing element.
