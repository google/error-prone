---
title: OverrideThrowableToString
summary: To return a custom message with a Throwable class, one should override getMessage() instead of toString().
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Many logging tools build a string representation out of getMessage() and ignores
toString() completely.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("OverrideThrowableToString")` to the enclosing element.
