---
title: DefaultLocale
summary: Implicit use of the JVM default locale, which can result in differing behaviour
  between JVM executions.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DefaultLocale")` to the enclosing element.
