---
title: PrimitiveArrayPassedToVarargsMethod
summary: Passing a primitive array to a varargs method is usually wrong
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
When you pass a primitive array as the only argument to a varargs method, the
primitive array is autoboxed into a single-element Object array. This is usually
not what was intended.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("PrimitiveArrayPassedToVarargsMethod")` to the enclosing element.
