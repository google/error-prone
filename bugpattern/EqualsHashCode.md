---
title: EqualsHashCode
layout: bugpattern
category: JDK
severity: WARNING
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>JDK</td></tr>
<tr><td>Severity</td><td>WARNING</td></tr>
<tr><td>Maturity</td><td>MATURE</td></tr>
</table></div>

# Bug pattern: EqualsHashCode
__Classes that override equals should also override hashCode.__

## The problem
The contact for Object.hashCode states that if two objects are equal, then calling the hashCode() method on each of the two objects must produce the same result. Implementing equals() but not hashCode() causes broken behaviour when trying to store the object in a collection. See Effective Java 3.9 for more information and a discussion of how to correctly implement hashCode().

## Suppression
Suppress false positives by adding an `@SuppressWarnings("EqualsHashCode")` annotation to the enclosing element.
