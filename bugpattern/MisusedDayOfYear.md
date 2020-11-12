---
title: MisusedDayOfYear
summary: Use of 'DD' (day of year) in a date pattern with 'MM' (month of year) is
  not likely to be intentional, as it would lead to dates like 'March 73rd'.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MisusedDayOfYear")` to the enclosing element.
