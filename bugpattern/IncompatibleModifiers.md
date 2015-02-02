---
title: IncompatibleModifiers
layout: bugpattern
category: JDK
severity: WARNING
maturity: MATURE
---

# Bug pattern: IncompatibleModifiers
__This annotation has incompatible modifiers as specified by its @IncompatibleModifiers annotation__

## The problem
The @IncompatibleModifiers annotation declares that the target annotation is incompatible with a set of provided modifiers. This check ensures that all annotations respect their @IncompatibleModifiers specifications.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("IncompatibleModifiers")` annotation to the enclosing element.
