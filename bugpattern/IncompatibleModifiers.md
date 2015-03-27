---
title: IncompatibleModifiers
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

# Bug pattern: IncompatibleModifiers
__This annotation has incompatible modifiers as specified by its @IncompatibleModifiers annotation__

## The problem
The @IncompatibleModifiers annotation declares that the target annotation is incompatible with a set of provided modifiers. This check ensures that all annotations respect their @IncompatibleModifiers specifications.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("IncompatibleModifiers")` annotation to the enclosing element.
