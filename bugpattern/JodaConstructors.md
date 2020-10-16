---
title: JodaConstructors
summary: Use of certain JodaTime constructors are not allowed.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Use JodaTime's static factories instead of the ambiguous constructors.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JodaConstructors")` to the enclosing element.
