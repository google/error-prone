---
title: FloggerRedundantIsEnabled
summary: Logger level check is already implied in the log() call. An explicit at[Level]().isEnabled()
  check is redundant.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FloggerRedundantIsEnabled")` to the enclosing element.
