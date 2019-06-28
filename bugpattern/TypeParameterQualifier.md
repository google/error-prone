---
title: TypeParameterQualifier
summary: Type parameter used as type qualifier
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
Using a type parameter as a qualifier in the name of a type or expression is
equivalent to referencing the type parameter's upper bound directly.

For example, this signature:

```java {.bad}
static <T extends Message> T populate(T.Builder builder) {}
```

Is identical to the following:

```java {.good}
static <T extends Message> T populate(Message.Builder builder) {}
```

The use of `T.Builder` is unnecessary and misleading. Always refer to the type
by its canonical name `Message.Builder` instead.

## Suppression
This check may not be suppressed.
