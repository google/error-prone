---
title: MissingSuperCall
summary: Overriding method is missing a call to overridden super method
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
API providers may annotate a method with an annotation like
`android.support.annotation.CallSuper` or
`javax.annotation.OverridingMethodsMustInvokeSuper` to require that overriding
methods invoke the super method. This check enforces those annotations.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MissingSuperCall")` to the enclosing element.
