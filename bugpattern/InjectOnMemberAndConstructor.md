---
title: InjectOnMemberAndConstructor
summary: Members shouldn't be annotated with @Inject if constructor is already annotated @Inject
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("InjectOnMemberAndConstructor")` to the enclosing element.
