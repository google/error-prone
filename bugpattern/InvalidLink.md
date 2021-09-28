---
title: InvalidLink
summary: This @link tag looks wrong.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
This error is triggered by a Javadoc `@link` tag that either is syntactically
invalid or can't be resolved. See [javadoc documentation][javadoc] for an
explanation of how to correctly format the contents of this tag.

[javadoc]: https://docs.oracle.com/javase/8/docs/technotes/tools/unix/javadoc.html#JSSOR654

### Linking to generic types

Use the erased type of method parameters in `@link` tags. For example, write
`{@link #foo(List)}` instead of `{@link #foo(List<Bah>)}`. Javadoc does yet not
support generics in `@link` tags, due to a bug:
[JDK-5096551](https://bugs.openjdk.java.net/browse/JDK-5096551).

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("InvalidLink")` to the enclosing element.
