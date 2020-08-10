---
title: UnnecessaryBoxedVariable
summary: It is unnecessary for this variable to be boxed. Use the primitive instead.
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
This variable is of boxed type, but equivalent semantics can be achieved using the corresponding primitive type, which avoids the cost of constructing an unnecessary object.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessaryBoxedVariable")` to the enclosing element.
