---
title: UnnecessaryBoxedAssignment
summary: This expression can be implicitly boxed.
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The Java language automatically converts primitive types to their boxed
representations in some contexts (see
[JLS 5.1.7](https://docs.oracle.com/javase/specs/jls/se11/html/jls-5.html#jls-5.1.7)).

That is, prefer this:

```java
int x;
Integer y = x;
```

to the equivalent but more verbose explicit conversion:

```java
int x;
Integer y = Integer.valueOf(x);
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessaryBoxedAssignment")` to the enclosing element.
