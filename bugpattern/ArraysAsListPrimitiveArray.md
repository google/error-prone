---
title: ArraysAsListPrimitiveArray
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

# Bug pattern: ArraysAsListPrimitiveArray
__Arrays.asList does not autobox primitive arrays, as one might expect.__

## The problem
Arrays.asList does not autobox primitive arrays, as one might expect. If you intended to autobox the primitive array, use an asList method from Guava that does autobox.  If you intended to create a singleton list containing the primitive array, use Collections.singletonList to make your intent clearer.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ArraysAsListPrimitiveArray")` annotation to the enclosing element.
