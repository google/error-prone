---
title: ScopeOnModule
summary: Scopes on modules have no function and will soon be an error.
layout: bugpattern
tags: ''
severity: SUGGESTION
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Scopes on modules have no function and are often incorrectly assumed to apply the scope to every binding method in the module. This check removes all scope annotations from any class annotated with `@Module` or `@ProducerModule`.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ScopeOnModule")` to the enclosing element.
