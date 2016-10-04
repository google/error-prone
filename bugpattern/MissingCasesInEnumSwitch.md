---
title: MissingCasesInEnumSwitch
summary: Enum switch statement is missing cases
layout: bugpattern
category: JDK
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Switches on enums should either handle all possible values of the enum, or have an explicit default case

## Suppression
Suppress false positives by adding an `@SuppressWarnings("MissingCasesInEnumSwitch")` annotation to the enclosing element.
