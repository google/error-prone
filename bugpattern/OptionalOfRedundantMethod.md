---
title: OptionalOfRedundantMethod
summary: Optional.of() always returns a non-empty optional. Using ifPresent/isPresent/orElse/orElseGet/orElseThrow/isPresent/or/orNull
  method on it is unnecessary and most probably a bug.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("OptionalOfRedundantMethod")` to the enclosing element.
