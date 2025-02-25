---
title: TimeInStaticInitializer
summary: Accessing the current time in a static initialiser captures the time at class
  loading, which is rarely desirable.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("TimeInStaticInitializer")` to the enclosing element.
