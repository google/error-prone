---
title: InjectMoreThanOneQualifier
summary: Using more than one qualifier annotation on the same element is not allowed.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
An element can be qualified by at most one qualifier.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("InjectMoreThanOneQualifier")` to the enclosing element.
