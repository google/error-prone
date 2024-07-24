---
title: UnnecessaryBreakInSwitch
summary: This break is unnecessary, fallthrough does not occur in -> switches
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The newer arrow (`->`) syntax for switches does not permit fallthrough between
cases. A `break` statement is allowed to break out of the switch, but including
a `break` as the last statement in a case body is unnecessary.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessaryBreakInSwitch")` to the enclosing element.
