---
title: CatchingUnchecked
summary: This catch block catches `Exception`, but can only catch unchecked exceptions. Consider catching RuntimeException (or something more specific) instead so it is more apparent that no checked exceptions are being handled.
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
Suppress false positives by adding the suppression annotation `@SuppressWarnings("CatchingUnchecked")` to the enclosing element.
