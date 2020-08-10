---
title: LossyPrimitiveCompare
summary: Using an unnecessarily-wide comparison method can lead to lossy comparison
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Implicit widening conversions when comparing two primitives with methods like Float.compare can lead to lossy comparison. For example, `Float.compare(Integer.MAX_VALUE, Integer.MAX_VALUE - 1) == 0`. Use a compare method with non-lossy conversion, or ideally no conversion if possible.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("LossyPrimitiveCompare")` to the enclosing element.
