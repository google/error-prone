---
title: GetClassOnClass
summary: Calling getClass() on an object of type Class returns the Class object for
  java.lang.Class; you probably meant to operate on the object directly
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Calling `getClass()` on an object of type Class returns the Class object for
java.lang.Class. Usually this is a mistake, and people intend to operate on the
object itself (for example, to print an error message). If you really did intend
to operate on the Class object for java.lang.Class, please use `Class.class`
instead for clarity.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("GetClassOnClass")` to the enclosing element.
