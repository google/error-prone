---
title: IterableAndIterator
summary: Class should not implement both `Iterable` and `Iterator`
layout: bugpattern
tags: FragileCode
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
An `Iterator` is a *state-ful* instance that enables you to check whether it has
more elements (via `hasNext()`) and moves to the next one if any (via `next()`),
while an `Iterable` is a representation of literally iterable elements. An
`Iterable` can generate multiple valid `Iterator`s, though.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("IterableAndIterator")` to the enclosing element.
