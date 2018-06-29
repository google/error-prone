---
title: SystemExitOutsideMain
summary: Code that contains System.exit() is untestable.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Calling System.exit terminates the java process and returns a status code. Since
it is disruptive to shut down the process within library code, System.exit
should not be called outside of a main method.

Instead of calling System.exit consider throwing an unchecked exception to
signal failure.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SystemExitOutsideMain")` to the enclosing element.
