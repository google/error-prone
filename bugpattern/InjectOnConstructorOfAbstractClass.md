---
title: InjectOnConstructorOfAbstractClass
summary: Constructors on abstract classes are never directly @Injected, only the constructors of their subclasses can be @Inject'ed.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
When dependency injection frameworks call constructors, they can only do so on constructors of concrete classes, which can delegate to superclass constructors. In the case of abstract classes, their constructors are only called by their concrete subclasses, not directly by injection frameworks, so the `@Inject` annotation has no effect.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("InjectOnConstructorOfAbstractClass")` annotation to the enclosing element.
