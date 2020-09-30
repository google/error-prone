---
title: JodaTimeConverterManager
summary: Joda-Time's ConverterManager makes the semantics of DateTime/Instant/etc
  construction subject to global static state. If you need to define your own converters,
  use a helper.
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
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JodaTimeConverterManager")` to the enclosing element.
