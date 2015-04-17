<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

---
title: ArrayToStringConcatenation
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>JDK</td></tr>
<tr><td>Severity</td><td>ERROR</td></tr>
<tr><td>Maturity</td><td>MATURE</td></tr>
</table></div>

# Bug pattern: ArrayToStringConcatenation
__Implicit toString used on an array (String + Array)__

## The problem
When concatenating an array to a string, the toString method on an array will yield its identity, such as [I@4488aabb. This is almost never needed. Use Arrays.toString to obtain a human-readable array summary.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ArrayToStringConcatenation")` annotation to the enclosing element.
