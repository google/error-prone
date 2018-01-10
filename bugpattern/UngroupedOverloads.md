---
title: UngroupedOverloads
summary: Constructors and methods with the same name should appear sequentially with no other code in between
layout: bugpattern
tags: ''
severity: SUGGESTION
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The [Google Java Style Guide §3.4.2.1][style] requires overloads to be grouped
together:

> When a class has multiple constructors, or multiple methods with the same
> name, these appear sequentially, with no other code in between (not even
> private members).

[style]: https://google.github.io/styleguide/javaguide.html#s3.4.2-ordering-class-contents

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UngroupedOverloads")` to the enclosing element.
