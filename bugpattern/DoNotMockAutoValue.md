---
title: DoNotMockAutoValue
summary: AutoValue classes represent pure data classes, so mocking them should not
  be necessary. Construct a real instance of the class instead.
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
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DoNotMockAutoValue")` to the enclosing element.
