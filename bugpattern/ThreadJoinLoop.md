---
title: ThreadJoinLoop
summary: Thread.join needs to be surrounded by a loop until it succeeds, as in Uninterruptibles.joinUninterruptibly.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Thread.join() can be interrupted, and so requires users to catch
InterruptedException. Most users should be looping until the join() actually
succeeds.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ThreadJoinLoop")` to the enclosing element.
