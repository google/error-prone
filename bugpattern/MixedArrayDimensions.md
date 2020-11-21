---
title: MixedArrayDimensions
summary: C-style array declarations should not be used
layout: bugpattern
tags: Style
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The Java language allows the `[]` to be placed after the declaration of a
variable or method:

```java
int foos[];
int getFoos() [] {
  return xs;
}
```

The example above is equivalent to the following, strongly preferred, form:

```java
int[] foos;
int[] getFoos() {
  return xs;
}
```

The
[Java Language Specification §8.4](https://docs.oracle.com/javase/specs/jls/se11/html/jls-8.html#jls-8.4)
notes:

> The declaration of a method that returns an array is allowed to place some or
> all of the bracket pairs that denote the array type after the formal parameter
> list. This syntax is supported for compatibility with early versions of the
> Java programming language. It is very strongly recommended that this syntax is
> not used in new code.

Placing the square brackets immediately after the type is required by the
[Google Java Style Guide][style].

[style]: https://google.github.io/styleguide/javaguide.html#s4.8.3.2-array-declarations

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MixedArrayDimensions")` to the enclosing element.
