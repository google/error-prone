---
title: JavaPeriodGetDays
summary: period.getDays() only accesses the "days" portion of the Period, and doesn't represent the total span of time of the period. Consider using org.threeten.extra.Days to extract the difference between two civil dates if you want the whole time.
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
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JavaPeriodGetDays")` to the enclosing element.
