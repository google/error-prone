---
title: DeprecatedVariable
summary: Applying the @Deprecated annotation to local variables or parameters has
  no effect
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
`@Deprecated` annotations should not be applied to local variables and
parameters, since they have no effect there.

The
[javadoc for `@Deprecated`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Deprecated.html)
says

> Use of the `@Deprecated` annotation on a local variable declaration or on a
> parameter declaration or a package declaration has no effect on the warnings
> issued by a compiler.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DeprecatedVariable")` to the enclosing element.
