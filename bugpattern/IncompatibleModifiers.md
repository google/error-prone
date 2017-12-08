---
title: IncompatibleModifiers
summary: This annotation has incompatible modifiers as specified by its @IncompatibleModifiers annotation
layout: bugpattern
tags: LikelyError
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The @IncompatibleModifiers annotation declares that the target annotation is incompatible with a set of provided modifiers. This check ensures that all annotations respect their @IncompatibleModifiers specifications.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("IncompatibleModifiers")` to the enclosing element.
