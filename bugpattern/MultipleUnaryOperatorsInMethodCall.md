---
title: MultipleUnaryOperatorsInMethodCall
summary: Avoid having multiple unary operators acting on the same variable in a method call
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Increment operators in method calls are dubious and while argument lists are
evaluated left-to-right, documentation suggests that code not rely on this
specification. In addition, code is clearer when each expression contains at
most one side effect.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MultipleUnaryOperatorsInMethodCall")` to the enclosing element.
