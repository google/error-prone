---
title: AnnotateFormatMethod
summary: |-
  This method passes a pair of parameters through to String.format, but the enclosing method wasn't annotated @FormatMethod. Doing so gives compile-time rather than run-time protection against malformed format strings.

  WARNING: There's a very high chance that existing code will not be passing in well-formed format strings. Make sure you run tests including all users of this code before submitting.
layout: bugpattern
tags: FragileCode
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AnnotateFormatMethod")` to the enclosing element.
