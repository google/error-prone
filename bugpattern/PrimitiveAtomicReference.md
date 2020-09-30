---
title: PrimitiveAtomicReference
summary: Using compareAndSet with boxed primitives is dangerous, as reference rather
  than value equality is used. Consider using AtomicInteger, AtomicLong, or AtomicBoolean
  instead.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("PrimitiveAtomicReference")` to the enclosing element.
