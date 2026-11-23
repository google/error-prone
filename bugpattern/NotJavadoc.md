---
title: NotJavadoc
summary: Avoid using `/**` for comments which aren't actually Javadoc.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
This comment starts with `/**`, but isn't actually Javadoc.

Javadoc comments
[must precede a class, field, constructor, or method declaration](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html#format).
Javadoc cannot be applied to classes or methods nested within a method, as those
elements never form part of an API.

Using `/**` comments in locations where Javadoc is not supported is confusing
and unnecessary.

Suggested solutions:

*   If the comment is intended to be part of the API documentation, move it to
    the appropriate class, field, constructor, or method declaration.

*   If the comment is intended to be an implementation comment, use a
    single-line `//` or a multi-line `/*` comment instead.

> NOTE: Javadoc must appear before any annotations, or the compiler will fail to
> recognise it as Javadoc. That is, prefer this:
>
> ```java
> /** Might return a frobnicator. */
> @Nullable
> Frobnicator getFrobnicator();
> ```
>
> instead of this:
>
> ```java
> @Nullable
> /** Might return a frobnicator. */
> Frobnicator getFrobnicator();
> ```

## Suppression

Suppress by applying `@SuppressWarnings("NotJavadoc")` to the enclosing element.

