---
title: SelfComparison
summary: An object is compared to itself
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The arguments to compareTo method are the same object, so it always returns 0.
Either change the arguments to point to different objects or substitute 0.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SelfComparison")` to the enclosing element.
