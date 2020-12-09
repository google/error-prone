---
title: FloggerMessageFormat
summary: Invalid message format-style format specifier ({0}), expected printf-style
  (%s)
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Flogger uses printf-style format specifiers, such as %s and %d. Message format-style specifiers like {0} don't work.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FloggerMessageFormat")` to the enclosing element.
