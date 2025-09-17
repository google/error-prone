---
title: SystemOut
summary: Production code should not print to standard out or standard error. Standard
  out and standard error should only be used for debugging.
layout: bugpattern
tags: LikelyError
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SystemOut")` to the enclosing element.
