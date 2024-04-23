---
title: MockitoDoSetup
summary: Prefer using when/thenReturn over doReturn/when for additional type safety.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Prefer using the format

```java
  when(mock.mockedMethod(...)).thenReturn(returnValue);
```

to initialise mocks, rather than,

```java
  doReturn(returnValue).when(mock).mockedMethod(...);
```

Mockito recommends the `when`/`thenReturn` syntax as it is both more readable
and provides type-safety: the return type of the stubbed method is checked
against the stubbed value at compile time.

There are certain situations where `doReturn` is required:

*   Overriding previous stubbing where the method will *throw*, as `when` makes
    an actual method call.
*   Overriding a `spy` where the method call where calling the spied method
    brings undesired side-effects.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MockitoDoSetup")` to the enclosing element.
