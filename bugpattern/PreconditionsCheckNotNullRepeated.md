---
title: PreconditionsCheckNotNullRepeated
summary: Including the first argument of checkNotNull in the failure message is not
  useful, as it will always be `null`.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("PreconditionsCheckNotNullRepeated")` to the enclosing element.
