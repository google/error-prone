---
title: DeadThread
summary: Thread created but not started
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The Thread is created with new, but is never started, and the reference is lost.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("DeadThread")` annotation to the enclosing element.
