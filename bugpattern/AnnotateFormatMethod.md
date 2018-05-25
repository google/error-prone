---
title: AnnotateFormatMethod
summary: This method passes a pair of parameters through to String.format, but the enclosing method wasn't annotated @FormatMethod. Doing so gives compile-time rather than run-time protection against malformed format strings.
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
