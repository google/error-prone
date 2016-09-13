---
title: ThrowsUncheckedException
summary: Unchecked exceptions do not need to be declared in the method signature.
layout: bugpattern
category: JDK
severity: SUGGESTION
maturity: MATURE
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
Suppress false positives by adding an `@SuppressWarnings("ThrowsUncheckedException")` annotation to the enclosing element.
