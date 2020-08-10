---
title: PublicConstructorForAbstractClass
summary: Constructors of an abstract class can be declared protected as there is never a need for them to be public
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Abstract classes' constructors are only ever called by subclasses, never directly by another class. Therefore they never need public constructors: protected is accessible enough.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("PublicConstructorForAbstractClass")` to the enclosing element.
