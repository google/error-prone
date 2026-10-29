---
title: LoopToTestParameter
summary: Migrate loops in tests to use github.com/google/TestParameterInjector. Test
  parameterization executes each input case in strict isolation, ensuring that a single
  failure doesn't halt the rest of your test case while providing clear, per-case
  reporting without the need for manual loops.
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Iterating over test inputs inside a `@Test` method executes all cases in a
single test invocation: the first failure aborts the remaining cases, and test
reports do not identify which input failed.

Using `TestParameterInjector` with `@TestParameter` runs each input case in
strict isolation with per-case reporting:

```java
@Test
public void parseValidUnits() {
  for (TimeUnit unit : TimeUnit.values()) {
    assertThat(parseUnit(unit.name())).isEqualTo(unit);
  }
}
```

```java
@RunWith(TestParameterInjector.class)
public final class MyTest {
  @Test
  public void parseValidUnits(@TestParameter TimeUnit unit) {
    assertThat(parseUnit(unit.name())).isEqualTo(unit);
  }
}
```

For explicit subsets of values, pass the elements to `@TestParameter`:

```java
@Test
public void acceptsKnownSchemes() {
  for (String scheme : ImmutableList.of("http", "https")) {
    assertThat(isValidScheme(scheme)).isTrue();
  }
}
```

```java
@RunWith(TestParameterInjector.class)
public final class MyTest {
  @Test
  public void acceptsKnownSchemes(@TestParameter({"http", "https"}) String scheme) {
    assertThat(isValidScheme(scheme)).isTrue();
  }
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("LoopToTestParameter")` to the enclosing element.
