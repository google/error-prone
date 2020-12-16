---
title: JodaDateTimeConstants
summary: Usage of the `_PER_` constants in `DateTimeConstants` are problematic because
  they encourage manual date/time math.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Manual date/time math leads to overflows, unit mismatches, and weak typing. Prefer to use strong types (e.g., `java.time.Duration` or `java.time.Instant`) and their APIs to perform date/time math.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JodaDateTimeConstants")` to the enclosing element.
