---
title: DateChecker
summary: Warns against suspect looking calls to java.util.Date APIs
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
java.util.Date uses 1900-based years, 0-based months, 1-based days, and 0-based hours/minutes/seconds. Additionally, it allows for negative values or very large values (which rollover).

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DateChecker")` to the enclosing element.
