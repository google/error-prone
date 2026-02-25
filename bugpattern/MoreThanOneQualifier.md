---
title: MoreThanOneQualifier
summary: Using more than one qualifier annotation on the same element is not allowed.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: InjectMoreThanOneQualifier_

## The problem
An element can be qualified by at most one qualifier.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MoreThanOneQualifier")` to the enclosing element.
