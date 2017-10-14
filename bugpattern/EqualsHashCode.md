---
title: EqualsHashCode
summary: Classes that override equals should also override hashCode.
layout: bugpattern
tags: FragileCode
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The contract for `Object.hashCode` states that if two objects are equal, then
calling the `hashCode()` method on each of the two objects must produce the same
result. Implementing `equals()` but not `hashCode()` causes broken behaviour
when trying to store the object in a collection.

See Effective Java §3.9 for more information and a discussion of how to
correctly implement `hashCode()`.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("EqualsHashCode")` annotation to the enclosing element.
