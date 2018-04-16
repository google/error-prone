---
title: DeadThread
summary: Thread created but not started
layout: bugpattern
tags: ''
severity: ERROR
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The Thread is created with `new`, but is never started and is not otherwise
captured.

Threads must be started with `start()` to actually execute.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DeadThread")` to the enclosing element.
