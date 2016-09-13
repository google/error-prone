---
title: RemoveUnusedImports
summary: Unused imports
layout: bugpattern
category: JDK
severity: SUGGESTION
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
This import is unused.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("RemoveUnusedImports")` annotation to the enclosing element.
