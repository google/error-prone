---
title: AndroidInjectionBeforeSuper
summary: AndroidInjection.inject() should always be invoked before calling super.lifecycleMethod()
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Members injection should always be called as early as possible to avoid
uninitialized @Inject members. This is also crucial to protect against bugs
during configuration changes and reattached Fragments to make sure that each
framework type is injected in the appropriate order.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AndroidInjectionBeforeSuper")` to the enclosing element.
