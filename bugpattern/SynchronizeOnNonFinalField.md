<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

---
title: SynchronizeOnNonFinalField
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

# Bug pattern: SynchronizeOnNonFinalField
__Synchronizing on non-final fields is not safe: if the field is ever updated, different threads may end up locking on different objects.__

## The problem
Possible fixes:
* If the field is already effectively final, add the missing 'final' modifier.
* If the field needs to be mutable, create a separate lock by adding a private  final field and synchronizing on it to guard all accesses.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("SynchronizeOnNonFinalField")` annotation to the enclosing element.
