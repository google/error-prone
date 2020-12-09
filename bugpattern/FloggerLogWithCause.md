---
title: FloggerLogWithCause
summary: Setting the caught exception as the cause of the log message may provide
  more context for anyone debugging errors.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FloggerLogWithCause")` to the enclosing element.
