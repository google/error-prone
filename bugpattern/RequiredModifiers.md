---
title: RequiredModifiers
layout: bugpattern
category: JDK
severity: WARNING
maturity: MATURE
---

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>JDK</td></tr>
<tr><td>Severity</td><td>WARNING</td></tr>
<tr><td>Maturity</td><td>MATURE</td></tr>
</table></div>

# Bug pattern: RequiredModifiers
__This annotation is missing required modifiers as specified by its @RequiredModifiers annotation__

## The problem
This annotation is itself annotated with @RequiredModifiers and can only be used when the specified modifiers are present. You are attempting touse it on an  element that is missing one or more required modifiers.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("RequiredModifiers")` annotation to the enclosing element.
