---
title: ImmutableEnumChecker
summary: Enums should always be immutable
layout: bugpattern
category: JDK
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
All fields in your enum class should be final and either be primitive or refer
to deeply immutable objects.

Static state is dangerous to begin with, but much worse for enums. We all think
of enum values as constants -- and even refer to them as "enum constants" --
and would be very surprised if any of their state ever changed, or was not
thread-safe.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ImmutableEnumChecker")` annotation to the enclosing element.
