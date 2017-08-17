---
title: UseBinds
summary: '@Binds is a more efficient and declarative mechanism for delegating a binding.'
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
A @Provides or @Produces method that returns its single parameter has long been Dagger's only mechanism for delegating a binding. Since the delegation is implemented via a user-defined method there is a disproportionate amount of overhead for such a conceptually simple operation. @Binds was introduced to provide a declarative way of delegating from one binding to another in a way that allows for minimal overhead in the implementation. @Binds should always be preferred over @Provides or @Produces for delegation.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("UseBinds")` annotation to the enclosing element.
