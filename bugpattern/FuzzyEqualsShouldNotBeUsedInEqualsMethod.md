---
title: FuzzyEqualsShouldNotBeUsedInEqualsMethod
summary: DoubleMath.fuzzyEquals should never be used in an Object.equals() method
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
From documentation: DoubleMath.fuzzyEquals is not transitive, so it is not
suitable for use in Object#equals implementations.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FuzzyEqualsShouldNotBeUsedInEqualsMethod")` to the enclosing element.
