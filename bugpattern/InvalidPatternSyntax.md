---
title: InvalidPatternSyntax
summary: Invalid syntax used for a regular expression
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
This error is triggered by calls to regex-accepting methods with invalid string
literals. These calls would cause a PatternSyntaxException at runtime.

We deliberately do not check java.util.regex.Pattern#compile as many of its
users are deliberately testing the regex compiler or using a vacuously true
regex.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("InvalidPatternSyntax")` to the enclosing element.
