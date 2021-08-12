---
title: ThrowsUncheckedException
summary: Unchecked exceptions do not need to be declared in the method signature.
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
[Effective Java 3rd Edition §74][ej3e-74] says:

> Use the Javadoc `@throws` tag to document each exception that a method can
> throw, but do *not* use the `throws` keyword on unchecked exceptions.

[ej3e-74]: https://books.google.com/books?id=BIpDDwAAQBAJ

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ThrowsUncheckedException")` to the enclosing element.
