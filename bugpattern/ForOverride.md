---
title: ForOverride
summary: Method annotated @ForOverride must be protected or package-private and only invoked from declaring class, or from an override of the method
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
A method that overrides a @ForOverride method should not be invoked directly. Instead, it should be invoked only from the class in which it was declared. For example, if overriding Converter.doForward, you should invoke it through Converter.convert. For testing, factor out the code you want to run to a separate method.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ForOverride")` annotation to the enclosing element.
