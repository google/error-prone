---
title: MissingCasesInEnumSwitch
layout: bugpattern
category: JDK
severity: ERROR
maturity: EXPERIMENTAL
---

# Bug pattern: MissingCasesInEnumSwitch
__Enum switch statement is missing cases__

## The problem
Enums on switches should either handle all possible values of the enum, or have an explicit 'default' case.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("MissingCasesInEnumSwitch")` annotation to the enclosing element.
