---
title: LongLiteralLowerCaseSuffix
summary: Prefer 'L' to 'l' for the suffix to long literals
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
A long literal can have a suffix of 'L' or 'l', but the former is less likely to
be confused with a '1' in most fonts.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("LongLiteralLowerCaseSuffix")` to the enclosing element.
