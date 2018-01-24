---
title: StringSplitter
summary: Prefer Splitter to String.split
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
`String.split(String)` has surprising behaviour. For example, consider the
following puzzler from
http://konigsberg.blogspot.com/2009/11/final-thoughts-java-puzzler-splitting.html:

```java
String[] nothing = "".split(":");
String[] bunchOfNothing = ":".split(":");
```

The result is `[""]` and `[]`!

Prefer Guava's
[`Splitter`](http://google.github.io/guava/releases/23.0/api/docs/com/google/common/base/Splitter.html),
which has more predicitable behaviour and provides explicit control over the
handling of empty strings and the trimming of whitespace.

Alternately, consider using [`String.split(String,
int)`](https://docs.oracle.com/javase/9/docs/api/java/lang/String.html#split-java.lang.String-int-)
and setting an explicit 'limit'.

TIP: consider extracting the `Splitter` instance to a static final field.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("StringSplitter")` to the enclosing element.
