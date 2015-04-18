---
title: UnlockMethod
layout: bugpattern
category: JDK
severity: ERROR
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>JDK</td></tr>
<tr><td>Severity</td><td>ERROR</td></tr>
<tr><td>Maturity</td><td>EXPERIMENTAL</td></tr>
</table></div>

# Bug pattern: UnlockMethod
__This method does not acquire the locks specified by its @UnlockMethod annotation__

_Alternate names: GuardedBy_

## The problem
Methods with the @UnlockMethod annotation are expected to release one or more locks. The caller must hold the locks when the function is entered, and will not hold them when it completes.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("UnlockMethod")` annotation to the enclosing element.
