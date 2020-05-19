---
title: JodaNewPeriod
summary: This may have surprising semantics, e.g. new Period(LocalDate.parse("1970-01-01"), LocalDate.parse("1970-02-02")).getDays() == 1, not 32.
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
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JodaNewPeriod")` to the enclosing element.

