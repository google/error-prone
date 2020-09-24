---
title: PreconditionsInvalidPlaceholder
summary: Preconditions only accepts the %s placeholder in error message strings
layout: bugpattern
tags: LikelyError
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The Guava Preconditions checks take error message template strings that look
similar to format strings, but only accept the %s format (not %d, %f, etc.).
This check points out places where a Preconditions error message template string
has a non-%s format, or where the number of arguments does not match the number
of %s formats in the string.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("PreconditionsInvalidPlaceholder")` to the enclosing element.
