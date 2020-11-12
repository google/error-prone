---
title: TimeUnitMismatch
summary: An value that appears to be represented in one unit is used where another
  appears to be required (e.g., seconds where nanos are needed)
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("TimeUnitMismatch")` to the enclosing element.
