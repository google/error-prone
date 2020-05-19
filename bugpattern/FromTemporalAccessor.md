---
title: FromTemporalAccessor
summary: Certain combinations of javaTimeType.from(TemporalAccessor) will always throw a DateTimeException or return the parameter directly.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Not all java.time types can be created via from(TemporalAccessor). For example, you can create a Month from a LocalDate (Month.from(localDate)) because a LocalDate consists of a year, month, and day. However, you cannot create a LocalDate from a Month (since it doesn't have the year or day information). Instead of throwing a DateTimeException at runtime, this checker validates the type transformations at compile time using static type information.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FromTemporalAccessor")` to the enclosing element.

