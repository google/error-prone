---
title: JavaUtilDate
summary: Date has a bad API that leads to bugs; prefer java.time.Instant or LocalDate.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The `Date` API is full of
[major design flaws and pitfalls](https://codeblog.jonskeet.uk/2017/04/23/all-about-java-util-date/)
and should be avoided at all costs. Prefer the `java.time` APIs, specifically,
`java.time.Instant` (for physical time) and `java.time.LocalDate[Time]` (for
civil time).

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JavaUtilDate")` to the enclosing element.
