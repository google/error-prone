---
title: ReturnValueIgnored
summary: Return value of this method must be used
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: ResultOfMethodCallIgnored, CheckReturnValue_

## The problem
Certain library methods do nothing useful if their return value is ignored. For
example, String.trim() has no side effects, and you must store the return value
of String.intern() to access the interned string. This check encodes a list of
methods in the JDK whose return value must be used and issues an error if they
are not.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ReturnValueIgnored")` to the enclosing element.
