---
title: JodaPlusMinusLong
summary: Use of JodaTime's type.plus(long) or type.minus(long) is not allowed (where <type> = {Duration,Instant,DateTime,DateMidnight}). Please use  type.plus(Duration.millis(long)) or type.minus(Duration.millis(long)) instead.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
JodaTime's type.plus(long) and type.minus(long) methods are often a source of bugs because the units of the parameters are ambiguous. Please use type.plus(Duration.millis(long)) or type.minus(Duration.millis(long)) instead.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JodaPlusMinusLong")` to the enclosing element.
