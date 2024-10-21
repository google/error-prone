---
title: JavaDurationGetSecondsToToSeconds
summary: Prefer duration.toSeconds() over duration.getSeconds()
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
duration.getSeconds() is a decomposition API which should always be used alongside duration.getNano(). duration.toSeconds() is a conversion API, and the preferred way to convert to seconds.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JavaDurationGetSecondsToToSeconds")` to the enclosing element.
