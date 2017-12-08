---
title: ThrowsUncheckedException
summary: Unchecked exceptions do not need to be declared in the method signature.
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
Effective Java Item 62 says:

> Use the Javadoc `@throws` tag to document each unchecked exception that a
> method can throw, but do not use the throws keyword to include unchecked
> exceptions in the method declaration.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ThrowsUncheckedException")` to the enclosing element.
