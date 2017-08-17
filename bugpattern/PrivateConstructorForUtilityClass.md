---
title: PrivateConstructorForUtilityClass
summary: Utility classes (only static members) are not designed to be instantiated and should be made noninstantiable with a default constructor.
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Classes that only include static members have no behavior particular to any given instance, so instantiating them is nonsense. To prevent users from mistakenly creating instances, the class should include a private constructor.  See Effective Java, Second Edition - Item 4.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("PrivateConstructorForUtilityClass")` annotation to the enclosing element.
