---
title: TopLevel
summary: Source files should not contain multiple top-level class declarations
layout: bugpattern
category: JDK
severity: WARNING
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The Google Java Style Guide §3.4.1 requires each source file to contain "exactly one top-level class".

## Suppression
Suppress false positives by adding an `@SuppressWarnings("TopLevel")` annotation to the enclosing element.
