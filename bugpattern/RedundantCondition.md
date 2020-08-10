---
title: RedundantCondition
summary: Redundant usage of a boolean variable with known value
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
If a boolean is enforced to have a certain value in an if statement and then
used as part of a condition within the block, it is redundant.

To fix the problem, replace the variable with `true` in inner statement.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("RedundantCondition")` to the enclosing element.
