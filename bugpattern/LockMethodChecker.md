---
title: LockMethodChecker
layout: bugpattern
category: JDK
severity: ERROR
maturity: EXPERIMENTAL
---

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>JDK</td></tr>
<tr><td>Severity</td><td>ERROR</td></tr>
<tr><td>Maturity</td><td>EXPERIMENTAL</td></tr>
</table></div>

# Bug pattern: LockMethodChecker
__This method does not acquire the locks specified by its @LockMethod annotation__

_Alternate names: GuardedBy_

## The problem
Methods with the @LockMethod annotation are expected to acquire one or more locks. The caller will hold the locks when the function finishes execution.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("LockMethodChecker")` annotation to the enclosing element.
