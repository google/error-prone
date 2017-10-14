---
title: MockitoUsage
summary: Missing method call for verify(mock) here
layout: bugpattern
tags: ''
severity: ERROR
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Calls to `Mockito.when` should always be accompanied by a call to a method
like `thenReturn`.

```java
when(mock.get()).thenReturn(answer); // correct
when(mock.get())                     // oops!
```

Similarly, calls to `Mockito.verify` should call the verified method *outside*
the call to `verify`.

```java
verify(mock).execute(); // correct
verify(mock.execute()); // oops!
```

For more information, see the [Mockito documentation][docs].

[docs]: http://github.com/mockito/mockito/wiki/FAQ#what-are-unfinished-verificationstubbing-errors

## Suppression
Suppress false positives by adding an `@SuppressWarnings("MockitoUsage")` annotation to the enclosing element.
