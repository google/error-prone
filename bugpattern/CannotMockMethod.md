---
title: CannotMockMethod
summary: Mockito cannot mock final or static methods, and can't detect this at runtime
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: MockitoBadFinalMethod, CannotMockFinalMethod_

## The problem
Mockito cannot mock `final` or `static` methods, and cannot tell at runtime that
this is attempted and fail with an error (as mocking `final` classes does).

`when(mock.finalMethod())` will invoke the real implementation of `finalMethod`.
In some cases, this may wind up accidentally doing what's intended:

```java
when(converter.convert(a)).thenReturn(b);
```

`convert` is final, but under the hood, calls `doForward`, so we wind up mocking
that method instead.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("CannotMockMethod")` to the enclosing element.
