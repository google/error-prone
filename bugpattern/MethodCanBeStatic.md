---
title: MethodCanBeStatic
summary: Method is non-static but does not reference enclosing class
layout: bugpattern
category: JDK
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Methods should be static unless they reference members of their enclosing class.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("MethodCanBeStatic")` annotation to the enclosing element.
