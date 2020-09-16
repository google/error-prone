---
title: RestrictInjectVisibility
summary: Some methods (such as those annotated with @Inject or @Provides) are only intended to be called by a framework, and so should have default visibility
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("RestrictInjectVisibility")` to the enclosing element.
