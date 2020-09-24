---
title: ObjectToString
summary: Calling toString on Objects that don't override toString() doesn't provide useful information
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Calling `toString` on objects that don't override `toString()` doesn't provide
useful information (just the class name and the `hashCode()`).

Consider overriding toString() function to return a meaningful String describing
the object.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ObjectToString")` to the enclosing element.
