---
title: StringCharset
summary: StringCharset
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Prefer StandardCharsets over using string names for charsets

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("StringCharset")` to the enclosing element.
