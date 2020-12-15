---
title: ZoneIdOfZ
summary: Use ZoneOffset.UTC instead of ZoneId.of("Z").
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Avoid the magic constant (ZoneId.of("Z")) in favor of a more descriptive API:  ZoneOffset.UTC

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ZoneIdOfZ")` to the enclosing element.
