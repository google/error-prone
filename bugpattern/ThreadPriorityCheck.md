---
title: ThreadPriorityCheck
summary: Relying on the thread scheduler is discouraged; see Effective Java Item 72 (2nd edition) / 84 (3rd edition).
layout: bugpattern
tags: ''
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ThreadPriorityCheck")` to the enclosing element.
