---
title: UnnecessaryStaticImport
summary: "Using static imports for types is unnecessary"
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
Using static imports for types is unnecessary, since they can always be replaced by equivalent non-static imports.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("UnnecessaryStaticImport")` annotation to the enclosing element.
