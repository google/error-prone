---
title: TruthConstantAsserts
summary: Truth Library assert is called on a constant.
layout: bugpattern
tags: FragileCode
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The arguments to assertThat method is a constant. It should be a variable or a
method invocation. For eg. switch assertThat(1).isEqualTo(methodCall()) to
assertThat(methodCall()).isEqualTo(1).

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("TruthConstantAsserts")` to the enclosing element.
