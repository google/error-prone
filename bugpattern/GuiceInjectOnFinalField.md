---
title: GuiceInjectOnFinalField
summary: Although Guice allows injecting final fields, doing so is disallowed because
  the injected value may not be visible to other threads.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
From the [Guice wiki][wiki]:

> Injecting `final` fields is not recommended because the injected value may not
> be visible to other threads.

[wiki]: https://github.com/google/guice/wiki/InjectionPoints#how-guice-injects

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("GuiceInjectOnFinalField")` to the enclosing element.
