---
title: EmptyIf
summary: Empty statement after if
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: empty_

## The problem
An if statement contains an empty statement as the then clause. A semicolon may
have been inserted by accident.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("EmptyIf")` to the enclosing element.
