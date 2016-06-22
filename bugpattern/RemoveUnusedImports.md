---
title: RemoveUnusedImports
summary: Unused imports
layout: bugpattern
category: JDK
severity: SUGGESTION
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Unused imports

## Suppression
Suppress false positives by adding an `@SuppressWarnings("RemoveUnusedImports")` annotation to the enclosing element.
